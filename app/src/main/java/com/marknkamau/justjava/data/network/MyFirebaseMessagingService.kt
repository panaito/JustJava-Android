package com.marknkamau.justjava.data.network

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.marknjunge.core.auth.AuthService
import com.marknjunge.core.data.firebase.ClientDatabaseService
import com.marknjunge.core.data.firebase.WriteListener
import com.marknkamau.justjava.JustJavaApp
import com.marknkamau.justjava.utils.NotificationHelper
import org.koin.android.ext.android.inject
import org.koin.core.KoinComponent
import timber.log.Timber

/**
 * Created by Mark Njung'e.
 * mark.kamau@outlook.com
 * https://github.com/MarkNjunge
 */

class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {
    private val notificationHelper: NotificationHelper by inject()
    private val broadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    private val authService: AuthService by inject()
    private val databaseService: ClientDatabaseService by inject()

    companion object {
        const val MPESA_ORDER_PAID_ACTION = "mpesa"
        const val ORDER_ID = "orderId"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            notificationHelper.showNotification(it.title ?: "JustJava", it.body ?: "")
        }

        remoteMessage.data?.let {
            Timber.d(it.toString())
            when(it["reason"]){
                "completed-order" -> {
                    notificationHelper.showCompletedOrderNotification()
                }
                "mpesa" -> {
                    // Show notification
                    notificationHelper.showPaymentNotification(it["body"]!!)

                    // Send local broadcast so that if the user is on the order's page, the status updates
                    if (it["status"] == "completed") {
                        val intent = Intent(MPESA_ORDER_PAID_ACTION)
                        intent.putExtra(ORDER_ID,  it["orderId"])
                        broadcastManager.sendBroadcast(intent)
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        if (authService.isSignedIn()){
            val user = authService.getCurrentUser()

            databaseService.updateUserFcmToken(user.userId, object : WriteListener {
                override fun onError(reason: String) {
                    Timber.e(reason)
                }

                override fun onSuccess() {
                    Timber.i("FCM token saved")
                }

            })
        }
    }
}