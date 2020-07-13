package com.akfagroup.radiatorfactoryaps;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

//----------ПОСЛЕ ОТСКАНИРОВАНИЯ РЕМОНТНИКОМ QR ПРОБЛЕМНОГО УЧАСТКА, ЭТОТ АКТИВИТИ  СПРАШИВАЕТ, ---------//
// ---------ХОЧЕТ ЛИ ОН СФОТКАТЬ РЕШЕНИЕ, ЕСЛИ ДА - ЗАПУСК КАМЕРЫ, ЕСЛИ НЕТ - ВОЗВРАТ В REPAIRERS PROBLEMS LIST---------//
public class RepairerTakePhoto extends AppCompatActivity implements View.OnTouchListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1; //код для камера активити
    String problemPushKey;
    Button takePic, dontTakePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repairer_take_photo);
        initInstances();

    }

    @SuppressLint("ClickableViewAccessibility")
    private void initInstances()
    {
        problemPushKey = getIntent().getExtras().getString("ID проблемы в таблице Maintenance_problems");
        takePic = findViewById(R.id.take_pic);
        dontTakePic = findViewById(R.id.dont_take_pic);
        takePic.setOnTouchListener(this);
        dontTakePic.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View button, MotionEvent event) {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                button.setBackgroundResource(R.drawable.edit_red_accent_pressed);
                break;
            case MotionEvent.ACTION_UP:
                //ГЛАВНЫЕ ДЕЙСТВИЯ ЭТОГО АКТИВИТИ:
                //ЕСЛИ НАЖАЛИ TAKE_PIC - ОТКРОЙ КАМЕРУ, ЕСЛИ НАЖАЛИ DONT_TAKE_PIC - ЗАКРОЙ АКТИВИТИ
                button.setBackgroundResource(R.drawable.edit_red_accent);
                switch(button.getId())
                {
                    case R.id.take_pic:
                        dispatchTakePictureIntent(problemPushKey);
                        break;
                    case R.id.dont_take_pic:
                        //проблема решена! аутпут
                        finish();
                        Toast.makeText(getApplicationContext(), "Проблема успешно решена", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
        }
        return false;
    }

    String currentPhotoPath; //the string of uri of where file is located

    private File createImageFile(String problemPushKey) throws IOException {
        // Create an image file name - our case will be the id of problem
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String currentFileName = problemPushKey + "_SOLVED.jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); //директория для пользования только твоим прилдожнием
        File image = new File(storageDir, currentFileName);
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent(String problemPushKey) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(problemPushKey);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getApplicationContext(), "Error creating the file", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //ЗАПУСТИ КАМЕРУ
                Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getString(R.string.package_name), photoFile); //создай файл в памяти телефона
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE  && resultCode == RESULT_OK) {
            finish(); //если получил фотку, закрывай уже это активити
            //загрузка фотки в БД
            Uri file = Uri.fromFile(new File(currentPhotoPath));
            StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
            StorageReference probPicRef = mStorageRef.child("solved_problem_pictures/" + file.getLastPathSegment());
            probPicRef.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(getApplicationContext(), "Фотография загружена успешно", Toast.LENGTH_LONG).show();
                    File picToDelete = new File(currentPhotoPath);
                    picToDelete.delete();
                }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        exception.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Ошибка загрузки файла", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        //progressbar
                    }
                });

        }
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
            super.onBackPressed();
        }
    }
}
