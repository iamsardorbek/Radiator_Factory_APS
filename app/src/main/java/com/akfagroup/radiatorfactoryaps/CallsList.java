package com.akfagroup.radiatorfactoryaps;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

//----------ПОКАЗЫВАЕТ, АКТИВНЫЕ ВЫЗОВЫ ДАННОГО ПОЛЬЗОВАТЕЛЯ ДРУГИМИ СПЕЦИАЛИСТАМИ--------//

public class CallsList extends AppCompatActivity {
    private static final int ID_TEXTVIEWS = 5000;
    private int problemCount = 0;
    private String employeeLogin, employeePosition;
    private List<String> callsKeys;


    private ActionBarDrawerToggle toggle;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_or_master_calls_list);
        setTitle("Нет вызовов");
        linearLayout = findViewById(R.id.linearLayout);
        Bundle arguments = getIntent().getExtras();
        employeeLogin = arguments.getString("Логин пользователя");
        employeePosition = arguments.getString("Должность");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users/" + employeeLogin);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull final DataSnapshot userSnap) {
                DatabaseReference сallsRef = FirebaseDatabase.getInstance().getReference("Calls");
                сallsRef.addValueEventListener(new ValueEventListener() {
                    @SuppressLint("ResourceType")
                    @Override public void onDataChange(@NonNull DataSnapshot сallsSnap) {
                        linearLayout.removeAllViews(); //для обновления данных удали все результаты предыдущего поиска
                        callsKeys = new ArrayList<>();
                        problemCount = 0;
                        if(сallsSnap.getValue() == null) //если ветка Urgent_problems пуста/не сущ -> дай знать, что все проблемы уже решены
                            setTitle("Нет вызовов");
                        else {
                            setTitle("Нет вызовов");
                            for (final DataSnapshot singleCallSnap : сallsSnap.getChildren()) {
                                final Call thisCall = singleCallSnap.getValue(Call.class);

                                String whoIsNeededPosition = thisCall.getWho_is_needed_position();
                                String callEquipmentName = thisCall.getEquipment_name();
                                String callShopName = thisCall.getShop_name();
                                boolean callIsComplete = thisCall.getComplete();

                                if (whoIsNeededPosition.equals(employeePosition) && !callIsComplete) {
                                    //условия query: должность вызванного и этого юзера соотвествуют, это цех, за который ответственен данный мастер/оператор,
                                    // а также этот вызов еще не был удовлетворен (тобиш вызываемый еще не пришел)
                                    boolean operatorIsResponsibleForThisEquipment = false, masterIsResponsibleForThisShop = false;;
                                    final String equipmentName;
                                    final String shopName;
                                    if (employeePosition.equals("operator")) //если оператор, нам нужно проверить, это линия, за которую он отвечает?
                                    //если да - добавь этот вызов в список, если нет - не добавляй/не показывай этот вызов оператору
                                    {
                                        shopName = userSnap.child("shop_name").getValue().toString();
                                        equipmentName = userSnap.child("equipment_name").getValue().toString();
                                        if (callEquipmentName.equals(equipmentName) && callShopName.equals(shopName) ) { //если оператор отвесвенен за данную линию
                                            operatorIsResponsibleForThisEquipment = true;
                                        }
                                    }
                                    else if (employeePosition.equals("master")) //если мастер, нам нужно проверить, это цех, за который он отвечает?
                                    //если да - добавь этот вызов в список, если нет - не добавляй/не показывай этот вызов мастеру
                                    {
                                        final String shopNameRepairer = userSnap.child("shop_name").getValue().toString();
                                        if (callShopName.equals(shopNameRepairer)) { //если мастер отвесвенен за данный цех
                                            masterIsResponsibleForThisShop = true;
                                        }
                                    }

                                    if ((operatorIsResponsibleForThisEquipment && employeePosition.equals("operator")) || (employeePosition.equals("master") && masterIsResponsibleForThisShop)
                                            || employeePosition.equals("repair")) { //если оператор ответсвенен за эту лини. или это цех мастера или это просто ремонтник
                                        //----СОЗДАНИЕ TEXTVIEW, ВНЕСЕНИЕ ДАННЫХ В НЕГО И ИНИЦИАЛИЗАЦИЯ ПАРАМЕТРОВ----//
                                        setTitle("Вас ждут в данных местах");
                                        TextView callInfo;
                                        callInfo = new TextView(getApplicationContext());
                                        //данные об этой проблеме запишем в строку callInfoFromDB
                                        String callInfoFromDB = "Цех: " + thisCall.getShop_name() + "\nОборудование: " + thisCall.getEquipment_name() + "\nПункт №" + thisCall.getPoint_no()
                                                + "\nДата и время вызова: " + thisCall.getDate_called() + " " + thisCall.getTime_called() + "\nВызвал: " + thisCall.getCalled_by();
                                        callInfo.setText(callInfoFromDB);
                                        callInfo.setPadding(25, 25, 25, 25);
                                        callInfo.setId(ID_TEXTVIEWS + problemCount);
                                        callInfo.setTextColor(Color.parseColor(getString(R.color.text)));
                                        callInfo.setTextSize(13);
                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                        params.setMargins(20, 25, 20, 25);

                                        callInfo.setLayoutParams(params);
                                        callInfo.setClickable(true);
                                        callInfo.setBackgroundResource(R.drawable.list_group_layout);
                                        callInfo.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Intent openQR = new Intent(getApplicationContext(), QRScanner.class);
                                                openQR.putExtra("Открой PointDynamic", "реагирование на вызов"); //описание действия для QR сканера
                                                openQR.putExtra("Должность", employeePosition);
                                                openQR.putExtra("Название линии", thisCall.getEquipment_name());
                                                openQR.putExtra("Название цеха", thisCall.getShop_name());
                                                openQR.putExtra(getString(R.string.nomer_punkta_textview_text), thisCall.getPoint_no());
                                                openQR.putExtra("Код вызова", singleCallSnap.getKey());
                                                openQR.putExtra("Логин пользователя", employeeLogin); //передавать логин пользователя взятый из Firebase
                                                startActivity(openQR);
                                            }
                                        });
                                        //----КОНЕЦ ИНИЦИАЛИЗАЦИИ TEXTVIEW ДЛЯ СРОЧНОЙ ПРОБЛЕМЫ----//
                                        linearLayout.addView(callInfo); //добавить textview в linearLayout
                                        callsKeys.add(singleCallSnap.getKey());
                                        problemCount++; //итерировать для уникализации айдишек textviews
                                    }
                                }

                            }
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                });

            }

            @Override public void onCancelled (@NonNull DatabaseError databaseError){ }
        });

        toggle = InitNavigationBar.setUpNavBar(CallsList.this, getApplicationContext(), getSupportActionBar(), employeeLogin, employeePosition, R.id.calls, R.id.activity_operator_or_master_calls_list); //инициализация navigation bar
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
//функция нужная, чтобы нав бар работал
        if(toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        if(isTaskRoot()) {
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
