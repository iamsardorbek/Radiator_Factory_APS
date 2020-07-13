package com.akfagroup.radiatorfactoryaps;

public class StationData {
    private String equipmentName, shopName, qrCode;
    int  stationNo;
    public StationData(int stationNo, String equipmentName, String shopName, String qrCode)
    {
        this.equipmentName = equipmentName;
        this.shopName = shopName;
        this.stationNo = stationNo;
        this.qrCode = qrCode;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getShopName() {
        return shopName;
    }

    public int getStationNo() {
        return stationNo;
    }

    public String getQrCode() {
        return qrCode;
    }
}
