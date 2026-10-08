// Chat Live — خادم التوكن (Cloudflare Worker)
// 1) يتحقق أن المستخدم مسجّل دخول عبر توكن Firebase (Authorization: Bearer).
// 2) يُصدر توكن Agora RTC صالحاً لساعة للغرفة المطلوبة.
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
    if (new URL(request.url).pathname !== '/token') return json({ error: 'not_found' }, 404);
    if (request.method !== 'POST') return json({ error: 'method_not_allowed' }, 405, { Allow: 'POST' });

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
