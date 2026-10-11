// Chat Live — خادم التوكن (Cloudflare Worker)
// 1) يتحقق أن المستخدم مسجّل دخول عبر توكن Firebase (Authorization: Bearer).
// 2) يُصدر توكن Agora RTC صالحاً لساعة للغرفة المطلوبة.
// 3) /notify: يرسل إشعارات Push (FCM) لقائمة مستخدمين — يحتاج السر NOTIFICATION_SERVICE_KEY.
// الشهادة AGORA_APP_CERTIFICATE تُقرأ من بيئة Cloudflare فقط، ولا تُعاد ولا تُسجَّل أبداً.
// الخوارزمية مطابقة لـAccessToken2 الرسمية من AgoraIO/Tools.

const enc = new TextEncoder();
const dec = new TextDecoder();

const JWKS_URL =
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';
const TOKEN_TTL_SECONDS = 3600;
const RTC_SERVICE_TYPE = 1;
const PRIV_JOIN_CHANNEL = 1;
const PRIV_PUBLISH_AUDIO = 2;
const PRIV_PUBLISH_VIDEO = 3;
const PRIV_PUBLISH_DATA = 4;
const CLOCK_SKEW_SECONDS = 60;

// ---------- بايتات بترتيب little-endian (كما في Agora) ----------
const u16 = (v) => {
  const b = new Uint8Array(2);
  new DataView(b.buffer).setUint16(0, v, true);
  return b;
};
const u32 = (v) => {
  const b = new Uint8Array(4);
  new DataView(b.buffer).setUint32(0, v >>> 0, true);
  return b;
};
const concat = (parts) => {
  const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0));
  let off = 0;
  for (const p of parts) {
    out.set(p, off);
    off += p.length;
  }
  return out;
};
const lenPrefixed = (bytes) => concat([u16(bytes.length), bytes]);
const isHex32 = (s) => typeof s === 'string' && /^[0-9a-fA-F]{32}$/.test(s);
const base64 = (bytes) => {
  let s = '';
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s);
};
const b64urlToBytes = (s) => {
  const std = s.replace(/-/g, '+').replace(/_/g, '/');
  const padded = std + '='.repeat((4 - (std.length % 4)) % 4);
  return Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
};

