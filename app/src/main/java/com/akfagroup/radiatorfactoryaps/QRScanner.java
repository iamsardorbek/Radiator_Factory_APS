package com.akfagroup.radiatorfactoryaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//--------------ОБЕСПЕЧИВАЕТ ТО, ЧТОБЫ СОТРУДНИКИ ШЛИ НА МЕСТА ВОЗНИКНОВЕНИЯ ПРОБЛЕМ, А НЕ ОТМЕЧАЛИ ПРОБЛЕМЫ РЕШЕННЫМИ/ПРОВЕРКИ ОКОНЧЕННЫМИ ПРОСТО С МЕСТА------------//
//--------------QR SCANNER ПОЗВОЛЯЕТ ДЕЛАТЬ СВОЕГО РОДА REALITY CHECK-------------//
//--------------ЕГО ОТКРЫВАЕТ ОПЕРАТОР ПРИ ТО ПРОВЕРКЕ, РЕМОНТНИК ПРИ РЕШЕНИИ ТО ПРОБЛЕМЫ, СПЕЦИАЛИСТ ПРИ РЕШЕНИИ СРОЧНОЙ ПРОБЛЕМЫ---------------//
public class QRScanner extends AppCompatActivity implements SurfaceHolder.Callback{
    SurfaceView surfaceView;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;
    TextView textView, directionsTextView;
    private int equipmentNumber, shopNumber, nomerPunkta, numOfPoints, problemsCount; //кросс-активити переменные QuestPointDynamic
    private long startTimeMillis; //кросс-активити переменная, передаваемая из QuestPointDynamic
    private boolean detectedOnce = false, flashlightOn = false; //для того, чтобы на уже обнаруженный (расшифрованный) код не реагировал повторно
    private String mCameraId, codeToDetect, intendedAction, shopName, equipmentName, callFirebaseKey, whoIsCalled;
    private String employeeLogin, employeePosition;
    List<EquipmentLine> equipmentLineList = new ArrayList<>();
    List<String> problemPushKeysOfTheWholeCheck;
    List<PointData> pointsData;
    Bundle arguments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quest_activity_q_r_scanner);
        getSupportActionBar().hide(); //спрятать нав бар
        initInstances(); //иниц все переменные и объекты
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build(); //детектор QR кодов инициализировать
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(640, 480).build(); //иниц источник камеры
        requestCameraPermission(); //в случае разрешения, стартует QR сканер
    }

    private void initInstances()
    {
        arguments = getIntent().getExtras();
        intendedAction = arguments.getString("Действие");
        equipmentNumber = arguments.getInt("Номер линии");
        equipmentName = arguments.getString("Название линии");
        shopNumber = arguments.getInt("Номер цеха");
        shopName = arguments.getString("Название цеха");
        nomerPunkta = arguments.getInt(getString(R.string.nomer_punkta_textview_text));
        numOfPoints = arguments.getInt("Количество пунктов");
        startTimeMillis = arguments.getLong("startTimeMillis");
        employeeLogin = arguments.getString("Логин пользователя");
        employeePosition = arguments.getString("Должность");
        problemsCount = arguments.getInt("Количество обнаруженных проблем");
        problemPushKeysOfTheWholeCheck = arguments.getStringArrayList("Коды проблем");
        callFirebaseKey = arguments.getString("Код вызова");
        whoIsCalled = arguments.getString("Кого вызываем");

        //UI объекты
        surfaceView = findViewById(R.id.camerapreview);
        textView = findViewById(R.id.textView);
        directionsTextView = findViewById(R.id.directionsTextView);
        directionsTextView.setVisibility(View.INVISIBLE); //пока данные с БД не получены, спрятай directionsTextView

        //codeToDetect, возьми данные из таблицы "QRCodes"
        if(intendedAction.equals("Открой PointDynamic") || intendedAction.equals("ремонтник")) //это оператор заходит в QRScanner в течении ТО проверки или ремонтник хочет решить проблему
        {
            DatabaseReference shopRef = FirebaseDatabase.getInstance().getReference().child("Shops/" + shopNumber); //ссылка к цеху
            shopRef.addListenerForSingleValueEvent(new ValueEventListener() {//единожды считать данные про линию текущего юзера
                @Override public void onDataChange(@NonNull DataSnapshot shop) {
                    String shopName = shop.child("shop_name").getValue().toString();
                    String equipmentName = shop.child("Equipment_lines/" + equipmentNumber + "/equipment_name").getValue().toString();
                    Log.e("qrscanner", "equipmentNum = " + equipmentNumber + "\tnomerPunkta = " + nomerPunkta);
                    String pointName = shop.child("Equipment_lines/" + equipmentNumber + "/Points/" + nomerPunkta + "/point_name").getValue().toString();
                    String directionsText = "Подойдите к\n" + shopName + "\n" + equipmentName + "\nПункт №" + nomerPunkta + "\n" + pointName;
                    directionsTextView.setText(directionsText);
                    codeToDetect = shop.child("Equipment_lines/" + equipmentNumber + "/QR_codes/qr_" + nomerPunkta).getValue().toString();
                    directionsTextView.setVisibility(View.VISIBLE); //данные получены и directions составлены, сделаем текствью видимым
                }
                @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
        else if(intendedAction.equals("Любой код"))
        { //пройтись по всем линиям, и забей данные о них в equipmentLineList для мастера/оператора которые сразу начинают проверку  прямой вход в QR из QuestMainActivity ("Любой код")
            DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference().child("Shops");
            shopsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot shops) {
                    for(DataSnapshot shop : shops.getChildren())
                    {
                        DataSnapshot shopEquipmentLines = shop.child("Equipment_lines");
                        for(DataSnapshot equipmentLine : shopEquipmentLines.getChildren()) {
                            String codeToDetect = equipmentLine.child("QR_codes/qr_1").getValue().toString();
                            int shopNo = Integer.parseInt(shop.getKey());
                            int equipmentLineNo = Integer.parseInt(equipmentLine.getKey().toString());
                            EquipmentLine equipmentLineObject = new EquipmentLine(shopNo, equipmentLineNo, codeToDetect);
                            equipmentLineList.add(equipmentLineObject);
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
        else if(intendedAction.equals("реагирование на вызов"))
        {

            DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("Shops");
            shopsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot shopsSnap) {
                    for(DataSnapshot singleShopSnap : shopsSnap.getChildren())
                    {
                        String shopNameDB = singleShopSnap.child("shop_name").getValue().toString();
                        if(shopNameDB.equals(shopName))
                        {
                            for(DataSnapshot singleEquipmentSnap : singleShopSnap.child("Equipment_lines").getChildren())
                            {
                                String equipmentNameDB = singleEquipmentSnap.child("equipment_name").getValue().toString();
                                if(equipmentNameDB.equals(equipmentName))
                                {
                                    String pointName = singleEquipmentSnap.child("Points/" + nomerPunkta + "/point_name").getValue().toString();
                                    String directionsText = "Подойдите к\n" + shopName + "\n" + equipmentName + "\nПункт №" + nomerPunkta + "\n" + pointName;
                                    directionsTextView.setText(directionsText);
                                    directionsTextView.setVisibility(View.VISIBLE);
                                    codeToDetect = singleEquipmentSnap.child("QR_codes/qr_" + nomerPunkta).getValue().toString();
                                    return;
                                }
                            }
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

        }
        else if(intendedAction.equals("определи адрес"))
        {
            directionsTextView.setText("Отсканируйте QR метку ближайшего участка, место определится автоматически");
            directionsTextView.setVisibility(View.VISIBLE);
            pointsData = new ArrayList<>();
            DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("Shops");
            shopsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot shopsSnap) {
                    for(DataSnapshot singleShopSnap : shopsSnap.getChildren())
                    {
                        String shopNameDB = singleShopSnap.child("shop_name").getValue().toString();

                        for (DataSnapshot singleEquipmentSnap : singleShopSnap.child("Equipment_lines").getChildren())
                        {
                            String equipmentNameDB = singleEquipmentSnap.child("equipment_name").getValue().toString();
                            int numOfStations = Integer.parseInt(singleEquipmentSnap.child(getString(R.string.number_of_points)).getValue().toString());
                            for(int i = 1; i <= numOfStations; i++)
                            {
                                String thisStationQRCode = singleEquipmentSnap.child("QR_codes/qr_" + i).getValue().toString();
                                pointsData.add(new PointData(i, equipmentNameDB, shopNameDB, thisStationQRCode));
                            }
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
    }

    private void requestCameraPermission() { //если разрешение на камеру все еще не дали, спросить снова. Спрашивается это также в Логин активити
        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionStatus == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 0);
        else
            qrScanCameraON(); //стартует cameraSource и QRScanner сам
    }

    private void qrScanCameraON()  //стартует cameraSource и QRScanner сам
    {
        surfaceView.getHolder().addCallback(this);

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override public void release() { }

            //------------САМОЕ ГЛАВНОЕ ЭТОГО АКТИВИТИ ЗДЕСЬ------------//
            //-------обработка обнаруженных кодов, сравнивание их с тем, что нам нужно, дальнейшие действия--------//
            @Override public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if(qrCodes.size()!=0) //если обнаружен хоть 1 код
                {
                    //именно textView.post используется, потому что он дает доступ изменять textView в других классах (а это inner class Detector.Processor<Barcode>)
                    //поэтому все умещаем внутри этого post и runnable
                    textView.post(new Runnable() {
                        @Override public void run() {
                            {
                                final String codeFromQR = qrCodes.valueAt(0).displayValue;
                                if(intendedAction.equals("Открой PointDynamic") || intendedAction.equals("ремонтник")) {
                                    if (!detectedOnce) {
                                        if (areDetectedAndPassedCodesSame(codeFromQR, codeToDetect)) {
                                            Vibration.vibration(getApplicationContext());
                                            //создаем интент, в него заносим код успешного распознования
                                            //открываем соответствующий активити (Point Dynamic / RepairerTakePhoto)
                                            if (intendedAction.equals("Открой PointDynamic")) { //когда идет переход Verification->QR->PointDynamic
                                                Intent intent = new Intent(getApplicationContext(), QuestPointDynamic.class);
                                                //кросс-активити данные PointDynamica
                                                intent.putExtra(getString(R.string.nomer_punkta_textview_text), nomerPunkta);
                                                intent.putExtra("Количество пунктов", numOfPoints);
                                                intent.putExtra("startTimeMillis", startTimeMillis);
                                                intent.putExtra("Логин пользователя", employeeLogin);
                                                intent.putExtra("Номер цеха", shopNumber);
                                                intent.putExtra("Номер линии", equipmentNumber);
                                                intent.putExtra("Количество обнаруженных проблем", problemsCount);
                                                intent.putExtra("Должность", employeePosition);
                                                intent.putStringArrayListExtra("Коды проблем", (ArrayList<String>) problemPushKeysOfTheWholeCheck);
                                                startActivity(intent);
                                            } else if (intendedAction.equals("ремонтник")) { //ремонтник нажал кнопку "Решить проблему" и подошел к соответствующему проблемному участку, и решил ее, потом отсканировал код этого участка
                                                String problemKey = arguments.getString("ID проблемы в таблице Maintenance_problems");
                                                DatabaseReference problemRef = FirebaseDatabase.getInstance().getReference().child("Maintenance_problems/" + problemKey);
                                                problemRef.child("solved").setValue(true); //перевести в статус РЕШЕННАЯ
                                                problemRef.child("solved_by").setValue(employeeLogin); //логин ремонтника для отчета и мониторинга ответственности
                                                //--получ данные о текущей дате и времени--//
                                                String date, time;
                                                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                                                date = sdf.format(new Date());
                                                sdf = new SimpleDateFormat("HH:mm");
                                                time = sdf.format(new Date());
                                                //--конец получ данных о дате-времени--//
                                                problemRef.child("date_solved").setValue(date); //записать эти данные в БД
                                                problemRef.child("time_solved").setValue(time);
                                                problemRef.child("solved_by").setValue(employeeLogin);
                                                Intent openRepairerTakePhoto = new Intent(getApplicationContext(), RepairerTakePhoto.class);
                                                openRepairerTakePhoto.putExtra("ID проблемы в таблице Maintenance_problems", problemKey);
                                                startActivity(openRepairerTakePhoto);
                                            }
                                            finish();
                                        }
                                        else {//если отсканировал несоответствующий код
                                            textView.setText("Вы не в том месте");
                                        }
                                    }
                                }
                                else if (intendedAction.equals("Любой код")) { //оператор/мастер сразу хочет начать проверку войдя в сканер из QUESTMAINACTIVTY
                                    if (!detectedOnce) { //чтобы не реагировал повторно
                                        EquipmentLine equipmentLine = detectedCodeAmongInitialPunkts(codeFromQR); //своеобразно проверяет, есть ли среди equipmentLineList линия с кодом 1-го участка, соответствующий тому, что только что отсканировали
                                        //если да, возвращается соответ объект equipmentLine, если нет - null
                                        if(equipmentLine != null) //если такой код сущ и вернули объект
                                        {
                                            Vibration.vibration(getApplicationContext());
                                            Intent intent = new Intent(getApplicationContext(), QuestPointDynamic.class); //начнет ТО проверку с 1-го участка соответ линии
                                            intent.putExtra(getString(R.string.nomer_punkta_textview_text), 1);
                                            intent.putExtra("Номер цеха", equipmentLine.getShopNo());
                                            intent.putExtra("Номер линии", equipmentLine.getEquipmentNo());
                                            intent.putExtra("Логин пользователя", employeeLogin);
                                            intent.putExtra("Количество обнаруженных проблем", problemsCount);
                                            intent.putExtra("Должность", employeePosition);
                                            intent.putStringArrayListExtra("Коды проблем", (ArrayList<String>) problemPushKeysOfTheWholeCheck);
                                            startActivity(intent);
                                            finish();
                                        }
                                        else
                                        {
                                            textView.setText("Подойдите к 1-участку линии, которую вы хотите проверить"); //если попутали что-то
                                        }
                                    }
                                }
                                else if(intendedAction.equals("срочная проблема")) //если специалист сканирует код с экрана пульта оператора
                                {
                                    if (!detectedOnce) {
                                        //go through the qr codes of urgent probs
                                        //create a dbref for that, take all qrs and compare. In case you find similarity, delete that node
                                        final DatabaseReference urgentProbsRef = FirebaseDatabase.getInstance().getReference("Urgent_problems");
                                        urgentProbsRef.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot urgentProbsSnap) {
                                                for(DataSnapshot singleUrgentProbSnap : urgentProbsSnap.getChildren())
                                                {
                                                    String codeToDetect = singleUrgentProbSnap.child("qr_random_code").getValue().toString(); //код итерируемой сроч проблемы в БД
                                                    if(codeFromQR.equals(codeToDetect) && singleUrgentProbSnap.child("status").getValue().toString().equals("DETECTED")) //если то что отсканировал соответствует коду сроч проблемы в БД и специалист не приходил на тот пункт
                                                    { //отметим, что специалист пришел, что переведет кнопку в состояние SPECIALIST_CAME и она перестанет мигать, а начнет ярко гореть
                                                        String urgentProblemKey = singleUrgentProbSnap.getKey();
                                                        //дата-время
                                                        final String dateSpecialistCame;
                                                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                                                        dateSpecialistCame = sdf.format(new Date());
                                                        final String timeSpecialistCame;
                                                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
                                                        timeSpecialistCame = sdf1.format(new Date());
                                                        //конец дата-время
                                                        //занеси данные в БД, измени статус проблемы, что повлечет изм-е статуса кнопки
                                                        urgentProbsRef.child(urgentProblemKey).child("date_specialist_came").setValue(dateSpecialistCame);
                                                        urgentProbsRef.child(urgentProblemKey).child("time_specialist_came").setValue(timeSpecialistCame);
                                                        urgentProbsRef.child(urgentProblemKey + "/status").setValue("SPECIALIST_CAME");
                                                        detectedOnce = true; //чтобы повторно не реагировало на коды
                                                        Toast.makeText(getApplicationContext(), "Специалист на месте", Toast.LENGTH_SHORT).show();
                                                        finish(); //вернись в предыдущ активити (UrgentProbList)
                                                        return;
                                                    }
                                                }
                                            }
                                            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                                        });

                                        if(!detectedOnce) //если все еще не отсканировал нужный код, дай ему подсказку
                                        {
                                            textView.setText("Отсканируйте код, сгенерированный на устройстве оператора, который сообщил о данной проблеме. " +
                                                    "Возможно, QR Код на экране оператора устарел. Нужно закрыть диалог и заново нажать на мигающую кнопку.");
                                        }
                                    }
                                }
                                else if(intendedAction.equals("реагирование на вызов"))
                                {
                                    if (!detectedOnce) {
                                        //retrieve the appropriate qr code value of point from DB, compare it
                                        if(codeFromQR.equals(codeToDetect))
                                        {
                                            detectedOnce = true; //чтобы повторно не реагировало на коды
                                            Vibration.vibration(getApplicationContext());
                                            //дата-время
                                            final String dateCame;
                                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                                            dateCame = sdf.format(new Date());
                                            final String timeCame;
                                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
                                            timeCame = sdf1.format(new Date());
                                            //----считал дату и время----//
                                            DatabaseReference callRef = FirebaseDatabase.getInstance().getReference("Calls/" + callFirebaseKey);

                                            callRef.child("date_came").setValue(dateCame);
                                            callRef.child("time_came").setValue(timeCame);
                                            callRef.child("who_came_login").setValue(employeeLogin);
                                            callRef.child("complete").setValue(true);
                                            finish();
                                        }
                                    }
                                }
                                else if(intendedAction.equals("определи адрес"))
                                {
                                    if (!detectedOnce) {
                                        for (PointData singlePointData : pointsData) {
                                            if (codeFromQR.equals(singlePointData.getQrCode())) {
                                                detectedOnce = true; //чтобы повторно не реагировало на коды
                                                Intent openMakeACall = new Intent(getApplicationContext(), MakeACall.class);
                                                openMakeACall.putExtra("Логин пользователя", employeeLogin);
                                                openMakeACall.putExtra("Должность", employeePosition);
                                                openMakeACall.putExtra("Название цеха", singlePointData.getShopName());
                                                openMakeACall.putExtra("Название линии", singlePointData.getEquipmentName());
                                                openMakeACall.putExtra(getString(R.string.nomer_punkta_textview_text), singlePointData.getPointNo());
                                                openMakeACall.putExtra("Кого вызываем", whoIsCalled);
                                                startActivity(openMakeACall);
                                                finish();
                                            }
                                        }
                                    }
                                    textView.setText("Пункт с такой QR-меткой не существует в базе");
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(nomerPunkta > 1 && (intendedAction.equals("Открой PointDynamic")))
        {//если оператор уже проверяет минимум 2 пункт, и хочет выйти из проверки, все данные об этой ТО проверке стираются из БД (Maintenance_problems->subnode + Storage->problem_pictures->picture
            AlertDialog diaBox = AskOption(); //конструирует объект диалога, в котором при нажатии на да стираются данные ТО проверки
            diaBox.show();
        }
        else if(intendedAction.equals("реагирование на вызов"))
        { //в этом случае CallsList закрывается во избежание дублирования, поэтому нужно снова открыть его
            Intent openCallsList = new Intent(getApplicationContext(), CallsList.class);
            openCallsList.putExtra("Логин пользователя", employeeLogin);
            openCallsList.putExtra("Должность", employeePosition);
            startActivity(openCallsList);
        }
        else //это не оператор/мастер уже начавшие проверку, данные не собраны, можно сразу закрывать сканер
        { //или если это конечный активити в стэке, ну не дай Бог
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

    private AlertDialog AskOption() //конструирует диалог
    {
        AlertDialog myQuittingDialogBox = new AlertDialog.Builder(this).setTitle("Закончить проверку").setMessage("Вы уверены, что хотите закончить проверку? Данные не будут сохранены.")
                .setIcon(R.drawable.close)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DatabaseReference problemsRef = FirebaseDatabase.getInstance().getReference("Maintenance_problems");
                        for(String problemPushKey : problemPushKeysOfTheWholeCheck)
                        { //удалим даннные и фотки проблем, о которых сообщил юзер в течение текущ ТО проверки
                            problemsRef.child(problemPushKey).setValue(null);
                            StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                            StorageReference problemPicRef = mStorageRef.child("problem_pictures/" + problemPushKey + ".jpg");
                            problemPicRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //file deleted successfully
                                }
                            });
                        }
                        finish();
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); //просто закр диалог
                    }
                })
                .create();
        return myQuittingDialogBox; //возврати сконструирнованный объект диалога
    }

    private EquipmentLine detectedCodeAmongInitialPunkts(String codeFromQR) {//своеобразно проверяет, есть ли среди equipmentLineList линия с кодом 1-го участка, соответствующий тому, что только что отсканировали
        //если да, возвращается соответ объект equipmentLine, если нет - null
        for(EquipmentLine equipmentCurrent : equipmentLineList)
            if(codeFromQR.equals(equipmentCurrent.getStartQRCode()))
            {
                detectedOnce = true;
                return equipmentCurrent;
            }
        return null;
    }

    private boolean areDetectedAndPassedCodesSame(String fromQRScanner, String fromDatabase)
    {//функция сравнивает две строчные переменные (чтобы сделать код читабельнее)
        if(fromQRScanner.equals(fromDatabase))
        {
            detectedOnce = true;
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        surfaceView.getHolder().addCallback(this);
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(640, 480).build(); //иниц источник камеры
        super.onResume();
    }


    @Override public void surfaceCreated(SurfaceHolder holder) {
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {//если юзер не дал разрешение на использование камеры, дай ему знать об этом и закрой QR Scanner
            Toast.makeText(getApplicationContext(), "У приложения нет разрешения на использование камеры. Вы можете его предоставить в настройках или перезапустив приложение. ", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try { cameraSource.start(holder); }
        catch (IOException e) { e.printStackTrace(); }
    }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
