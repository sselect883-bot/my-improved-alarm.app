let alarmHour = null;
let alarmMinute = null;
let alarmPlayed = false;


// ======================
// ハンバーガーメニュー
// ======================

const menuButton = document.getElementById("menu-button");
const menu = document.getElementById("menu");

if (menuButton && menu) {

    menuButton.addEventListener("click", function() {

        menu.classList.toggle("open");

    });

}


// ======================
// アラーム設定
// ======================

const alarmButton = document.getElementById("set-button");

if (alarmButton) {

    alarmButton.addEventListener("click", function() {

        alarmHour =
            Number(document.getElementById("alarm-hour").value);

        alarmMinute =
            Number(document.getElementById("alarm-minute").value);

        alarmPlayed = false;

        document.getElementById("alarm-message").textContent =
            alarmHour + "時" +
            alarmMinute + "分にアラームを設定しました";

    });

}


// ======================
// 時計
// ======================

function updateclock() {

    const clock =
        document.getElementById("realtime-clock");

    const dateElement =
        document.getElementById("date-text");


    // 時計がないページでは何もしない
    if (!clock || !dateElement) {
        return;
    }


    const now = new Date();

    const hour = now.getHours();
    const minute = now.getMinutes();
    const second = now.getSeconds();

    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const day = now.getDay();


    clock.textContent =
        String(hour).padStart(2, "0") + " : " +
        String(minute).padStart(2, "0") + " : " +
        String(second).padStart(2, "0");


    const weekdays = [
        "日",
        "月",
        "火",
        "水",
        "木",
        "金",
        "土"
    ];

    dateElement.textContent =
        year + "年" +
        month + "月" +
        date + "日(" +
        weekdays[day] + ")";


    // アラーム
    if (
        hour === alarmHour &&
        minute === alarmMinute &&
        alarmPlayed === false
    ) {

        const sound =
            document.getElementById("alarm-sound");

        if (sound) {

            sound.play();

            alarmPlayed = true;

        }
    }
}


// ======================
// アラーム停止
// ======================

const stopButton = document.getElementById("stop-button");

if (stopButton) {

    stopButton.addEventListener("click", function() {

        const sound =
            document.getElementById("alarm-sound");

        if (sound) {

            sound.pause();
            sound.currentTime = 0;

        }

        alarmPlayed = true;

    });

}

const menuClose = document.getElementById("close-menu");

if(menuClose){
     menuClose.addEventListener("click",function(){
          menu.classList.remove("open");
     })
}


// 時計開始
updateclock();

setInterval(updateclock, 1000);