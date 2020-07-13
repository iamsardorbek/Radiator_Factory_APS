package com.akfagroup.radiatorfactoryaps;

public class PointData {
    private String equipmentName, shopName, qrCode;
    int pointNo;
    public PointData(int pointNo, String equipmentName, String shopName, String qrCode)
    {
        this.equipmentName = equipmentName;
        this.shopName = shopName;
        this.pointNo = pointNo;
        this.qrCode = qrCode;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getShopName() {
        return shopName;
    }

    public int getPointNo() {
        return pointNo;
    }

    public String getQrCode() {
        return qrCode;
    }
}
