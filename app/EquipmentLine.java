package com.akfa.apsproject;

//---------КЛАСС ОБЪЕКТОВ ИСПОЛЬЗУЕТСЯ В QR SCANNER ДЛЯ УДОБНОЙ РАБОТЫ С ДАННЫМИ ЛИНИЙ ОБОРУДОВАНИЯ----------//
public class EquipmentLine {
    private int shopNo, equipmentNo; //номер цеха и линии
    private String startQRCode; //QR КОД первого участка этой линии оборудования
    public EquipmentLine(int shopNo, int equipmentNo, String startQRCode)
    {
        this.shopNo = shopNo;
        this.equipmentNo = equipmentNo;
        this.startQRCode = startQRCode;
    }

    public int getShopNo() { return shopNo; }
    public int getEquipmentNo() { return equipmentNo; }
    public String getStartQRCode() { return startQRCode; }

}
