package com.akfagroup.radiatorfactoryaps;

import java.util.TreeMap;

//------- ИСПОЛЬЗУЕТСЯ В QUEST MAIN ACTIVITY ЧТОБЫ УДОБНО ЗАПИСЫВАТЬ ДАННЫЕ ИЗ FIREBASE О ЦЕХЕ В EXPANDABLE LIST VIEW-------//
public class Shop {
    public String name;
    public TreeMap<Integer, String> equipmentLines = new TreeMap<>();
    public Shop(String name)
    {
        this.name = name;
    }
}
