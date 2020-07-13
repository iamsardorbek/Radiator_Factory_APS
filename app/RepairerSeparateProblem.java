package com.akfa.apsproject;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

//--------ПОКАЗЫВАЕТ БОЛЬШЕ ДЕТАЛЕЙ ПРО ТО ПРОБЛЕМУ, ЧЕМ В REPAIRERS PROBLEM LIST. ПРИ НАЖАТИИ НА КНОПКУ "ПРОБЛЕМА РЕШЕНА"-------
//--------ОТКРЫВАЕТ QR И ДАЕТ ВОЗМОЖНОСТЬ ПРИКРЕПИТЬ ФОТКУ РЕШЕНИЯ---------//
public class RepairerSeparateProblem extends AppCompatActivity implements View.OnTouchListener {
    Button problemSolved, callOperator;
    ImageView problemPic;

    private String IDOfTheProblem;
    private int nomerPunkta, equipmentNo, shopNo;
    private String equipmentName, shopName;
    private String employeeLogin, employeePosition;
    private MaintenanceProblem problem;
    private boolean callForOperatorOpen = false;

    DatabaseReference problemsRef, thisProblemRef, callRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.repairer_activity_separate_problem);
        setTitle(getString(R.string.separate_problem_title));
        initInstances();
    }

    private void initInstances() {
        problemsRef = getInstance().getReference().child("Maintenance_problems"); //ссылка к ТО пробам
        IDOfTheProblem = getIntent().getExtras().getString("ID проблемы в таблице Maintenance_problems"); //айди именно текущей проблемы
        employeeLogin = getIntent().getExtras().getString("Логин пользователя"); //кросс-активити перем
        employeePosition = getIntent().getExtras().getString("Должность");

        thisProblemRef = problemsRef.child(IDOfTheProblem);
        thisProblemRef.addListenerForSingleValueEvent(new ValueEventListener() { //единожды загрузим данные про текущ пробу
            @Override public void onDataChange(@NonNull DataSnapshot problemDataSnapshot) {
                problem = problemDataSnapshot.getValue(MaintenanceProblem.class); //считай данные пробы в объект
                //на месте иниц views и задай их текст
                TextView shopNameTextView = findViewById(R.id.shop_name);
                TextView equipmentNameTextView = findViewById(R.id.equipment_name);
                TextView pointNo = findViewById(R.id.point_no);
                TextView subpointNo = findViewById(R.id.subpoint_no);
                TextView employeeLogin = findViewById(R.id.employee_login);
                TextView date = findViewById(R.id.date);
                shopNameTextView.setText(problem.getShop_name());
                equipmentNameTextView.setText(problem.getEquipment_line_name());
                pointNo.setText(Integer.toString(problem.getPoint_no()));
                subpointNo.setText(Integer.toString(problem.getSubpoint_no()));
                employeeLogin.setText(problem.getDetected_by_employee());
                date.setText(problem.getDate() + " " + problem.getTime());

                //переменные для передачи в QR Scanner
                nomerPunkta = problem.getPoint_no();
                equipmentName = problem.getEquipment_line_name();
                equipmentNo = problem.getEquipment_line_no();
                shopNo = problem.getShop_no();
                shopName = problem.getShop_name();
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        DatabaseReference callsRef = getInstance().getReference("Calls");
        callsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot callsSnap) {
                for(DataSnapshot singleCallSnap : callsSnap.getChildren()) {
                    if(singleCallSnap.child("problem_key").exists()) { //если это вызов прямо из RepairersSeparateProblem (без разницы какой ремонтник вызвал)
                        String problemKey = singleCallSnap.child("problem_key").getValue().toString();
                        boolean isCallComplete = (boolean) singleCallSnap.child("complete").getValue();
                        if (problemKey.equals(IDOfTheProblem) && !isCallComplete) //если оператор еще не пришел, а если он уже пришел и потенциально ушел, можно его вызвать снова
                        {
                            String thisCallKey = singleCallSnap.getKey();
                            DatabaseReference callRef = getInstance().getReference("Calls/" + thisCallKey);
                            callRef.addValueEventListener(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot callSnap) {
                                    boolean callSnapComplete = callSnap.child("complete").getValue(Boolean.class);
                                    if (callSnapComplete) { //когда оператор прибыл позже, после первоначальной инициализации этого листенера
                                        callOperator.setBackgroundResource(R.drawable.call_closed_button);
                                        callOperator.setText("Оператор прибыл");
                                        Resources r = getApplicationContext().getResources();
                                        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics());
                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        params.setMargins(px, 2 * px, px, 0);
                                        callOperator.setLayoutParams(params);
                                        callOperator.setClickable(true); //теперь если вдруг уйдет, можно вызывать снова
                                        callForOperatorOpen = false; //вызов уже закрыт, можно вызывать снова
                                    } else {
                                        callForOperatorOpen = true;
                                        callOperator.setClickable(false); //если есть уже активный вызов оператора, еще раз вызвать его нельзя, а то БД заполнится
                                        callOperator.setBackgroundResource(R.drawable.call_opened_button);
                                        callOperator.setText("Оператор вызван");
                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        params.setMargins(5, 40, 5, 40);
                                        params.gravity = Gravity.CENTER;
                                        callOperator.setLayoutParams(params);
                                    }
                                }

                                @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                            });
                        }
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //загрузим фотку с Storage в ImageView с помощью Glide
        problemPic = findViewById(R.id.problemPic);
        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("problem_pictures");
        StorageReference singlePicRef = mStorageRef.child(IDOfTheProblem + ".jpg");
        Glide.with(getApplicationContext()).load(singlePicRef).into(problemPic); //load the pic from FB top imageview

        //действия при нажатии кнопки
        problemSolved = findViewById(R.id.problemSolved);
        problemSolved.setOnTouchListener(this);
        callOperator = findViewById(R.id.call_operator);
        callOperator.setOnTouchListener(this);
        if(employeePosition.equals("head"))
        {
            problemSolved.setVisibility(View.GONE);
            callOperator.setVisibility(View.GONE);
        }
    }

    private void qrStart(int nomerPunkta, int equipmentNo, int shopNo) { //ЗАПУСК QR SCANNER
        Intent intent = new Intent(getApplicationContext(), QRScanner.class);
        intent.putExtra("Номер цеха", shopNo);
        intent.putExtra("Номер линии", equipmentNo);
        intent.putExtra("Номер пункта", nomerPunkta);
        intent.putExtra("Открой PointDynamic", "ремонтник");
        intent.putExtra("Логин пользователя", employeeLogin);
        intent.putExtra("ID проблемы в таблице Maintenance_problems", IDOfTheProblem);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) { //обработка нажатия с эффектом
        if(v.isClickable()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: //затемнена кнопка
                    v.setBackgroundResource(R.drawable.edit_red_accent_pressed);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setBackgroundResource(R.drawable.edit_red_accent);
                    switch (v.getId()) {
                        case R.id.call_operator: //если вызов оператора
                            if (!callForOperatorOpen) {
                                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                                DatabaseReference newCallRef = dbRef.child("Calls").push(); //создать ветку нового вызова
                                //дата-время
                                final String dateCalled;
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                                dateCalled = sdf.format(new Date());
                                final String timeCalled;
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
                                timeCalled = sdf1.format(new Date());
                                //----считал дату и время----//
                                newCallRef.setValue(new Call(dateCalled, timeCalled, employeeLogin, "operator", problem.getPoint_no(),
                                        problem.getEquipment_line_name(), problem.getShop_name(), false, IDOfTheProblem));

                                newCallRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot callSnap) {
                                        boolean callSnapComplete = callSnap.child("complete").getValue(Boolean.class);
                                        if (callSnapComplete) {
                                            callOperator.setBackgroundResource(R.drawable.call_closed_button);
                                            callOperator.setText("Оператор прибыл");
                                            callOperator.setClickable(true); //теперь если вдруг уйдет, можно вызывать снова
                                            callForOperatorOpen = false;
                                        } else {
                                            callForOperatorOpen = true;
                                            callOperator.setClickable(false); //если есть уже активный вызов оператора, еще раз вызвать его нельзя, а то БД заполнится
                                            callOperator.setBackgroundResource(R.drawable.call_opened_button);
                                            callOperator.setText("Оператор вызван");
                                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                            params.setMargins(5, 40, 5, 40);
                                            params.gravity = Gravity.CENTER;
                                            callOperator.setLayoutParams(params);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                    }
                                });
                            }
                            break;
                        case R.id.problemSolved:
                            qrStart(nomerPunkta, equipmentNo, shopNo); //открыть QR Scanner
                            finish();
                            break;
                    }
                    break;
            }
        }
        return false;
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
