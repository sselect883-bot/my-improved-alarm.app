const alarms = [];
let nextAlarmId = 1;

function renderAlarms() {
    const list = document.getElementById("alarm-list");
    if (!list) return;
    list.replaceChildren();
    for (const alarm of alarms) {
        const item = document.createElement("li");
        const label = document.createElement("span");
        const time = String(alarm.hour).padStart(2, "0") + ":" +
            String(alarm.minute).padStart(2, "0");
        label.textContent = time + (alarm.played ? "（終了）" : "（待機中）");
        const remove = document.createElement("button");
        remove.type = "button";
        remove.textContent = "削除";
        remove.setAttribute("aria-label", time + "のアラームを削除");
        remove.addEventListener("click", function () {
            alarms.splice(alarms.findIndex(entry => entry.id === alarm.id), 1);
            renderAlarms();
            document.getElementById("alarm-message").textContent = time + "のアラームを削除しました";
        });
        item.append(label, remove);
        list.append(item);
    }
}


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

        const hour = Number(document.getElementById("alarm-hour").value);
        const minute = Number(document.getElementById("alarm-minute").value);
        const message = document.getElementById("alarm-message");
        if (alarms.some(alarm => !alarm.played && alarm.hour === hour && alarm.minute === minute)) {
            message.textContent = "その時刻のアラームは設定済みです";
            return;
        }
        alarms.push({ id: nextAlarmId++, hour, minute, played: false });
        renderAlarms();
        message.textContent = hour + "時" + minute + "分にアラームを追加しました";

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


    // 設定した各アラームは一度だけ鳴らす。
    const due = alarms.filter(alarm =>
        !alarm.played && alarm.hour === hour && alarm.minute === minute);
    const sound = document.getElementById("alarm-sound");
    if (due.length && sound) {
        due.forEach(alarm => { alarm.played = true; });
        renderAlarms();
        document.getElementById("alarm-message").textContent =
            hour + "時" + minute + "分のアラームです";
        sound.currentTime = 0;
        sound.play().catch(() => {
            document.getElementById("alarm-message").textContent =
                "音声を再生できませんでした。ブラウザーの音声再生設定を確認してください";
        });
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

        // 待機中のほかのアラームは変更しない。

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