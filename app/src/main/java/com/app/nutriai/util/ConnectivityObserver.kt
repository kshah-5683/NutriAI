package com.app.nutriai.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that exposes device internet connectivity as a [Flow<Boolean>].
 *
 * The flow emits:
 *  - `true`  when the active network has [NetworkCapabilities.NET_CAPABILITY_VALIDATED]
 *    (device successfully reached the internet, not just a captive portal).
 *  - `false` when all networks are lost or the validated capability is revoked.
 *
 * The flow emits the current state immediately on first collection, then
 * updates reactively via [ConnectivityManager.NetworkCallback].
 *
 * [distinctUntilChanged] prevents redundant recompositions when the capability
 * status toggles rapidly (e.g. Wi-Fi → mobile handoff).
 *
 * Requires: `ACCESS_NETWORK_STATE` permission in AndroidManifest.xml (already granted).
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network is available — check if it's actually validated
                val caps = connectivityManager.getNetworkCapabilities(network)
                trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }

            override fun onLost(network: Network) {
                // Re-check remaining networks — there may still be another connected network
                val active = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(active)
                trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit the current state immediately so the UI has a value before
        // any callback fires (avoids a momentary false "offline" flash).
        val current = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        trySend(current?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
