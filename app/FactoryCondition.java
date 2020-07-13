package com.akfa.apsproject;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

//------ВЕБ БРАУЗЕР СОСТОЯНИЯ ПРОИЗВОДСТВА, ВЫВОДЯЩИЙ САЙТ В WebView------//
//------Мастер и ремонтик имеет доступ к данному активити-------//
public class FactoryCondition extends AppCompatActivity {
    WebView webview;
    ActionBarDrawerToggle toggle;
    String employeeLogin, employeePosition; //кросс-активити переменные

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.factory_condition);
        initInstances();
        toggle = InitNavigationBar.setUpNavBar(FactoryCondition.this, getApplicationContext(),  getSupportActionBar(), employeeLogin, employeePosition, R.id.web_monitoring, R.id.factory_condition);
        setTitle("Веб-мониторинг");
        //иниц WebView
        webview.setWebViewClient(new WebViewClient());
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webview.loadUrl(getString(R.string.website_address_factory_condition));
    }

    // find all variables
    protected void initInstances(){
        webview=findViewById(R.id.webview);
        employeeLogin = getIntent().getExtras().getString("Логин пользователя");
        employeePosition = getIntent().getExtras().getString("Должность");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        }
        else {
            if (isTaskRoot()) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (sharedPrefs.getString("Логин пользователя", null) == null) //Еcли в sharedPrefs есть данные юзера, открой соот активти
                {
                    stopService(new Intent(getApplicationContext(), BackgroundService.class)); //если до этого уже сервис был включен, выключи сервис
                    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    stopService(new Intent(getApplicationContext(), BackgroundService.class));
                    final Handler handler = new Handler();
                    Runnable runnableCode = new Runnable() {
                        @Override
                        public void run() {
                            //do something you want
                            //stop service
                            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            if (sharedPrefs.getString("Логин пользователя", null) == null) //Еcли в sharedPrefs есть данные юзера, открой соот активти
                            {
                                stopService(new Intent(getApplicationContext(), BackgroundService.class)); //если до этого уже сервис был включен, выключи сервис
                            }
                            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.cancelAll();

                        }
                    };
                    handler.postDelayed(runnableCode, 12000);
                }
            }
            super.onBackPressed();
        }
    }
}