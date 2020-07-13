package com.akfa.apsproject;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

//----------не дает уведомлениям высвечиваться, когда юзер без "запомни меня" выходит из приложения----------//
public class AppLifecycleTrackerService extends Service {
    @Override //при запуске сервиса
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPrefs.getString("Логин пользователя", null) == null) //Еcли в sharedPrefs есть данные юзера, открой соот активти
        {
            stopService(new Intent(getApplicationContext(), BackgroundService.class)); //если до этого уже сервис был включен, выключи сервис
            NotificationManager notificationManager = (NotificationManager)  getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            stopService(new Intent(getApplicationContext(), BackgroundService.class));
            final Handler handler = new Handler();
            Runnable runnableCode = new Runnable() {
                @Override
                public void run() {
                    //do something you want
                    //stop service
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    if(sharedPrefs.getString("Логин пользователя", null) == null) //Еcли в sharedPrefs есть данные юзера, открой соот активти
                    {
                        stopService(new Intent(getApplicationContext(), BackgroundService.class)); //если до этого уже сервис был включен, выключи сервис
                    }
                    NotificationManager notificationManager = (NotificationManager)  getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();

                }
            };
            handler.postDelayed(runnableCode, 12000);
        }
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
