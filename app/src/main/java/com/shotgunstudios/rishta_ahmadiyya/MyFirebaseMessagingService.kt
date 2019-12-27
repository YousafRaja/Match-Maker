package com.shotgunstudios.rishta_ahmadiyya

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shotgunstudios.rishta_ahmadiyya.activities.RishtaActivity
import com.shotgunstudios.rishta_ahmadiyya.activities.RishtaCallback
import android.app.ActivityManager.RunningTaskInfo
import androidx.core.content.ContextCompat.getSystemService
import android.app.ActivityManager
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.storage.StorageTaskScheduler.sInstance
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager




class MyFirebaseMessagingService : FirebaseMessagingService() {

    val TAG = String::class.java.simpleName
    private var callback: RishtaCallback? = null
    private var userId: String? = ""
    private val ADMIN_CHANNEL_ID = "admin_channel"


    private var chatingWith = ""


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage?.from}")

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            var title = remoteMessage.data.get("title").toString()
            var msg = remoteMessage.data.get("message").toString()
            if(title=="CHATTING WITH:"){ //keep track of who we're chatting with, should be an empty string if not chatting
                chatingWith = msg
                return
            }
            if(title=="New Message:"+chatingWith){ //no need to send a notification when already in chat
                return
            }

            // Compose and show notification
            if (!remoteMessage.data.isNullOrEmpty()) {
                sendNotification(msg, title)
            }

        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            sendNotification(remoteMessage.notification?.body, remoteMessage.notification?.title)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupChannels(notificationManager: NotificationManager?) {
        val adminChannelName = "New notification"
        val adminChannelDescription = "Device to devie notification"

        val adminChannel: NotificationChannel
        adminChannel = NotificationChannel(ADMIN_CHANNEL_ID, adminChannelName, NotificationManager.IMPORTANCE_HIGH)
        adminChannel.description = adminChannelDescription
        adminChannel.enableLights(true)
        adminChannel.lightColor = Color.RED
        adminChannel.enableVibration(true)
        notificationManager?.createNotificationChannel(adminChannel)
    }

    private fun sendNotification(messageBody: String?, messageTitle: String?) {
        val intent = Intent(this, RishtaActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        var title = messageTitle
        if(messageTitle!!.contains("New Message:"))title="New Message"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ahmadiyya_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        // https://developer.android.com/training/notify-user/build-notification#Priority
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
    }





}