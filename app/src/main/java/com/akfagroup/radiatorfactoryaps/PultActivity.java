package com.akfagroup.radiatorfactoryaps;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;

//----------------PULT----------------//
public class PultActivity extends AppCompatActivity implements View.OnTouchListener, ChooseProblematicPointDialog.ChooseProblematicStationDialogListener, QRCodeDialog.QRCodeDialogListener { //здесь пульты
    // all variables
    private final int NUM_OF_BUTTONS = 4;
    private Button[] andons = new Button[NUM_OF_BUTTONS];

    //состояния кнопок (0, 1, 2)
    //0 - SOLVED (не горит, решена или нейтрально, все в порядке)
    //1 - DETECTED (мигает, проблема обнаружена и спец не приходил)
    //2 - SPECIALIST_CAME (горит -> идет работа)
    public Integer[] btnCondition = new Integer[NUM_OF_BUTTONS];

    private boolean[] btnBlocked = new boolean[NUM_OF_BUTTONS]; //состояния заблокированности кнопок. Блокируется, если обнаружили проблему и вызвали спеца, а он еще не пришел
    private String[] positionTypes = {"repair", "quality", "raw", "master"}; //в БД в ветке пульта NUM_OF_BUTTONS дочерних веток. На момент написания коммента, это repair, quality, raw, master
    //positionTypes создан для упрощения работы с этими подветками и связания из строковый названий с числами-индексами

    private String nomerPulta, equipmentName, shopName; //данные, хранящиеся в ветке отдельного пользотвателя-оператора (номер пульта, назв-е линии, цеха)
    private String employeeLogin, employeePosition; //inter-activity strings
    public int shopNo, equipmentNo; //для сохранения индексов цеха и линии
    private final String DETECTED = "DETECTED", SPECIALIST_CAME = "SPECIALIST_CAME", SOLVED = "SOLVED"; //константы, хранящие в себе состояния обнаруженных проблем ОБНАРУЖЕНА, СПЕЦ_ПРИШЕЛ, РЕШЕНА. Это для понятного кода без хардкодинга

    //andonDrawableReferences - 2D массив, каждая строка массива соответствует drawable-элементам отдельной кнопки (*у каждой кнопки свои цвета мигания и нейтрального состояния)
    //0-столбец соотвествует бэкграунду при состоянии SOLVED, 1-столбец соот. состоянию DETECTED (мигание; в этом drawable 2 картинки смиксованы и воспроизводятся как гиф-анимация AnimationDrawable
    //2-столбец соот. сост-ю SPECIALIST_CAME (горит ярко)
    private int [][] andonDrawableReferences = {{R.drawable.remont_button, R.drawable.remont_button_animation, R.drawable.remont_button_alert},
            {R.drawable.otk_button, R.drawable.otk_button_animation, R.drawable.otk_button_alert},
            {R.drawable.materials_button, R.drawable.materials_button_animation, R.drawable.materials_button_alert},
            {R.drawable.master_button, R.drawable.master_button_animation, R.drawable.master_button_alert}};

