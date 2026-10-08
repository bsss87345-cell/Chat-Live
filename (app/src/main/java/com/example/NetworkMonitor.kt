package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private fun ConnectivityManager.hasInternet(): Boolean {
    val caps = getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun Context.observeInternet(): Flow<Boolean> = callbackFlow {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(cm.hasInternet()) }
        override fun onLost(network: Network) { trySend(cm.hasInternet()) }
        override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) {
            trySend(cm.hasInternet())
        }
    }
    trySend(cm.hasInternet())
    cm.registerNetworkCallback(
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build(),
        callback
    )
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

@Composable
fun NoInternetScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D12)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "لا يوجد اتصال بالإنترنت\nتحقق من الشبكة وحاول مرة أخرى",
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
