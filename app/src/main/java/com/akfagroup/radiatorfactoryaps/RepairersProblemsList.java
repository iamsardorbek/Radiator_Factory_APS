package com.akfagroup.radiatorfactoryaps;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

//----------ПОКАЗЫВАЕТ СПИСОК ТО ПРОБЛЕМ НА ЗАВОДЕ-------//
//----------ПРИ НАЖАТИИ НА TextView С ПРОБЛЕМОЙ, ОТКРЫВАЕТ RepairersSeparateProblem---------//
//---------layout xml пустой почти, потому что элементы динамически добавляются, заголовок задается программно---------//
public class RepairersProblemsList extends AppCompatActivity {
    private final int ID_TEXTVIEWS = 5000;
    private int problemCount = 0;
    private String employeeLogin, employeePosition;
    private List<String> problemIDs;
    ActionBarDrawerToggle toggle;
    LinearLayout linearLayout;
    View.OnClickListener textviewClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.repairers_activity_problems_list);
        setTitle("Загрузка данных..."); //если нет проблем, надо сделать: нету проблем
        initInstances();
        toggle = InitNavigationBar.setUpNavBar(RepairersProblemsList.this, getApplicationContext(),  getSupportActionBar(), employeeLogin, employeePosition, R.id.problems_list, R.id.repairers_activity);
        addProblemsFromDatabase();
    }
    private void initInstances()
    {//иниц кросс-активити перем-х
        employeeLogin = getIntent().getExtras().getString("Логин пользователя");
        employeePosition = getIntent().getExtras().getString("Должность");
        linearLayout = findViewById(R.id.linearLayout);
        problemIDs = new ArrayList<>();
        //к каждому textview проблемы будет прикреплен этот listener
        textviewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nomerProblemy = v.getId() - ID_TEXTVIEWS;
                Intent intent = new Intent(getApplicationContext(), RepairerSeparateProblem.class);
                String IDOfSelectedProblem = problemIDs.get(nomerProblemy);
                intent.putExtra("ID проблемы в таблице Maintenance_problems", IDOfSelectedProblem);
                intent.putExtra("Логин пользователя", employeeLogin);
                intent.putExtra("Должность", employeePosition);
                startActivity(intent);
            }
        };
    }

    private void addProblemsFromDatabase() {
        //----САМОЕ ГЛАВНОЕ ЭТОГО АКТИВИТИ----//
        //на самом деле нужно взять количество строк в таблице problems
        DatabaseReference problemsRef = FirebaseDatabase.getInstance().getReference().child("Maintenance_problems"); //ссылка на проблемы ТО
        problemsRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceType")
            @Override public void onDataChange(@NonNull DataSnapshot problemsSnap) {
                linearLayout.removeAllViews(); //для обновления данных удали все результаты предыдущего поиска
                if(problemsSnap.getValue() == null)
                {
                    setTitle("Все проблемы решены");
                }
                else
                {
                    for(DataSnapshot problemDataSnapshot : problemsSnap.getChildren())
                    { //пройдись по всем проблемах в ветке
                        MaintenanceProblem problem = problemDataSnapshot.getValue(MaintenanceProblem.class); //считай в объект
                        if(!problem.solved)
                        {
                            setTitle("Проблемы на линиях");
                            problemIDs.add(problemDataSnapshot.getKey()); //добавь айди данной проблемы в лист

                            //инициализация TEXTVIEW
                            String problemInfoFromDB = "Цех: " + problem.getShop_name() + "\nОборудование: " + problem.getEquipment_line_name() + "\nПункт №" + problem.getPoint_no() + "\nПункт №" + problem.getSubpoint_no();
                            TextView problemsInfo;
                            problemsInfo = new TextView(getApplicationContext());
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
                            //добавить textview в layout
                            linearLayout.addView(problemsInfo);
                            problemCount++; //итерируй для уникализации айдишек textview и обращения к лист элементам
                        }
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(toggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }

}
