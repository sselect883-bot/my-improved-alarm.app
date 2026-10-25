"use strict";
// This file is packaged only inside Android. GitHub Pages keeps script2.js.
const bridge = window.NativeAlarm;
function message(text) {
    const box = document.getElementById("alarm-message");
    if (box) box.textContent = text;
}
function perform(operation, success) {
    try {
        const result = JSON.parse(operation());
        message(result.ok ? success : result.error);
        renderAlarms();
    } catch (_) { message("Androidとの連携に失敗しました。アプリを開き直してください。"); }
}
function renderAlarms() {
    const list = document.getElementById("alarm-list");
    if (!list) return;
    const alarms = JSON.parse(bridge.list());
    list.replaceChildren();
    for (const alarm of alarms) {
        const li = document.createElement("li");
        const label = document.createElement("span");
        const at = new Date(alarm.at);
        label.textContent = at.toLocaleString("ja-JP", {
            month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit"
        }) + "（予約済み）";
        const remove = document.createElement("button");
        remove.textContent = "削除";
        remove.type = "button";
        remove.setAttribute("aria-label", label.textContent + "のアラームを削除");
        remove.addEventListener("click", () =>
            perform(() => bridge.remove(alarm.id), "アラームを削除しました"));
        li.append(label, remove);
        list.append(li);
    }
}
document.getElementById("set-button")?.addEventListener("click", () => {
    const hour = Number(document.getElementById("alarm-hour").value);
    const minute = Number(document.getElementById("alarm-minute").value);
    perform(() => bridge.add(hour, minute), "次の指定時刻に予約しました。画面を消しても鳴ります。");
});
document.getElementById("stop-button")?.addEventListener("click", () =>
    perform(() => bridge.stop(), "鳴動を停止しました。ほかの予約は残っています。"));
const menu = document.getElementById("menu");
document.getElementById("menu-button")?.addEventListener("click", () => menu?.classList.toggle("open"));
document.getElementById("close-menu")?.addEventListener("click", () => menu?.classList.remove("open"));
function tick() {
    const now = new Date();
    const clock = document.getElementById("realtime-clock");
    const date = document.getElementById("date-text");
    if (clock) clock.textContent = now.toLocaleTimeString("ja-JP", { hour12: false });
    if (date) date.textContent = now.toLocaleDateString("ja-JP", {
        year: "numeric", month: "long", day: "numeric", weekday: "short"
    });
    renderAlarms(); // Display only: Android owns all scheduling and playback.
}
tick();
setInterval(tick, 1000);
document.addEventListener("visibilitychange", () => { if (!document.hidden) tick(); });
