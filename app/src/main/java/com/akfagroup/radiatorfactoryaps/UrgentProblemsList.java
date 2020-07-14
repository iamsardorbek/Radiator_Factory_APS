package com.akfagroup.radiatorfactoryaps;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
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

public class UrgentProblemsList extends AppCompatActivity implements View.OnTouchListener {
    //ЭТОТ КЛАСС СОЗДАН ДЛЯ ТОГО, ЧТО ПОКАЗЫВАТЬ СПЕЦИАЛИСТАМ СООТВЕТСТВУЯ ИХ СПЕЦИАЛЬНОСТИ (МАСТЕР, ОТК, РЕМОНТ И ДР) СПИСОК СРОЧНЫХ ПРОБЛЕМ,
    //ОБНАРУЖЕННЫХ ОПЕРАТОРАМИ И СООБЩЕННЫХ ЧЕРЕЗ ПУЛЬТЫ
    private final int ID_TEXTVIEWS = 5000; //константа чтобы задавать айдишки TextView элементам
    private int problemCount = 0;  //счетчик выводящихся проблем, чтобы уникализировать id каждого TextView
    //private List<String> problemIDs; //лист для сохранения айдишек срочных проблем как в БД, если потребуется сделать так, чтобы специалист шел, и сканировал QR код именно данного оператора
    private Button qrScan; //кнопка, которая стоит внизу, чтобы сразу отсканировать код на экране оператора
    ActionBarDrawerToggle toggle; //для работы navigation bar, инициализируется возвращаемым значением функции setUpNavBar()
    LinearLayout linearLayout; //vertical linear layout, который находится внутри ScrollView; в него и будем добавлять в отдельных textview данные об отдельных срочных проблемах
    String employeeLogin, employeePosition; //логин и должность сотрудника-пользователя, которые мы получим из intent(предыдущих окон)
    View.OnClickListener textviewClickListener; //слушатель кликов textviews

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urgent_problems_list);

        initInstances(); //иниуиализация объектов дизайна и глобальных переменных
        toggle = InitNavigationBar.setUpNavBar(UrgentProblemsList.this, getApplicationContext(), getSupportActionBar(), employeeLogin, employeePosition, R.id.urgent_problems, R.id.activity_urgent_problems_list); //инициализация navigation bar
        addProblemsFromDatabase(); //добавление срочных проблем в linearLayout динамично
    }

    private void initInstances()
    {//иниуиализация объектов дизайна и глобальных переменных
        qrScan = findViewById(R.id.qr_scan);
        linearLayout = findViewById(R.id.linearLayout);
        qrScan.setOnTouchListener(this);
        employeeLogin = getIntent().getExtras().getString("Логин пользователя");
        employeePosition = getIntent().getExtras().getString("Должность");
        initTextViewClickListener();
    }

    private void initTextViewClickListener()
    {//инициализирует действие (открыть QR сканер, чтобы отсканировать код на экране у оператора) при кликах на textviews с срочными проблемами
        //
        textviewClickListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent openQR = new Intent(getApplicationContext(), QRScanner.class);
                openQR.putExtra("Действие", "срочная проблема"); //описание действия для QR сканера
                openQR.putExtra("Должность", employeePosition);
                openQR.putExtra("Логин пользователя", employeeLogin); //передавать логин пользователя взятый из Firebase
                startActivity(openQR);
            }
        };
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //дейсвтие при нажатиях на кнопку (отсканировать QR код)
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                qrScan.setBackgroundResource(R.drawable.edit_red_accent_pressed); //эффект нажатия
                break;
            case MotionEvent.ACTION_UP: //когда уже отпустил, октрой qr
                qrScan.setBackgroundResource(R.drawable.edit_red_accent);
                Intent openQR = new Intent(getApplicationContext(), QRScanner.class);
                openQR.putExtra("Действие", "срочная проблема");//описание действия для QR сканера
                openQR.putExtra("Должность", employeePosition); //передать должность
                openQR.putExtra("Логин пользователя", employeeLogin); //передавать логин пользователя взятый из Firebase
                startActivity(openQR);
                break;
        }
        return false;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
