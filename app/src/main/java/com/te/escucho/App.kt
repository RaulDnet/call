package com.te.escucho

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.te.escucho.services.LinphoneService

class App : Application(){

    override fun onCreate(){
        super.onCreate()
        createNotificationChannel()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val notificationChannel: NotificationChannel = NotificationChannel(
                LinphoneService.NOTIF_CHANNEL_ID,
                "DNETCALL",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager? = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel);
        }
    }
}