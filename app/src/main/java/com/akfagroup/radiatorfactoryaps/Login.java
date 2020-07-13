package com.akfagroup.radiatorfactoryaps;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

//---------ДАЕТ ПОЛЬЗОВАТЕЛЮ АВТОРИЗОВАТЬСЯ-------//
//---------ЕСЛИ ПРИ ПРЕДЫДУЩЕМ ЛОГИНЕ ЧЕКНУЛ "ЗАПОМНИТЬ МЕНЯ", SPLASHACTIVITY ПРОПУСТИТ ЛОГИН АКТИВИТИ-------//

public class Login extends AppCompatActivity {
    //layout views
    private TextView loading;
    private EditText passwordView, loginView;
    private Button enter;
    private CheckBox rememberMe;
    //данные, введенные в полях edit text
    private String login, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //возьми разрешение использования камеры
        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionStatus == PackageManager.PERMISSION_DENIED) { ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 0); }
//        permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY);
//        if (permissionStatus == PackageManager.PERMISSION_DENIED) { ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 0); }
//        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        if(!n.isNotificationPolicyAccessGranted()) {
//            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
//            startActivity(intent);
//        }

        initInstances();
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //кнопка ВОЙТИ
                //----ЗДЕСЬ САМОЕ ОСНОВНОЕ ЭТОГО АКТИВИТИ, ПРОВЕРЯЕТ ПРАВИЛЬНОСТЬ ВВОДА, РЕШАЕТ, КАКОЙ АКТИВИТЕ ЗАПУСТИТЬ ДАЛЬШЕ И ТД----//
                login = loginView.getText().toString(); //введенные в edittext логин и пароль
                password = passwordView.getText().toString();
                if(login.isEmpty() || password.isEmpty() || password.charAt(0) == ' ' || login.charAt(0) == ' ') //если пустые поля или начинаются с пробела - ошибка
                {
                    Toast.makeText(getApplicationContext(), "Заполните оба поля корректно", Toast.LENGTH_SHORT).show();
                }
                else {
                    loading.setVisibility(View.VISIBLE); //кликнул, пока БД не считали, стоит надпись ЗАГРУЗКА ДАННЫХ
                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users/" + login); //ссылка на node именно этого пользователя
                    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot user) {
                            loading.setVisibility(View.INVISIBLE);
                            if (user.exists()) // если подветка такая существует
                            {//3 ifs with boolean checker functions in condition: isOperator, isMaster, isRepairer
                                if (user.child("password").getValue().toString().equals(password)) { //если и пароль введен правильно (проверка с подветкой user->password
                                    if (user.child("position").getValue().toString().equals("operator")) { //ОПЕРАТОР
                                        Intent openPult = new Intent(getApplicationContext(), PultActivity.class); //открой пульт
                                        openPult.putExtra("Логин пользователя", login);
                                        openPult.putExtra("Должность", user.child("position").getValue().toString());
                                        startActivity(openPult);
                                    } else if (user.child("position").getValue().toString().equals("master")) {//МАСТЕР
                                        Intent openUrgentProblemsList = new Intent(getApplicationContext(), QuestListOfEquipment.class); //actually there should be the FactoryCondition.class, but it is incomplete yet
                                        openUrgentProblemsList.putExtra("Логин пользователя", login);
                                        openUrgentProblemsList.putExtra("Должность", user.child("position").getValue().toString());
                                        startActivity(openUrgentProblemsList);
                                    } else if (user.child("position").getValue().toString().equals("repair")) { //РЕМОНТНИК
                                        Intent openUrgentProblemsList = new Intent(getApplicationContext(), QuestListOfEquipment.class);
                                        openUrgentProblemsList.putExtra("Логин пользователя", login);
                                        openUrgentProblemsList.putExtra("Должность", user.child("position").getValue().toString());
                                        startActivity(openUrgentProblemsList);
                                    } else if (user.child("position").getValue().toString().equals("raw") || user.child("position").getValue().toString().equals("quality")) { //ОТК / СЫРЬЕ
                                        Intent openUrgentProblemsList = new Intent(getApplicationContext(), UrgentProblemsList.class);
                                        openUrgentProblemsList.putExtra("Логин пользователя", login);
                                        openUrgentProblemsList.putExtra("Должность", user.child("position").getValue().toString());
                                        startActivity(openUrgentProblemsList);
                                    } else if(user.child("position").getValue().toString().equals("head")){
                                        Intent openTodayChecks = new Intent(getApplicationContext(), TodayChecks.class);
                                        openTodayChecks.putExtra("Логин пользователя", login);
                                        openTodayChecks.putExtra("Должность", user.child("position").getValue().toString());
                                        startActivity(openTodayChecks);
                                    }

                                    if (rememberMe.isChecked()) { // был ли отмечен чекбокс "ЗАПОМНИ МЕНЯ"? Если да, запиши логин и должность в SharedPrefs
                                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                        SharedPreferences.Editor editor = sharedPrefs.edit();
                                        editor.putString("Логин пользователя", login);
                                        editor.putString("Должность", user.child("position").getValue().toString());
                                        editor.commit(); //запиши данные в sharedPref
                                    }

                                    if(!user.child("position").getValue().toString().equals("head")) {
                                        //если это любой специалист, кроме ГЛАВНЫХ, включи фоновый сервис слежения за появлением проблем (срочный и ТО), вызовов
                                        //при появлении проблемы, будет отправлять уведомление
                                        stopService(new Intent(getBaseContext(), BackgroundService.class)); //если до этого уже сервис для другого аккаунта был включен и произошел повторный логин
                                        Intent startBackgroundService = new Intent(getApplicationContext(), BackgroundService.class);
                                        startBackgroundService.putExtra("Должность", user.child("position").getValue().toString()); //уведомления сортируются в зависимости от должности и логина пользователя
                                        startBackgroundService.putExtra("Логин пользователя", user.getKey());
                                        ContextCompat.startForegroundService(getApplicationContext(), startBackgroundService);//эта функция запускает фоновый сервис
                                        startService(new Intent(getApplicationContext(), AppLifecycleTrackerService.class));
                                        finish();
                                    }
                                }
                                else { Toast.makeText(getApplicationContext(), "Неверный пароль", Toast.LENGTH_LONG).show(); }
                            }
                            else { Toast.makeText(getApplicationContext(), "Такого пользователя не существует\nВнимательно заполните поля", Toast.LENGTH_LONG).show(); }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }
        });
    }

    private void initInstances()
    {//иниц views
        getSupportActionBar().hide();
        loginView = findViewById(R.id.access_login);
        passwordView = findViewById(R.id.password);
        enter = findViewById(R.id.enter);
        loading = findViewById(R.id.loading);
        loading.setVisibility(View.INVISIBLE);
        rememberMe = findViewById(R.id.remember_me);
    }
}
