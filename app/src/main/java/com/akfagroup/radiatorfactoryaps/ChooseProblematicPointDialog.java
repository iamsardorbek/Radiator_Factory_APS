package com.akfagroup.radiatorfactoryaps;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

//-------- ДИАЛОГ ВЫБОРА И ИНФОРМИРОВАНИЯ О СРОЧНОЙ НЕПОЛАДКЕ/ПРОБЛЕМЕ ОБНАРУЖЕННОЙ ОПЕРАТОРОМ В PULT ACTIVITY --------//
//--------ГЛАВНЫЕ ЭЛЕМЕНТЫ: SPINNER С НОМЕРАМИ УЧАСТКОВ ДЛЯ ВЫБОРА, --------//
public class ChooseProblematicPointDialog extends DialogFragment implements View.OnTouchListener {
    private ChooseProblematicStationDialogListener listener; //для передачи данных PultActivity через и интерфейс

    private int numOfStations;
    private int whoIsNeededIndex; //master/raw/repair/quality
    private String equipmentLineName, shopName;
    private String operatorLogin;
    private List<String> spinnerArray =  new ArrayList<String>();

    private Spinner spinnerStations;
    private Button confirm, cancel;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_choose_problematic_point, container, false); //связать с xml файлом
        initInstances(view);
        spinnerArray.add("Нажмите сюда для выбора участка");

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users/" + operatorLogin);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot userSnap) {
                //по аккаунту юзера (подветки equipment_name, shop_name) найти кол-во участков
                equipmentLineName = userSnap.child("equipment_name").getValue().toString();
                shopName = userSnap.child("shop_name").getValue().toString();
                DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("Shops");
                shopsRef.addListenerForSingleValueEvent(new ValueEventListener() { //единожды пройдись пока не найдешь нужный цех
                    @Override
                    public void onDataChange(@NonNull DataSnapshot shopsSnap) {
                        for(DataSnapshot shopSnap : shopsSnap.getChildren())
                        {
                            if(shopSnap.child("shop_name").getValue().toString().equals(shopName))
                            {
//                                shopName = shopSnap.child("shop_name").getValue().toString();
                                for(DataSnapshot equipmentSnap : shopSnap.child("Equipment_lines").getChildren()) // пройдись пока не найдешь нужную линию
                                {
                                    if(equipmentSnap.child("equipment_name").getValue().toString().equals(equipmentLineName)) //нашел нужную линию
                                    {
                                        numOfStations = Integer.parseInt(equipmentSnap.child(getString(R.string.number_of_points)).getValue().toString()); //искомое кол-во участков
                                        //ниже: заполни спиннер
                                        for(int i = 1; i <=numOfStations; i++) {
                                            spinnerArray.add("Пункт №" + i);
                                        }
                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, spinnerArray);
                                        adapter.setDropDownViewResource(R.layout.spinner_item);
                                        spinnerStations.setAdapter(adapter);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
        return view;
    }

    private void initInstances(View view)
    {
        confirm = view.findViewById(R.id.confirm);
        cancel = view.findViewById(R.id.cancel);
        spinnerStations = view.findViewById(R.id.spinner_points);
        cancel.setOnTouchListener(this);
        confirm.setOnTouchListener(this);

        Bundle bundle = getArguments();
        operatorLogin = bundle.getString("Логин пользователя");
        whoIsNeededIndex = bundle.getInt("Вызвать специалиста");
    }

    public interface ChooseProblematicStationDialogListener { //интерфейс чтобы PultActivity и диалог могли общаться
        void submitPointNo(int pointNo, String equipmentLineName, String shopName, String operatorLogin, int whoIsNeededIndex);
        void onDialogCanceled(int whoIsNeededIndex);
    }

    @Override
    public boolean onTouch(View button, MotionEvent event) {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN: //эффект нажатия
                switch (button.getId())
                {
                    case R.id.cancel:
                        button.setBackgroundResource(R.drawable.red_rectangle_pressed);
                        break;
                    case R.id.confirm:
                        button.setBackgroundResource(R.drawable.green_rectangle_pressed);
                }
                break;
            case MotionEvent.ACTION_UP: //что делать при клике
                switch (button.getId())
                {
                    case R.id.cancel: //закрыть диалог и сообщить об этом PultActivity через интерфейс
                        button.setBackgroundResource(R.drawable.red_rectangle);
                        listener.onDialogCanceled(whoIsNeededIndex);
                        getDialog().dismiss();
                        break;
                    case R.id.confirm: //
                        button.setBackgroundResource(R.drawable.green_rectangle);
                        if(spinnerStations.getSelectedItem().toString().equals("Нажмите сюда для выбора пункта")) //если юзер не открыл spinner и не выбрал пункт
                            Toast.makeText(getView().getContext(), "Выберите пункт", Toast.LENGTH_SHORT).show();
                        else
                        {
                            int pointNo = spinnerStations.getSelectedItemPosition(); //какой пункт выбрали (индекс выбранного элемента спиннера)
                            listener.submitPointNo(pointNo, equipmentLineName, shopName, operatorLogin, whoIsNeededIndex); //передай в интерфейс функцию данные
                            getDialog().dismiss(); // и закрой диалог
                        }
                        break;
                }
                break;
        }
        return false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()){
            @Override public void onBackPressed() {
                listener.onDialogCanceled(whoIsNeededIndex); // при нажатии на кнопку назад дай об ээтом знать PultActivity
                getDialog().dismiss();
            }
        };
    }

    @Override
    public void onAttach(Context context) { //attach listener to this dialog
        super.onAttach(context);
        try { listener = (ChooseProblematicStationDialogListener) context; }
        catch (ClassCastException e) { throw new ClassCastException(context.toString() + "must implement ChooseProblematicStationDialogListener"); }
    }
}
