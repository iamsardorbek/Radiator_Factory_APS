package com.akfa.apsproject;

import android.content.Context;
import android.os.Vibrator;

public class Vibration {
    private static final int VIBRATION_DURATION = 500;
    public static void vibration(Context context)
    {
        //вибрирует
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATION_DURATION);

    }
}
