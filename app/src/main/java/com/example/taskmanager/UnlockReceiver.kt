package com.example.taskmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UnlockReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "UnlockReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action || Intent.ACTION_SCREEN_ON == intent.action) {
            Log.d(TAG, "Device unlocked, starting MainActivity")
            
            // 启动主Activity
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            context.startActivity(activityIntent)
        }
    }
}