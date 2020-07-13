package com.akfa.apsproject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;


//------ПОКАЗЫВАЕТ QR КОД НА ПУЛЬТЕ ОПЕРАТОРА, ЧТОБЫ ПОДОШЕДШИЙ СПЕЦИАЛИСТ ОТСКАНИРОВАЛ (ДОКАЗАТЕЛЬСТВО ЧТО ОН ПРИШЕЛ НА САМОМ ДЕЛЕ / REALITY CHECK) ------//
//------ТАКЖЕ ЧЕРЕЗ 15 СЕКУНД ПОСЛЕ СВОЕГО ЗАПУСКА, ЗАПУСКАЕТ RUNNABLE, ОБНОВЛЯЮЩИЙ КАЖДЫЕ 15 СЕК QR ТЕКУЩЕЙ СРОЧНОЙ ПРОБЛЕМЫ (ЧТОБЫ ИЗБЕЖАТЬ ТОГО, ЧТО ОПЕРАТОР ОТПРАВЛЯЕТ ЧЕРЕЗ ТГ СКРИН QR И МАСТЕР НЕ ПРИХОДИТ)------//
public class QRCodeDialog extends DialogFragment {
    private QRCodeDialogListener listener; //обрабатывает и передает данные в pultActivity с помощью интерфейса

    private static final long QR_REFRESH_TIME = 15000; //in millis
    private int whoIsNeededIndex; //
    private boolean runnableStarted = false; //чтобы в нужное время останавливать runnable обновляющий QR Код

    //относ к runnable обновления QR кода
    private Handler handler = new Handler();
    private Runnable runnableCode;
    private boolean stopped = false;

    View view; //view всего диалога
    ImageView qrCode;
    Button back;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_qr_code, container, false);

        qrCode = view.findViewById(R.id.qr_code);
        back = view.findViewById(R.id.back);
        Bundle arguments = getArguments();
        final String shopName, equipmentName, operatorLogin;
        shopName = arguments.getString("Название цеха");
        equipmentName = arguments.getString("Название линии");
        operatorLogin = arguments.getString("Логин пользователя");
//        String qrCodeString = arguments.getString("Код"); //encode in the QR code
        whoIsNeededIndex = arguments.getInt("Вызвать специалиста");

        final DatabaseReference urgentProblemsRef = FirebaseDatabase.getInstance().getReference("Urgent_problems");
        //этот слушатель БД единожды находит рассматриваемую оператором срочную проблему, находить в ней пару "qr_random_code : value"
        //и устанавливает многоразовый слушатель именно на эту пару (для каждого обновления QR кода)
        urgentProblemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot urgentProblemsSnap) {
                for(final DataSnapshot urgentProblemSnap : urgentProblemsSnap.getChildren()) //пройди по срочным проблемам, пока не найдешь нужную нам сейчас
                {
                    String shopNameDB = urgentProblemSnap.child("shop_name").getValue().toString();
                    String equipmentNameDB = urgentProblemSnap.child("equipment_name").getValue().toString();
                    String operatorLoginDB = urgentProblemSnap.child("operator_login").getValue().toString();
                    String status = urgentProblemSnap.child("status").getValue().toString();
                    if(shopName.equals(shopNameDB) && equipmentName.equals(equipmentNameDB) && operatorLogin.equals(operatorLoginDB) && status.equals("DETECTED"))
                    { //проблема с теми же данными, и к которой еще специалист не пришел
                        DatabaseReference urgentProblemQrRandomCodeRef = urgentProblemsRef.child(urgentProblemSnap.getKey()).child("qr_random_code");
                        if(!runnableStarted)
                        { //если runnable не запустил еще, запусти
                            initializeRunnable(urgentProblemSnap.getKey());
                            stopped = false;
                            handler.postDelayed(runnableCode, QR_REFRESH_TIME); //runnable запустится через QR_REFRESH_TIME мс
                        }
                        urgentProblemQrRandomCodeRef.addValueEventListener(new ValueEventListener() { //прислушивайся к изменениям в БД
                            @Override public void onDataChange(@NonNull DataSnapshot qrRandomCodeSnap) {
                                String qrCodeString = urgentProblemSnap.child("qr_random_code").getValue().toString();
                                generateQrBitmap(qrCodeString); //к сожалению, контент image view с QR не обновляется
                            }
                            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });
                        return;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        back.setOnTouchListener(backButtonListener); //установи listener кликов кнопки
        return view;
    }

    public interface QRCodeDialogListener { //иниц интерфейса
        void onQRCodeDialogCanceled(int whoIsNeededIndex);
    }

    // Define the code block to be executed

    private void initializeRunnable(final String urgentProblemKey)
    {
        runnableCode = new Runnable() {
            @Override
            public void run() {
                if(!stopped) {
                    DatabaseReference urgentProblemQRCodeRef = FirebaseDatabase.getInstance().getReference("Urgent_problems/" + urgentProblemKey + "/qr_random_code");
                    String randomlyGeneratedCode = GenerateRandomString.randomString(3);
                    urgentProblemQRCodeRef.setValue(randomlyGeneratedCode);
                    // Repeat this the same runnable code block again another 2 seconds
                    // 'this' is referencing the Runnable object
                    handler.postDelayed(this, QR_REFRESH_TIME);
                }
            }
        };
    }

    View.OnTouchListener backButtonListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //что делать, если юзер нажал кнопку назад
            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN: //эффект зажатия
                    back.setBackgroundResource(R.drawable.red_rectangle_pressed);
                    break;
                case MotionEvent.ACTION_UP: //действие при клике
                    back.setBackgroundResource(R.drawable.red_rectangle);
                    stopped = true; //чтобы runnable остановился
                    listener.onQRCodeDialogCanceled(whoIsNeededIndex); //отправим через интерфейс диалога в PultActivity сообщение, что нажали "назад"
                    getDialog().dismiss(); //закрыть диалог
                    break;
            }
            return false;
        }
    };

    private void generateQrBitmap(String qrCodeString)
    {
        //из строки qrCodeString сгенерировать QR Код
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qrCodeString, BarcodeFormat.QR_CODE,300,300);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCode.setImageBitmap(bitmap);
        }
        catch (WriterException e) { e.printStackTrace(); }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()){
            @Override public void onBackPressed() {
                //при наж кнопки назад информируй PultActivity через интерфейс
                listener.onQRCodeDialogCanceled(whoIsNeededIndex);
                getDialog().dismiss();
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try { //прикрепи интерфейс к диалогу
            listener = (QRCodeDialog.QRCodeDialogListener) context;
        } catch (ClassCastException e) { throw new ClassCastException(context.toString() + "must implement QRCodeDialogListener"); }
    }
}
