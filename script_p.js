

let alarmHour = null;
let alarmMinute = null;
let alarmPlayed = false;

// アラーム設定ボタン
const alarmButton = document.getElementById("set-button");

alarmButton.addEventListener("click", function() {

    alarmHour = Number(document.getElementById("alarm-hour").value);
    alarmMinute = Number(document.getElementById("alarm-minute").value);
    

    if(alarmHour==""&&alarmMinute==""){
        document.getElementById("alarm-message").textContent=
        "時刻が設定されていません";
        return;
    }

    

    alarmPlayed = false;

    document.getElementById("alarm-message").textContent =
        alarmHour + "時" +
        alarmMinute + "分にアラームを設定しました";
});

const menuButton = document.getElementById("menu-button");

menuButton.addEventListener("click",function(){
     menu.classList.toggle("open");
})

/*const backForm = document.getElementById("back-form");

backForm.addEventListener("click",function(){
     backForm.classList.remove("open");
})*/

function updateclock() {

    const now = new Date();

    const hour = now.getHours();
    const minute = now.getMinutes();
    const second = now.getSeconds();

    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = now.getDay();


    // 時刻表示
    const clock = document.getElementById("realtime-clock");


    clock.textContent = 
        String(hour).padStart(2,"0") + " : " +
        String(minute).padStart(2,"0") + " : " +
        String(second).padStart(2,"0");


    // 日付・曜日
    const weekdays = [
        "日",
        "月",
        "火",
        "水",
        "木",
        "金",
        "土"
    ];

    const dayText = weekdays[day];

    const dateElement =
        document.getElementById("date-text");

    dateElement.textContent =
        year + "年" +
        month + "月" +
        date + "日(" +
        dayText + ")";


    // アラーム判定
    if (
        hour === alarmHour &&
        minute === alarmMinute &&
        alarmPlayed === false
    ) {

        const sound =
            document.getElementById("alarm-sound");

        sound.play();
        alarmPlayed = true;
    }
}

const stopButton = document.getElementById("stop-button");

stopButton.addEventListener("click", function(){
    const sound = document.getElementById("alarm-sound");
    sound.pause();
    sound.currentTime = 0;

    alarmPlayed = true;
})



setInterval(updateclock, 1000);

updateclock();