package com.akfagroup.radiatorfactoryaps;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MakeACall extends AppCompatActivity implements View.OnTouchListener {
    private Button callMaster, callOperator, callRepairer;
    private String employeePosition, employeeLogin, shopName, equipmentName, whoIsCalled;
    private int pointNo;
    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_make_a_call);
        initInstances();
    }

    private void initInstances() {
        setTitle("Вызвать сотрудника");
        Bundle args = getIntent().getExtras();
        employeeLogin = args.getString("Логин пользователя");
        employeePosition = args.getString("Должность");
        whoIsCalled = args.getString("Кого вызываем");
        callOperator = findViewById(R.id.call_operator);
        callRepairer = findViewById(R.id.call_repairer);
        callMaster = findViewById(R.id.call_master);
        if(employeeLogin.equals("master"))
        {
            callMaster.setVisibility(View.INVISIBLE);
        }
        else if(employeeLogin.equals("quality") || employeeLogin.equals("raw"))
        {
            callRepairer.setVisibility(View.INVISIBLE);
        }
        callOperator.setOnTouchListener(this);
        callRepairer.setOnTouchListener(this);
        callMaster.setOnTouchListener(this);
        if(whoIsCalled != null)
        {
            shopName = args.getString("Название цеха");
            equipmentName = args.getString("Название линии");
            pointNo = args.getInt(getString(R.string.nomer_punkta_textview_text));
            DialogFragment dialogFragment = new ConfirmCallDialog();
            Bundle bundle = new Bundle();
            bundle.putString("Название цеха", shopName);
            bundle.putString("Название линии", equipmentName);
            bundle.putInt(getString(R.string.nomer_punkta_textview_text), pointNo);
            bundle.putString("Логин пользователя", employeeLogin);
            bundle.putString("Вызываемый специалист", whoIsCalled);
            dialogFragment.setArguments(bundle);
            dialogFragment.show(getSupportFragmentManager(), "Подтвердить вызов");
        }

        DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");
        callsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot callsSnap) {
                for(DataSnapshot callSnap : callsSnap.getChildren())
                {
                    Call call = callSnap.getValue(Call.class);
                    String callCalledBy = call.getCalled_by();
                    boolean callComplete = call.getComplete();
                    if(!callComplete && callCalledBy.equals(employeeLogin))
                    {
                        String callWhoIsNeededPosition = call.getWho_is_needed_position();
                        switch (callWhoIsNeededPosition)
                        {
                            case "operator":
                                callOperator.setBackgroundResource(R.drawable.call_opened_button);
                                callOperator.setClickable(false);
                                callOperator.setText("Оператор вызван");
                                break;
                            case "repair":
                                callRepairer.setBackgroundResource(R.drawable.call_opened_button);
                                callRepairer.setClickable(false);
                                callRepairer.setText("Ремонтник вызван");
                                break;
                            case "master":
                                callMaster.setBackgroundResource(R.drawable.call_opened_button);
                                callMaster.setClickable(false);
                                callMaster.setText("Мастер вызван");
                                break;
                        }
                        DatabaseReference activeCallRef = FirebaseDatabase.getInstance().getReference("Calls/" + callSnap.getKey());
                        activeCallRef.addValueEventListener(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot activeCallSnap) {
                                Call call = activeCallSnap.getValue(Call.class);
                                boolean callComplete = call.getComplete();
                                if(callComplete)
                                {
                                    String callWhoIsNeededPosition = call.getWho_is_needed_position();
                                    switch (callWhoIsNeededPosition)
                                    {
                                        case "operator":
                                            callOperator.setBackgroundResource(R.drawable.call_closed_button);
                                            callOperator.setClickable(true);
                                            callOperator.setText("Оператор прибыл");
                                            break;
                                        case "repair":
                                            callRepairer.setBackgroundResource(R.drawable.call_closed_button);
                                            callRepairer.setClickable(true);
                                            callRepairer.setText("Ремонтник прибыл");
                                            break;
                                        case "master":
                                            callMaster.setBackgroundResource(R.drawable.call_closed_button);
                                            callMaster.setClickable(true);
                                            callMaster.setText("Мастер прибыл");
                                            break;
                                    }
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        toggle = InitNavigationBar.setUpNavBar(MakeACall.this, getApplicationContext(),  getSupportActionBar(), employeeLogin, employeePosition, R.id.make_a_call, R.id.activity_make_a_call);
    }

    @Override
    public boolean onTouch(View button, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                String whoIsCalled = "";
                switch (button.getId())
                {
                    case R.id.call_master:
                        whoIsCalled = "master";
                        break;
                    case R.id.call_operator:
                        whoIsCalled = "operator";

                        break;
                    case R.id.call_repairer:
                        whoIsCalled = "repair";
                        break;
                }
                Intent openQR = new Intent(getApplicationContext(), QRScanner.class);
                openQR.putExtra("Действие", "определи адрес"); //описание действия для QR сканера
                openQR.putExtra("Должность", employeePosition);
                openQR.putExtra("Кого вызываем", whoIsCalled);
                openQR.putExtra("Логин пользователя", employeeLogin); //передавать логин пользователя взятый из Firebase
                startActivity(openQR);
                finish();
                break;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

}