async function hmacSha256(keyBytes, msgBytes) {
  const key = await crypto.subtle.importKey(
    'raw',
    keyBytes,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  return new Uint8Array(await crypto.subtle.sign('HMAC', key, msgBytes));
}

// صيغة zlib (deflate) — نفس التي يفكّها Agora بـ zlib.inflateSync
async function deflateZlib(bytes) {
  const stream = new Blob([bytes]).stream().pipeThrough(new CompressionStream('deflate'));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

// ---------- توكن Agora RTC (AccessToken2 / Token007) ----------
function privilegeMap(map) {
  const keys = Object.keys(map)
    .map(Number)
    .sort((a, b) => a - b);
  return concat([u16(keys.length), ...keys.map((k) => concat([u16(k), u32(map[k])]))]);
}

function packRtcService(channelName, uid, publisher, seconds) {
  const privileges = { [PRIV_JOIN_CHANNEL]: seconds };
  if (publisher) {
    privileges[PRIV_PUBLISH_AUDIO] = seconds;
    privileges[PRIV_PUBLISH_VIDEO] = seconds;
    privileges[PRIV_PUBLISH_DATA] = seconds;
  }
  return concat([
    u16(RTC_SERVICE_TYPE),
    privilegeMap(privileges),
    lenPrefixed(enc.encode(channelName)),
    lenPrefixed(enc.encode(String(uid))),
  ]);
}

// مفتاح التوقيع المشتق من الشهادة:
// k1 = HMAC(key = LE32(issueTs), msg = cert)، ثم k2 = HMAC(key = LE32(salt), msg = k1)
async function deriveSigningKey(appCertificate, issueTs, salt) {
  const k1 = await hmacSha256(u32(issueTs), enc.encode(appCertificate));
  return hmacSha256(u32(salt), k1);
}

export async function buildRtcToken({
  appId,
  appCertificate,
  channelName,
  uid,
  publisher,
  expireSeconds = TOKEN_TTL_SECONDS,
  issueTs,
  salt,
}) {
  const ts = issueTs ?? Math.floor(Date.now() / 1000);
  const s = salt ?? 1 + (crypto.getRandomValues(new Uint32Array(1))[0] % 99999999);
  const signingKey = await deriveSigningKey(appCertificate, ts, s);
  const info = concat([
    lenPrefixed(enc.encode(appId)),
    u32(ts),
    u32(expireSeconds),
    u32(s),
    u16(1), // عدد الخدمات: خدمة RTC واحدة
    packRtcService(channelName, uid, publisher, expireSeconds),
  ]);
  const signature = await hmacSha256(signingKey, info);
  const content = concat([lenPrefixed(signature), info]);
  return '007' + base64(await deflateZlib(content));
}

// uid ثابت لكل مستخدم مشتق من معرّف Firebase، والعميل لا يختاره (فلا ينتحل أحد هوية غيره)
export async function agoraUidFor(firebaseUid) {
  const d = new Uint8Array(await crypto.subtle.digest('SHA-256', enc.encode(firebaseUid)));
  const v = ((d[0] << 24) | (d[1] << 16) | (d[2] << 8) | d[3]) & 0x7fffffff;
  return v || 1;
}

// ---------- التحقق من توكن Firebase (وفق وثائق Firebase الرسمية) ----------
let jwksCache = { keys: null, expiresAt: 0 };

async function firebaseKeys() {
  if (jwksCache.keys && Date.now() < jwksCache.expiresAt) return jwksCache.keys;
  const res = await fetch(JWKS_URL);
  if (!res.ok) throw new Error('jwks_unavailable');
  const maxAge = Number(/max-age=(\d+)/.exec(res.headers.get('Cache-Control') || '')?.[1] || 3600);
  const keys = (await res.json()).keys;
  jwksCache = { keys, expiresAt: Date.now() + maxAge * 1000 };
  return keys;
}

export async function verifyFirebaseIdToken(idToken, projectId, { getKeys = firebaseKeys, nowSeconds } = {}) {
  const parts = String(idToken).split('.');
  if (parts.length !== 3) throw new Error('malformed');
  const header = JSON.parse(dec.decode(b64urlToBytes(parts[0])));
  const payload = JSON.parse(dec.decode(b64urlToBytes(parts[1])));
  if (header.alg !== 'RS256' || typeof header.kid !== 'string') throw new Error('bad_header');

  const jwk = (await getKeys()).find((k) => k.kid === header.kid);
  if (!jwk) throw new Error('unknown_kid');
  const key = await crypto.subtle.importKey(
    'jwk',
    { kty: 'RSA', n: jwk.n, e: jwk.e, alg: 'RS256', ext: true },
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['verify'],
  );
  const valid = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5',
    key,
    b64urlToBytes(parts[2]),
    enc.encode(`${parts[0]}.${parts[1]}`),
  );
  if (!valid) throw new Error('bad_signature');

  const now = nowSeconds ?? Math.floor(Date.now() / 1000);
  if (!(payload.exp > now)) throw new Error('expired');
  if (!(payload.iat <= now + CLOCK_SKEW_SECONDS)) throw new Error('iat');
  if (!(payload.auth_time <= now + CLOCK_SKEW_SECONDS)) throw new Error('auth_time');
  if (payload.aud !== projectId) throw new Error('aud');
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) throw new Error('iss');
  if (typeof payload.sub !== 'string' || payload.sub.length === 0) throw new Error('sub');
  return payload.sub;
}