    //объекты связанные с БД
    FirebaseDatabase database = FirebaseDatabase.getInstance(); //объект всей БД
    DatabaseReference pultRef; //ссылка к конкретнуму пульту, с которым связан данный оператор (current user)
    ValueEventListener pathToRelevantPultQuery; //listener того пульта
    //для navigation bar
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initInstances(); //инициализация всех layout элементов
        setAndonsVisibility(false); //спрятать все кнопки-андоны, пока не установлена связь с веткой пульта в БД
        Bundle arguments = getIntent().getExtras(); //аргументы переданные с других активити
        if(arguments != null) //был ли сделан правилно логин и возвратил ли он оттуда номер пульта, или передал ли предыдущий активити аргументы
        {
            employeePosition = arguments.getString("Должность");
            employeeLogin = arguments.getString("Логин пользователя");
            //создать ссылку на ветку пользователя для получения номера пульта
            DatabaseReference userRef = database.getReference("Users/" + employeeLogin);
            findPathToRelevantPult(); //инициализировать pathToRelevantPultQuery для пульта этого юзера
            userRef.addListenerForSingleValueEvent(pathToRelevantPultQuery); //привязать pathToRelevantPultQuery к ветке пользователей (там далее берутся данные о линии, цехе и номере пульта, что запускает потом асинхронный listener для пульта
        }
        else Toast.makeText(getApplicationContext(), "Ошибка, постарайтесь зайти снова", Toast.LENGTH_LONG).show(); //сработает, если в код сделали изменения и это нарушило стабильность работы приложения
        //напр: забыли приписать putExtras к интенту, открывшему этот активити PultActivity
        toggle = InitNavigationBar.setUpNavBar(PultActivity.this, getApplicationContext(),  getSupportActionBar(), employeeLogin, employeePosition, R.id.pult, R.id.activity_main); //setUpNavBar выполняет все действия и возвращает toggle, которые используется в функции onOptionsItemSelected()
        setTitle("Загрузка данных...");
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void initInstances(){
        //инициализация всех объектов layout и их listeners
        andons[0] = findViewById(R.id.repair_btn);
        andons[1] = findViewById(R.id.quality_btn);
        andons[2] = findViewById(R.id.raw_btn);
        andons[3] = findViewById(R.id.master_btn);

        // set on touch listener for andon-button
        for(Button andon : andons)
            andon.setOnTouchListener(this);

    }

