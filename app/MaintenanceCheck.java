package com.akfa.apsproject;

public class MaintenanceCheck {
    private String checked_by, duration, equipment_name, shop_name, time_finished, date_finished;
    private int num_of_detected_problems;
    public MaintenanceCheck(){}
    public MaintenanceCheck(String checked_by, String duration, String equipment_name, String shop_name, String time_finished, int num_of_detected_problems, String date_finished)
    {
        this.checked_by = checked_by;
        this.duration = duration;
        this.equipment_name = equipment_name;
        this.shop_name = shop_name;
        this.time_finished = time_finished;
        this.num_of_detected_problems = num_of_detected_problems;
        this.date_finished = date_finished;
    }

    public String getChecked_by() {
        return checked_by;
    }

    public String getDuration() {
        return duration;
    }

    public String getEquipment_name() {
        return equipment_name;
    }

    public String getShop_name() {
        return shop_name;
    }

    public String getTime_finished() {
        return time_finished;
    }

    public int getNum_of_detected_problems() {
        return num_of_detected_problems;
    }

    public String getDate_finished() {
        return date_finished;
    }
}