// ---------- إشعارات Push (FCM — Firebase Cloud Messaging API v1) ----------
// يستخدم حساب الخدمة (Service Account) من السر NOTIFICATION_SERVICE_KEY (base64 لملف JSON).
const FCM_SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';
const OAUTH_TOKEN_URL = 'https://oauth2.googleapis.com/token';
const FIRESTORE_DOC_URL = (projectId, userId) =>
  `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/users/${encodeURIComponent(userId)}`;
const FCM_SEND_URL = (projectId) =>
  `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

const b64url = (bytes) => base64(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const b64urlJson = (obj) => b64url(enc.encode(JSON.stringify(obj)));

function pemToPkcs8Bytes(pem) {
  const body = String(pem)
    .replace(/-----BEGIN [^-]+-----/g, '')
    .replace(/-----END [^-]+-----/g, '')
    .replace(/\s+/g, '');
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
}

let notifyTokenCache = { token: null, expiresAt: 0 };

// توكن OAuth2 لحساب الخدمة: JWT موقّع بمفتاح RSA الخاص ثم يُبادل في Google
async function notifyAccessToken(env) {
  if (notifyTokenCache.token && Date.now() < notifyTokenCache.expiresAt) {
    return notifyTokenCache.token;
  }
  const raw = env.NOTIFICATION_SERVICE_KEY;
  if (!raw) throw new Error('notify_not_configured');
  let account;
  try {
    account = JSON.parse(dec.decode(Uint8Array.from(atob(raw), (c) => c.charCodeAt(0))));
  } catch {
    throw new Error('notify_key_invalid');
  }
  const clientEmail = account?.client_email;
  const privateKey = account?.private_key;
  if (!clientEmail || !privateKey) throw new Error('notify_key_invalid');

  const key = await crypto.subtle.importKey(
    'pkcs8',
    pemToPkcs8Bytes(privateKey),
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const now = Math.floor(Date.now() / 1000);
  const assertion = `${b64urlJson({ alg: 'RS256', typ: 'JWT' })}.${b64urlJson({
    iss: clientEmail,
    scope: FCM_SCOPE,
    aud: OAUTH_TOKEN_URL,
    iat: now,
    exp: now + 3600,
  })}`;
  const signature = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, enc.encode(assertion));
  const jwt = `${assertion}.${b64url(new Uint8Array(signature))}`;

  const res = await fetch(OAUTH_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${encodeURIComponent(jwt)}`,
  });
  if (!res.ok) throw new Error('oauth_failed');
  const data = await res.json();
  if (typeof data.access_token !== 'string') throw new Error('oauth_failed');
  const ttlMs = (Number(data.expires_in) - 60) * 1000;
  notifyTokenCache = { token: data.access_token, expiresAt: Date.now() + ttlMs };
  return data.access_token;
}