    //связывается с веткой текущего пользователя по его логину, считывает названия его цеха, линии и номер пульта (shopName, equipmentName, nomerPulta)
    //
    private void findPathToRelevantPult() { //инициализирует листенер состояний кнопок пульта nomerPulta в БД в ветке "Pults" именно линии, за которую ответственен оператор
        pathToRelevantPultQuery = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot userSnap) {
                //считывает названия цеха, линии и номер пульта текущего пользователя
                nomerPulta = userSnap.child("pult_no").getValue().toString();
                shopName = userSnap.child("shop_name").getValue().toString();
                equipmentName = userSnap.child("equipment_name").getValue().toString();
                //основываясь на этих строковых данных, найдем путем прохождения, в худшем случае, по всей ветке Shops, ища цех с названием shopName
                DatabaseReference shopsRef = database.getReference("Shops");
                shopsRef.addListenerForSingleValueEvent(new ValueEventListener() //проходимся лишь один раз, поэтому addListenerForSingleValueEvent
                {
                    @Override public void onDataChange(@NonNull DataSnapshot shops)
                    { //найдем цех, в котором работает оператор
                        for (DataSnapshot shop : shops.getChildren()) //пройдись по каждой подветке Shops, тобиш по ветке каждого цеха
                        {
                            shopNo = Integer.parseInt(shop.getKey()); //сохрани номер цеха (key каждого цеха - его номер)
                            String shopNameDB = (String) shop.child("shop_name").getValue(); //название текущего цеха, по которому мы проходимся в БД
                            if (shopNameDB.equals(shopName)) //если искомое название цеха такое же как и у названия текущего цеха из БД (shopNameDB), войди глубже в эту ветку и найди искомую линии оборудования
                            { //тот самый цех
                                DataSnapshot equipmentLines = shop.child("Equipment_lines");
                                for (DataSnapshot equipmentLine : equipmentLines.getChildren()) //пройдись по каждой подветке Equipment_lines текущего цеха, тобиш по ветке каждой линии оборудования
                                { //теперь найдем линию, за которой смотрит оператор
                                    equipmentNo = Integer.parseInt(equipmentLine.getKey()); //сохрани номер линии (key каждой линии - ее номер)
                                    String equipmentNameDB = (String) equipmentLine.child("equipment_name").getValue();//название текущей линии, по которой мы проходимся в БД
                                    if (equipmentNameDB.equals(equipmentName)) //если искомое название линии такое же как и у названия текущей линии из БД (equipmentNameDB), войди глубже в эту ветку и найди искомый пульт (Pults/1 или Pults/2)
                                    {
                                        //та самая линия. Все, запускаем постоянный listener измененений на пульте данной линии
                                        //инициализируем listener базы данных этого пульта, чтобы считывать оттуда данные
                                        //пока данные не пришли с базы, в pultInfo будет показываться "Загрузка данных"

                                        initPultRefListener(shopNo, equipmentNo, nomerPulta); //ГЛАВНЫЙ ЭКШН, ВНУТРИ ЭТОЙ ФУНКЦИИ ИДЕТ ПРИВЯЗКА НЕПОСРЕДСТВЕННО К НУЖНОМУ ПУЛЬТУ
                                        setAndonStates(); //проверь ветку Urgent problems, если там есть проблемы, связанные с нашим пультом, измени соответственно состояние кнопок пульта
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
                });


            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        };
    }

    private void initPultRefListener(int shopNoLocal, int equipmentNoLocal, final String nomerPultaLocal)
    { //функция вызывается единожды, когда findPathToRelevantPult() находить нужные нам название цеха, линии и номер пульта
        //----------САМЫЙ ГЛАВНЫЙ ЭКШН ЗДЕСЬ!!!----------//
        //----- ССЫЛКА НЕПОСРЕДСТВЕННО К ПУЛЬТУ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ, ЭТО ПОЗВОЛЯЕТ СОХРАНЯТЬ СИНХРОНИЗАЦИЮ КАЖДОЙ КНОПКИ С ЕЕ СОСТОЯНИЕМ В БД -----//
        pultRef = database.getReference("Shops/" + shopNoLocal + "/Equipment_lines/" + equipmentNoLocal + "/Pults/" + nomerPultaLocal);
        pultRef.addValueEventListener(new ValueEventListener()
        { //listener будет многоразовым ~ постоянным. Нужно, чтобы он следил за всеми изменениями состояний пульта динамично
            @Override public void onDataChange(@NonNull DataSnapshot pultButtonStates)
            {
                setTitle(equipmentName + ". Пульт " + nomerPultaLocal); //app bar текст задаем. ("Пульт 1", "Пульт 2" и тд)
                //цикл ниже пройдется по каждой кнопке этого пульта: считает состояния кнопок, задаст их background с помощью функции setAndonBackground(int, int)
                for (DataSnapshot buttonStateSnap : pultButtonStates.getChildren())
                {
                    //ветка отдельной кнопки (мастер, ремонт, отк, сырье) именно этого пульта
                    //каждая кнопка отвечает за вызов специалиста определенного профиля (МАСТЕР, ОТК, РЕМОНТ, СЫРЬЕ и тд)
                    String whoIsNeededPosition = buttonStateSnap.getKey();
                    int whoIsNeededIndex = renderWhoIsNeededIndex(whoIsNeededPosition); //каждому названию профиля соответствует свой индекс в массиве positionTypes
                    int buttonState = Integer.parseInt(buttonStateSnap.getValue().toString()); //состояние кнопки, на которой мы сейчас в процессе итерации
                    btnCondition[whoIsNeededIndex] = buttonState; //записать это состояние кнопки в массив btnCondition
                    setAndonBackground(whoIsNeededIndex, buttonState); //синхронизовать внешнее состояние данной кнопки с состоянием в БД
                }
                setAndonsVisibility(true); //когда уже считали все данные с БД, сделать элементы видимыми
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setAndonStates()
    {
        //функция срабатывает при запуске активити единожды, ОДНАКО имеется внутри асинхронный листенер
        //инициализируй состояния кнопок в зависимости от ветки Urgent_problems
        DatabaseReference urgentProblemsRef = database.getReference("Urgent_problems"); //ссылка на все срочные проблемы
        urgentProblemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot urgentProblemsSnap) {
                for(DataSnapshot urgentProblemSnap : urgentProblemsSnap.getChildren())
                { //рассмотрим по одному каждую проблему (aka Query)
                    UrgentProblem urgentProblem = urgentProblemSnap.getValue(UrgentProblem.class); //считать ветку в объект срочная проблема
                    String operatorLogin = urgentProblem.getOperator_login();
                    if(operatorLogin.equals(employeeLogin) && urgentProblemSnap.child("status").getValue().toString().equals(DETECTED))
                    { //если проблема относится к данному пользователю, но специалист еще не пришел (состояние 1), но оператор сообщил  о проблеме уже до этого
                        String whoIsNeededPosition = urgentProblem.getWho_is_needed_position();
                        int whoIsNeededIndex = renderWhoIsNeededIndex(whoIsNeededPosition); //какая кнопка было нажата (кого вызвали?)

                        btnBlocked[whoIsNeededIndex] = true; //заблокируй кнопку
                        btnCondition[whoIsNeededIndex] = 1; //смени состояние кнопки в мигающее
                        updateButton(whoIsNeededIndex);
                        //если update button значок QR сам соотвественно появится, потому что вызовется pultRef Listener
//                        setQRIconOnAndonButton(1, whoIsNeededIndex); //поставь значок QR в край кнопки (значит кнопка в мигающем состоянии)

                        //---- ВНИМАНИЕ, НИЖЕ ОБЪЯВЛЯЕТСЯ АСИНХРОННЫЙ СЛУШАТЕЛЬ ИЗМЕНЕНИЙ ИМЕННО НА ВЕТКЕ ЭТОЙ СРОЧНОЙ ПРОБЛЕМЫ, ПРИ ПРИХОДЕ СПЕЦИАЛИСТА, ----//
                        //---- СЛУШАТЕЛЬ (LISTENER) ПЕРЕВОДИТ КНОПКУ В СОСТОЯНИЕ 2, РАБЛОКИРУЕТ ЕЕ И ОБНОВЛЯЕТ БД ----//
                        DatabaseReference thisUrgentProblem =  database.getReference("Urgent_problems/" + urgentProblemSnap.getKey());
                        thisUrgentProblem.child("status").addValueEventListener(getUrgentProblemStatusListener(whoIsNeededIndex));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private int renderWhoIsNeededIndex(String whoIsNeededLogin)
    {//у каждого названия специальности есть свой индекс. Эта функция служит для перевода названия в индекс
        //используется в функциях setAndonStates(), initPultRefListener()
        //переход от названий должностей в номера
        switch(whoIsNeededLogin)
        {
            case "repair":
                return 0;
            case "quality":
                return 1;
            case "raw":
                return 2;
            case "master":
                return 3;
        }
        return 0;
    }

    private void setAndonBackground(int andonIndex, int buttonState)
    {//set up the button background behaviour
        // behaviour depends on the button index, its condition value number
        //that's why there are two switch statements
        int drawableReference = andonDrawableReferences[andonIndex][buttonState]; //записать в локальную переменную ссылку к нужному в данном случае drawable - элементе
        switch(buttonState)
        {
            case 0: //случай SOLVED (не гореть)
            case 2: //случай SPECIALIST_CAME (гореть)
                andons[andonIndex].setBackgroundResource(drawableReference);
                setQRIconOnAndonButton(buttonState, andonIndex); //в зависимости от состояния кнопки, вставь/убери иконку QR
                break;
            case 1: //в случае DETECTED кнопка должна мигать (бэкграунд - гифка), для это запускаем AnimationDrawable
                andons[andonIndex].setBackgroundResource(drawableReference);
                AnimationDrawable problemAlert = (AnimationDrawable) andons[andonIndex].getBackground(); //берем задний фон кнопки и отдаем его AnimationDrawable объекту
                problemAlert.start(); //стартуем анимацию (aka гифка)
                setQRIconOnAndonButton(buttonState, andonIndex); //в зависимости от состояния кнопки, вставь/убери иконку QR
                break;
        }
        btnCondition[andonIndex] = buttonState; //обнови состояние кнопки в массиве btnCondition
    }

    private void setQRIconOnAndonButton(int buttonState, int whoIsNeededIndex) //значок QR появляется в левом/правом краю кнопки когда кнопка в состоянии DETECTED
    {       //в зависимости от buttonState состояния кнопки, вставить/убрать код QR
        if(buttonState == 1) { //вставить значок
            //следующие 4 строки определяют, куда поставить значок QR - справа или слева (зависит от четности)
            if (whoIsNeededIndex % 2 == 0) //четный слева
                andons[whoIsNeededIndex].setCompoundDrawablesWithIntrinsicBounds(R.drawable.qrcode_drawable, 0, 0, 0);
            else //нечетный справа
                andons[whoIsNeededIndex].setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.qrcode_drawable, 0);
        }
        else //убрать с кнопки значок QR  (состояние кнопки SOLVED или SPECIALIST_CAME
            andons[whoIsNeededIndex].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    private void setAndonsVisibility(boolean visible) {
        //измени видимость кнопок
        int visibilityState;
        if(visible) visibilityState = View.VISIBLE;
        else visibilityState = View.INVISIBLE;
        for(Button andon : andons) //сделай все кнопки либо видимыми, либо скрытыми
            andon.setVisibility(visibilityState);
    }

    private ValueEventListener getUrgentProblemStatusListener(final int whoIsNeededIndex)
    {
        //---- ВНИМАНИЕ, НИЖЕ ОБЪЯВЛЯЕТСЯ АСИНХРОННЫЙ СЛУШАТЕЛЬ ИЗМЕНЕНИЙ СТАТУСА (РЕШЕНА, СПЕЦ ПРИШЕЛ, ОБНАРУЖЕНА) ЭТОЙ СРОЧНОЙ ПРОБЛЕМЫ, ПРИ ПРИХОДЕ СПЕЦИАЛИСТА, ----//
        //---- СЛУШАТЕЛЬ (LISTENER) ПЕРЕВОДИТ КНОПКУ В СОСТОЯНИЕ 2, РАБЛОКИРУЕТ ЕЕ И ОБНОВЛЯЕТ БД ----//
        ValueEventListener urgentProblemListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot thisUrgentProblemStatusSnap) {
                if(thisUrgentProblemStatusSnap.getValue().toString().equals(SPECIALIST_CAME)) //если специалист пришел
                {
                    btnBlocked[whoIsNeededIndex] = false; //разблокируй кнопку
                    btnCondition[whoIsNeededIndex] = 2; //переведи состояние кнопки в состояние "специалист пришел"
                    updateButton(whoIsNeededIndex); //внеси изменения в БД
                    //значок QR с кнопки автоматом уберется в pultRef Listener
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        };
        return urgentProblemListener;
        //----КОНЕЦ ОБЪЯВЛЕНИЯ АСИНХРОННОГО СЛУШАТЕЛЯ БД----//
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        //связано с навигейшн бар
        if(toggle.onOptionsItemSelected(item)) return true;
        else return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View button, MotionEvent event) {
        //обработка касаний - зажато (DOWN для красоты) , отпущено (UP)
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN: //зажатое состояние (pressed)
                switch(button.getId())
                {
                    case R.id.repair_btn:
                    case R.id.raw_btn:
                        button.setBackgroundResource(R.drawable.pressed_even_button);
                        break;
                    case R.id.quality_btn:
                    case R.id.master_btn:
                        button.setBackgroundResource(R.drawable.pressed_odd_button);
                        break;
                }
                break;
            case MotionEvent.ACTION_UP: //отпущенное состояние
                switch(button.getId()) {
                    //4 случая ниже - для обработки больших кнопок: ремонт, отк, сырье, мастер
                    case R.id.repair_btn:
                        btnCondition[0]++; //итерировать состояние кнопки в следующее состояние
                        updateButton(0); //если btnCondition перешло из состояния 2(SPECIALIST_CAME) в 3, приравнивает ее состояние в btnCondition к 0 и записывает это в БД
                        processCallForSpecialist(0); //проверяет статус срочной проблема (если таковая активная связана с данной кнопкой)
                        //следующие 3 случая делают то же самое, просто айдишка каждой кнопки уникальна и унифицировать их в массив с индексами неэффективно на данный момент
                        break;
                    case R.id.quality_btn:
                        btnCondition[1]++;
                        updateButton(1);
                        processCallForSpecialist(1);
                        break;
                    case R.id.raw_btn:
                        btnCondition[2]++;
                        updateButton(2);
                        processCallForSpecialist(2);
                        break;
                    case R.id.master_btn:
                        btnCondition[3]++;
                        updateButton(3);
                        processCallForSpecialist(3);
                        break;
                }
                break;
        }
        return true;
    }

    private void updateButton(int whoIsNeededIndex)
    { //запиши в базу данных новое состояние одной конкретной кнопки
        //whoIsNeededIndex - тип сигнала (Ремонт, мастер, отк, сырье)
        // check status and drop for 0 if more than 2
        if ((btnCondition[whoIsNeededIndex] >= 3)) //всего 3 состояния (нейтральное - порядок, мигает - срочная проблема ждет решения, горит - специалист работает
            //если больше 2, переведи в 0
            btnCondition[whoIsNeededIndex] = 0;

        //занеси новое состояние в базу данных
        pultRef = database.getReference("Shops/" + shopNo + "/Equipment_lines/" + equipmentNo + "/Pults/" + nomerPulta); //ссылка именно к этому пульту
        pultRef.child(positionTypes[whoIsNeededIndex]).setValue(btnCondition[whoIsNeededIndex]); //непосредственно асинхроннкая запись в БД
    }

    private void processCallForSpecialist(final int whoIsNeededIndex)
    {//обработай нажатие на кнопку, проверяя заблокирована ли она, и реши, какой диалог (ChooseProblematicStationDialog / QRCodeDialog) показать или перевести ее в SOLVED status
        if(btnCondition[whoIsNeededIndex] == 1) //DETECTED
        {//хочет вызвать специалиста
            btnCondition[whoIsNeededIndex] = 0;
            updateButton(whoIsNeededIndex);

            //startDialogFragment для выбора проблемного участка и вызова специалиста
            DialogFragment dialogFragment = new ChooseProblematicPointDialog();
            Bundle bundle = new Bundle();
            bundle.putString("Логин пользователя", employeeLogin);
            bundle.putInt("Вызвать специалиста", whoIsNeededIndex);
            dialogFragment.setArguments(bundle);
            dialogFragment.show(getSupportFragmentManager(), "Выбор участка");
        }
        else if(btnBlocked[whoIsNeededIndex] && btnCondition[whoIsNeededIndex] == 2) //ХОЧЕТ ПЕРЕВЕСТИ В SPECIALIST_CAME, НО специалист не пришел, поэтому показываем юзеру QR Код, чтобы мастер отсканировал
        {
            //если кнопка заблокирована(спец еще не пришел), высветить QR Code
            btnCondition[whoIsNeededIndex] = 1; //вернуть в состояние "специалист не пришел, ПРОБЛЕМА"
            updateButton(whoIsNeededIndex);
            //Открыть диалог с QR Кодом
            DialogFragment dialogFragment = new QRCodeDialog();
            Bundle bundle = new Bundle();
            bundle.putString("Название цеха", shopName);
            bundle.putString("Название линии", equipmentName);
            bundle.putString("Логин пользователя", employeeLogin);
            bundle.putString("Номер пульта", nomerPulta);
            bundle.putString("Должность", positionTypes[whoIsNeededIndex]);
            dialogFragment.setArguments(bundle);
            dialogFragment.show(getSupportFragmentManager(), "QR Код");
        }
        else if(btnCondition[whoIsNeededIndex] == 0 && !btnBlocked[whoIsNeededIndex])
        {
            //хочет нажать для индикации того, что проблема решена (Из горящего состояния перевести в нейтральное)

            //---получить данные о дате и времени---//
            final String dateSolved;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            dateSolved = sdf.format(new Date());
            final String timeSolved;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
            timeSolved = sdf1.format(new Date());
            //---конец данные о дате и времени---//

            final DatabaseReference urgentProblemsRef = database.getReference("Urgent_problems");
            urgentProblemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot urgentProblemsSnap) {
                    for(DataSnapshot urgentProblemSnap : urgentProblemsSnap.getChildren())
                    {
                        String operatorLogin = urgentProblemSnap.child("operator_login").getValue().toString();
                        String whoIsNeededLogin = urgentProblemSnap.child("who_is_needed_position").getValue().toString();
                        //проверка (Query) выбор срочной проблемы с тем же логином оператора, специалиста и проблема, которая не решена еще
                        if(operatorLogin.equals(employeeLogin) && whoIsNeededLogin.equals(positionTypes[whoIsNeededIndex]) && urgentProblemSnap.child("status").getValue().toString().equals(SPECIALIST_CAME)) {
                            //если специалист пришел, но еще не решил проблему (status = SPECIALIST_CAME)

                            //---ввести изменения о решенности проблемы в базу данных---//
                            String thisProbKey = urgentProblemSnap.getKey();
                            urgentProblemsRef.child(thisProbKey).child("date_solved").setValue(dateSolved);
                            urgentProblemsRef.child(thisProbKey).child("time_solved").setValue(timeSolved);
                            urgentProblemsRef.child(thisProbKey).child("status").setValue(SOLVED);
                            return;
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
    }

    @Override
    public void submitPointNo(int pointNo, String equipmentLineName, String shopName, String operatorLogin, final int whoIsNeededIndex) {
        //интерфейс функция которая вызывается при успешном сообщении о существовании проблемы при правильном выборе проблемного участка и нажатии ОК в диалоге ChooseProblematicStationDialog
        //вбить экстренную проблему в базу, QR генерируется внутри самого диалога
        DatabaseReference thisUrgentProblem = database.getReference().child("Urgent_problems").push(); //вбить новую ветку в urgent problems
        String qrRandomCode = GenerateRandomString.randomString(3); //сгенерировать для этой проблемы случайный код с 3-символами (цифры и буквы заглавные латинские)

        // ----получи дату и время в строки dateDetected, timeDetected----//
        final String dateDetected;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        dateDetected = sdf.format(new Date());
        final String timeDetected;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
        timeDetected = sdf1.format(new Date());
        //----считал дату и время----//

        btnCondition[whoIsNeededIndex] = 1;
        updateButton(whoIsNeededIndex);

        //вбить обнаруженную срочную проблему в базу
        thisUrgentProblem.setValue(new UrgentProblem(pointNo, nomerPulta, equipmentLineName, shopName, operatorLogin, positionTypes[whoIsNeededIndex], qrRandomCode, dateDetected, timeDetected, DETECTED)); //DETECTED - это строка "DETECTED"
        btnBlocked[whoIsNeededIndex] = true; //задать состояние кнопки блокированным

        //здесь же добавить БД слушатель, чтобы реагировал позднее на изменения в БД (specialist_came, solved)
        thisUrgentProblem.child("status").addValueEventListener(getUrgentProblemStatusListener(whoIsNeededIndex));
    }

    @Override
    public void onDialogCanceled(int whoIsNeededIndex) {
        //если диалог выбора проблемного участка отменили/закрыли
        //возврати мигающую кнопку в нейтральное состояние SOLVED (0)
        btnCondition[whoIsNeededIndex] = 0;
        updateButton(whoIsNeededIndex);
    }

    @Override
    public void onQRCodeDialogCanceled(int whoIsNeededIndex, int andonState) {
        //чтобы кнопка не  зависла в состоянии ACTION_DOWN (серый фон при нажатии), повторно зададим ее фон вызовом метода setAndonBackground
        setAndonBackground(whoIsNeededIndex, andonState);
        updateButton(whoIsNeededIndex);
    }

}
