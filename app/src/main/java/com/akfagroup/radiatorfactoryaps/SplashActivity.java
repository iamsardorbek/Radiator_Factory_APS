package com.akfagroup.radiatorfactoryaps;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

//-------SPLASH ЭКРАН, КОТОРЫЙ ПОКАЗЫВАЕТСЯ ПРИ ЗАПУСКЕ ПРИЛОЖЕНИЯ--------//
//-------ЕСЛИ В SHARED PREFS ЕСТЬ ДАННЫЕ О ЮЗЕРЕ, ОТКРЫВАЕТ СООТ. АКТИВИТИ; ЕСЛИ НЕТ - ОТКРЫВАЕТ ЛОГИН АКТИВИТИ
public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION = 2000; //сколько должен длиться splash
    Handler handler;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_new);
        getSupportActionBar().hide();

        if(android.os.Build.VERSION.SDK_INT != 27)
        {
            grantNotificationPolicyAccess(); //дать доступ контролировать звук уведомлений
        }
        else {
            //!!!! это скорее всего артель u3 (у него api 27) на котором DND функции нету, как проверить dnd mode я еще не знаю, пока это кустарное решение
        }
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPrefs.getString("Логин пользователя", null) != null) //Еcли в sharedPrefs есть данные юзера, открой соот активти
        {
            String rememberedLogin = sharedPrefs.getString("Логин пользователя", null);
            String rememberedPosition = sharedPrefs.getString("Должность", null);
            switch (rememberedPosition)
            {
                case "operator":
                    Intent openPult = new Intent(getApplicationContext(), PultActivity.class);
                    openPult.putExtra("Логин пользователя", rememberedLogin);
                    openPult.putExtra("Должность", rememberedPosition);
                    keepSplash(openPult);
                    break;
                case "master":
                    Intent openFactoryCondition = new Intent(getApplicationContext(), QuestListOfEquipment.class); //actually there should be the FactoryCondition.class, but it is incomplete yet
                    openFactoryCondition.putExtra("Логин пользователя", rememberedLogin);
                    openFactoryCondition.putExtra("Должность", rememberedPosition);
                    keepSplash(openFactoryCondition);
                    break;
                case "repair":
                    Intent openProblemsList = new Intent(getApplicationContext(), QuestListOfEquipment.class);
                    openProblemsList.putExtra("Логин пользователя", rememberedLogin);
                    openProblemsList.putExtra("Должность", rememberedPosition);
                    keepSplash(openProblemsList);
                    break;
                case "raw":
                case "quality":
                    Intent openUrgentProblemsList = new Intent(getApplicationContext(), UrgentProblemsList.class);
                    openUrgentProblemsList.putExtra("Логин пользователя", rememberedLogin);
                    openUrgentProblemsList.putExtra("Должность", rememberedPosition);
                    startActivity(openUrgentProblemsList);
                    break;
                case "head":
                    Intent openTodayChecks = new Intent(getApplicationContext(), TodayChecks.class);
                    openTodayChecks.putExtra("Логин пользователя", rememberedLogin);
                    openTodayChecks.putExtra("Должность", rememberedPosition);
                    startActivity(openTodayChecks);
                    break;
            }
            if(!rememberedPosition.equals("head")) {
                //запусти сервис
                startBGServ(rememberedPosition, rememberedLogin);
            }

        }
        else
        {
            Intent intent=new Intent(SplashActivity.this, Login.class);
            keepSplash(intent);
        }

    }

    private void grantNotificationPolicyAccess(){

        //чтобы регулировать програмно звук уведомлений
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        if (intent.resolveActivity(getPackageManager()) != null) {
            //защита от случая, когда активити для предоставления доступа контролировать DND Mode нету (на артель U3)
            if(!n.isNotificationPolicyAccessGranted()) {
                startActivityForResult( intent, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // создает типа рекурсию, пока не дать доступ контролировать режим Do not disturb
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            grantNotificationPolicyAccess();
        }
    }

    private void startBGServ(String rememberedPosition, String rememberedLogin)
    {
        stopService(new Intent(getBaseContext(), BackgroundService.class)); //если до этого уже сервис для другого аккаунта был включен и произошел повторный логин, для безопасности выключи сервис
        Intent startBackgroundService = new Intent(getApplicationContext(), BackgroundService.class);
        startBackgroundService.putExtra("Должность", rememberedPosition);
        startBackgroundService.putExtra("Логин пользователя", rememberedLogin);
        //эта функция запускает фоновый сервис проверки наличия новообнаруженных проблем и неполадок
        ContextCompat.startForegroundService(getApplicationContext(), startBackgroundService);
    }

    private void keepSplash(final Intent intent)
    {
        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                startActivity(intent);
                finish();
            }
        },SPLASH_DURATION);
    }
}