// قراءة توكنات FCM للمستخدم من Firestore (حساب الخدمة يتجاوز قواعد الأمان)
async function readFcmTokens(accessToken, projectId, userId) {
  const res = await fetch(FIRESTORE_DOC_URL(projectId, userId), {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (res.status === 404) return [];
  if (!res.ok) throw new Error('firestore_read_failed');
  const doc = await res.json();
  const values = doc?.fields?.fcmTokens?.arrayValue?.values;
  if (!Array.isArray(values)) return [];
  return values.map((v) => v?.stringValue).filter((t) => typeof t === 'string' && t.length > 0);
}

// إرسال رسالة FCM v1 لجهاز واحد
async function sendFcmMessage(accessToken, projectId, token, title, body, data) {
  const res = await fetch(FCM_SEND_URL(projectId), {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json; charset=utf-8',
    },
    body: JSON.stringify({
      message: { token, notification: { title, body }, data, android: { priority: 'HIGH' } },
    }),
  });
  return res.ok;
}

async function handleNotify(request, env) {
  const projectId = env.FIREBASE_PROJECT_ID;
  if (!projectId) return json({ error: 'server_misconfigured' }, 500);

  const match = /^Bearer (.+)$/.exec(request.headers.get('Authorization') || '');
  if (!match) return json({ error: 'unauthorized' }, 401);
  try {
    await verifyFirebaseIdToken(match[1], projectId);
  } catch {
    return json({ error: 'unauthorized' }, 401);
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: 'invalid_json' }, 400);
  }
  const userIds = body?.userIds;
  if (
    !Array.isArray(userIds) ||
    userIds.length === 0 ||
    userIds.length > 200 ||
    !userIds.every((u) => typeof u === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(u))
  ) {
    return json({ error: 'invalid_user_ids' }, 400);
  }
  const title = body?.title;
  if (typeof title !== 'string' || title.length === 0 || title.length > 100) {
    return json({ error: 'invalid_title' }, 400);
  }
  const text = body?.body;
  if (typeof text !== 'string' || text.length > 500) {
    return json({ error: 'invalid_body' }, 400);
  }
  // قيم data في FCM يجب أن تكون نصوصاً
  const data = {};
  const rawData = body?.data;
  if (rawData && typeof rawData === 'object' && !Array.isArray(rawData)) {
    for (const [k, v] of Object.entries(rawData)) {
      if (typeof k === 'string' && k.length > 0 && k.length <= 64) data[k] = String(v);
    }
  }

  let accessToken;
  try {
    accessToken = await notifyAccessToken(env);
  } catch (e) {
    if (e?.message === 'notify_not_configured') {
      return json({ error: 'notify_not_configured' }, 503);
    }
    return json({ error: 'notify_unavailable' }, 502);
  }

  let sent = 0;
  let failed = 0;
  for (const userId of userIds) {
    let tokens;
    try {
      tokens = await readFcmTokens(accessToken, projectId, userId);
    } catch {
      failed += 1;
      continue;
    }
    for (const token of tokens) {
      try {
        if (await sendFcmMessage(accessToken, projectId, token, title, text, data)) sent += 1;
        else failed += 1;
      } catch {
        failed += 1;
      }
    }
  }
  return json({ sent, failed });
}

// ---------- نقطة الدخول ----------
const json = (body, status = 200, headers = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      ...headers,
    },
  });

export default {
  async fetch(request, env) {
    const path = new URL(request.url).pathname;
    if (path !== '/token' && path !== '/notify') return json({ error: 'not_found' }, 404);
    if (request.method !== 'POST') return json({ error: 'method_not_allowed' }, 405, { Allow: 'POST' });
    if (path === '/notify') return handleNotify(request, env);

    const appId = env.AGORA_APP_ID;
    const appCertificate = env.AGORA_APP_CERTIFICATE;
    const projectId = env.FIREBASE_PROJECT_ID;
    if (!isHex32(appId) || !isHex32(appCertificate) || !projectId) {
      return json({ error: 'server_misconfigured' }, 500);
    }

    const match = /^Bearer (.+)$/.exec(request.headers.get('Authorization') || '');
    if (!match) return json({ error: 'unauthorized' }, 401);
    let firebaseUid;
    try {
      firebaseUid = await verifyFirebaseIdToken(match[1], projectId);
    } catch {
      return json({ error: 'unauthorized' }, 401);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: 'invalid_json' }, 400);
    }
    const channel = body?.channel;
    const role = body?.role;
    if (typeof channel !== 'string' || !/^[A-Za-z0-9_-]{1,64}$/.test(channel)) {
      return json({ error: 'invalid_channel' }, 400);
    }
    if (role !== 'host' && role !== 'audience') return json({ error: 'invalid_role' }, 400);

    const uid = await agoraUidFor(firebaseUid);
    const token = await buildRtcToken({
      appId,
      appCertificate,
      channelName: channel,
      uid,
      publisher: role === 'host',
      expireSeconds: TOKEN_TTL_SECONDS,
    });
    return json({ token, uid, channel, expiresIn: TOKEN_TTL_SECONDS });
  },
};
