package com.taskalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.taskalarm.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Telefon qayta yuklandi. Budilniklar qayta tiklanmoqda...")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val activeTasks = db.taskDao().getActiveTasks()
                    val scheduler = AlarmScheduler(context)

                    for (task in activeTasks) {
                        scheduler.schedule(task)
                    }
                    Log.d("BootReceiver", "${activeTasks.size} ta faol vazifa budilnigi qayta tiklandi.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Budilniklarni tiklashda xatolik: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
