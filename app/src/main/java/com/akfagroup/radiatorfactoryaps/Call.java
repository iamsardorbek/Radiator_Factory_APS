package com.akfagroup.radiatorfactoryaps;

//----------ДЛЯ READABLE ВНЕСЕНИЯ ДАННЫХ О ВЫЗОВЕ ОПЕРАТОРА/МАСТЕРА В FIREBASE--------//
public class Call {
    public String date_called, time_called, called_by, who_is_needed_position, equipment_name, shop_name, problem_key;
    public int point_no;
    public boolean complete; //удовлетворил ли оператор/мастер вызов
    public Call(){};
    public Call(String date_called, String time_called, String called_by, String who_is_needed_position, int point_no, String equipment_name, String shop_name, boolean complete, String problem_key){
        this.date_called = date_called;
        this.time_called = time_called;
        this.called_by = called_by;
        this.who_is_needed_position = who_is_needed_position;
        this.equipment_name = equipment_name;
        this.shop_name = shop_name;
        this.point_no = point_no;
        this.complete = complete;
        this.problem_key = problem_key;
    };

    public Call(String date_called, String time_called, String called_by, String who_is_needed_position, int point_no, String equipment_name, String shop_name, boolean complete){
        this.date_called = date_called;
        this.time_called = time_called;
        this.called_by = called_by;
        this.who_is_needed_position = who_is_needed_position;
        this.equipment_name = equipment_name;
        this.shop_name = shop_name;
        this.point_no = point_no;
        this.complete = complete;
    };


    public int getPoint_no() {
        return point_no;
    }

    public String getShop_name() {
        return shop_name;
    }

    public String getEquipment_name() {
        return equipment_name;
    }

    public String getCalled_by() {
        return called_by;
    }

    public String getDate_called()
    {
        return date_called;
    }

    public String getTime_called() {
        return time_called;
    }

    public String getWho_is_needed_position()
    {
        return who_is_needed_position;
    }

    public boolean getComplete() {
        return complete;
    }

    public String getProblem_key() {
        return problem_key;
    }
}
