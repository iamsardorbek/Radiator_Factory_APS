package com.akfa.apsproject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import static android.content.Context.NOTIFICATION_SERVICE;

public class InitNavigationBar {

    public static ActionBarDrawerToggle setUpNavBar(final Activity activity, final Context context, ActionBar actionBar, final String employeeLogin, final String employeePosition, final int currentMenuID, final int currentActivityID)
    {
        //---------код связанный с nav bar---------//
        //настрой actionBar
        actionBar.show();
        //настрой сам навигейшн бар
        final DrawerLayout drawerLayout;
        ActionBarDrawerToggle toggle;
        NavigationView navigationView;
        drawerLayout = activity.findViewById(currentActivityID);
        toggle = new ActionBarDrawerToggle(activity, drawerLayout,R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        actionBar.setDisplayHomeAsUpEnabled(true);
        navigationView = activity.findViewById(R.id.nv);
        View headerView = navigationView.getHeaderView(0);
        TextView userInfo = headerView.findViewById(R.id.user_info);
        userInfo.setText(employeeLogin);
        navigationView.getMenu().clear();
        switch(employeePosition){ //у каждого специалиста свое меню выводится в nav bar
            case "operator":
                navigationView.inflateMenu(R.menu.operator_menu);
                break;
            case "repair":
                navigationView.inflateMenu(R.menu.repair_menu);
                break;
            case "master":
                navigationView.inflateMenu(R.menu.master_menu);
                break;
            case "raw":
                navigationView.inflateMenu(R.menu.raw_menu);
                break;
            case "quality":
                navigationView.inflateMenu(R.menu.quality_menu);
                break;
            case "head":
                navigationView.inflateMenu(R.menu.head_staff_menu);
                break;
            //other positions shouldn't be able to access checking page at all
            //if some changes, u can add a case
        }
        //ниже действия, выполняемые при нажатиях на элементы нав бара
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == currentMenuID) {
                    //когда нажали на подменю текущего окна, нав бар просто закрывается
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                else
                {
                    switch (id) {
                        case R.id.urgent_problems:
                            Intent openUrgentProblemsList = new Intent(context, UrgentProblemsList.class);
                            openUrgentProblemsList.putExtra("Логин пользователя", employeeLogin);
                            openUrgentProblemsList.putExtra("Должность", employeePosition);
                            activity.startActivity(openUrgentProblemsList);
                            activity.finish();
                            break;
                        case R.id.problems_list: //открой лист ТО проблем
                            Intent openProblemsList = new Intent(context, RepairersProblemsList.class);
                            openProblemsList.putExtra("Логин пользователя", employeeLogin);
                            openProblemsList.putExtra("Должность", employeePosition);
                            activity.startActivity(openProblemsList);
                            activity.finish();
                            break;
                        case R.id.calls:
                            Intent openCallsList = new Intent(context, CallsList.class);
                            openCallsList.putExtra("Логин пользователя", employeeLogin);
                            openCallsList.putExtra("Должность", employeePosition);
                            activity.startActivity(openCallsList);
                            activity.finish();
                            break;
                        case R.id.make_a_call:
                            Intent openMakeACall = new Intent(context, MakeACall.class);
                            openMakeACall.putExtra("Логин пользователя", employeeLogin);
                            openMakeACall.putExtra("Должность", employeePosition);
                            activity.startActivity(openMakeACall);
                            activity.finish();
                            break;
                        case R.id.pult:
                            Intent openMainActivity = new Intent(context, PultActivity.class);
                            openMainActivity.putExtra("Логин пользователя", employeeLogin);
                            openMainActivity.putExtra("Должность", employeePosition);
                            activity.startActivity(openMainActivity);
                            activity.finish();
                            break;
                        case R.id.check_equipment: //переход в модуль проверки
                            Intent openQuest = new Intent(context, QuestListOfEquipment.class);
                            openQuest.putExtra("Логин пользователя", employeeLogin);
                            openQuest.putExtra("Должность", employeePosition);
                            activity.startActivity(openQuest);
                            activity.finish();
                            break;
                        case R.id.web_monitoring: //переход в модуль веб-мониторинга
                            Intent openFactoryCondition = new Intent(context, FactoryCondition.class);
                            openFactoryCondition.putExtra("Логин пользователя", employeeLogin);
                            openFactoryCondition.putExtra("Должность", employeePosition);
                            activity.startActivity(openFactoryCondition);
                            activity.finish();
                            break;
                        case R.id.today_checks:
                            Intent openTodayChecks = new Intent(context, TodayChecks.class);
                            openTodayChecks.putExtra("Логин пользователя", employeeLogin);
                            openTodayChecks.putExtra("Должность", employeePosition);
                            activity.startActivity(openTodayChecks);
                            activity.finish();
                            break;
                        case R.id.checks_history:
                            Intent openChecksHistory = new Intent(context, ChecksHistory.class);
                            openChecksHistory.putExtra("Логин пользователя", employeeLogin);
                            openChecksHistory.putExtra("Должность", employeePosition);
                            activity.startActivity(openChecksHistory);
                            activity.finish();
                            break;
                        case R.id.about: //инфа про приложение и компанию и иинструкции может
                            Intent openAboutApp = new Intent(activity.getApplicationContext(), AboutApp.class);
                            openAboutApp.putExtra("Логин пользователя", employeeLogin);
                            openAboutApp.putExtra("Должность", employeePosition);
                            activity.startActivity(openAboutApp);
                            break;
                        case R.id.log_out: //возвращение в логин page
                            activity.stopService(new Intent(context, BackgroundService.class)); //если до этого уже сервис был включен, выключи сервис
                            NotificationManager notificationManager = (NotificationManager)  context.getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.cancelAll();
                            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor editor = sharedPrefs.edit();
                            editor.clear();
                            editor.commit();
                            Intent logOut = new Intent(context, Login.class);
                            logOut.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            activity.startActivity(logOut);
                            activity.finish();
                        default:
                            return true;
                    }
                    return true;
                }
            }
        });
        return toggle;
    }


}
