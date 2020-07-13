package com.akfagroup.radiatorfactoryaps;

import java.util.Random;

//-----ФУНКЦИЯ randomString(int) этого класса генерирует случайную строку состояющую из 3 чисел или заглавных англ букв
public class GenerateRandomString {

    public static final String DATA = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static Random RANDOM = new Random();

    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) { //в sb прикрепляем по одной букве, RANDOM - генератор случайности
            sb.append(DATA.charAt(RANDOM.nextInt(DATA.length())));
        }
        return sb.toString();
    }
}