//функция нужная, чтобы нав бар работал
        if(toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    private void addProblemsFromDatabase() {//добавление срочных проблем в linearLayout динамично
        //на самом деле нужно взять количество строк в таблице problems
        DatabaseReference urgentProblemsRef = FirebaseDatabase.getInstance().getReference().child("Urgent_problems"); //ссылка на срочные проблемы
        urgentProblemsRef.addValueEventListener(new ValueEventListener() { //addValueEvent, а не addListenerForSingleValueEvent, потому что нужно мониторить изменения в БД
            @SuppressLint("ResourceType")
            @Override public void onDataChange(@NonNull DataSnapshot urgentProblemsSnap) {
                linearLayout.removeAllViews(); //для обновления данных удали все результаты предыдущего поиска
                if(urgentProblemsSnap.getValue() == null) //если ветка Urgent_problems пуста/не сущ -> дай знать, что все проблемы уже решены
                    setTitle("Все проблемы решены");
                else
                {//но если есть проблемы, получи о каждой проблеме данные и занеси в linearLayout (ОСНОВНОЙ ЭКШН ЗДЕСЬ)
                    setTitle("Срочные проблемы на линиях"); //AppBar надпись задать
                    for(DataSnapshot urgentProblemSnap : urgentProblemsSnap.getChildren())
                    { //цикл проходит по каждой срочной проблеме, чтобы получить о ней данные и занести их в виде отдельных textviews в linearLayout
                        UrgentProblem urgentProblem = urgentProblemSnap.getValue(UrgentProblem.class); //считываем данные прямо в объект UrgentProblem
                        String whoIsNeededPosition = urgentProblem.getWho_is_needed_position(); //специалист какого профиля нужен (должность: master, quality, raw)
                        if(whoIsNeededPosition.equals(employeePosition) && urgentProblemSnap.child("status").getValue().toString().equals("DETECTED")) {
                            //условия query: срочная проблема нуждается во вмешании специалиста с профилем, соответствующим профилю данного пользователя (который пользуется сейчас приложением)
                            //а также проблемой еще никто из спецов не занимался, она все еще в состоянии DETECTED
                            //----СОЗДАНИЕ TEXTVIEW, ВНЕСЕНИЕ ДАННЫХ В НЕГО И ИНИЦИАЛИЗАЦИЯ ПАРАМЕТРОВ----//
                            TextView problemsInfo;
                            problemsInfo = new TextView(getApplicationContext());
                            //данные об этой проблеме запишем в строку problemInfoFromDB
                            String problemInfoFromDB = "Цех: " + urgentProblem.getShop_name() + "\nОборудование: " + urgentProblem.getEquipment_name() + "\nПункт №" + urgentProblem.getPoint_no()
                                    + "\nДата и время обнаружения: " + urgentProblem.getDate_detected() + " " + urgentProblem.getTime_detected();
                            problemsInfo.setText(problemInfoFromDB);
                            problemsInfo.setPadding(25, 25, 25, 25);
                            problemsInfo.setId(ID_TEXTVIEWS + problemCount);
                            problemsInfo.setTextColor(Color.parseColor(getString(R.color.text)));
                            problemsInfo.setTextSize(13);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(20, 25, 20, 25);
                            problemsInfo.setLayoutParams(params);
                            problemsInfo.setClickable(true);
                            problemsInfo.setBackgroundResource(R.drawable.list_group_layout);
                            problemsInfo.setOnClickListener(textviewClickListener);
                            //----КОНЕЦ ИНИЦИАЛИЗАЦИИ TEXTVIEW ДЛЯ СРОЧНОЙ ПРОБЛЕМЫ----//
                            linearLayout.addView(problemsInfo); //добавить textview в linearLayout
                            problemCount++; //итерировать для уникализации айдишек textviews
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

}
