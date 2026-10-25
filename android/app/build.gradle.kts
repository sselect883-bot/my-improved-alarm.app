plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "jp.sselect883.myalarm"
    compileSdk = 35
    defaultConfig {
        applicationId = "jp.sselect883.myalarm"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/webAssets"))
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/soundRes"))
}
val prepareWebAssets by tasks.registering(Sync::class) {
    from(rootProject.projectDir.parentFile) {
        include("index.html", "index_w.html", "setting.html", "style2b.css", "style2w.css", "style3.css")
        filesMatching("*.html") {
            filter { line: String ->
                line.replace("script2.js", "native.js")
                    .replace("各アラームは一度だけ鳴ります。ページを離れたり再読み込みすると設定は消えます。",
                        "Androidが予約を管理します。画面を消しても鳴ります。各アラームは次の指定時刻に一度だけ鳴ります。")
                    .replace("ページを離れたり再読み込みすると設定は消えるため、この画面を開いたままにしてください。",
                        "予約はAndroidに保存され、画面を消しても鳴ります。過ぎた時刻を指定すると翌日に予約します。")
            }
        }
    }
    from("src/main/web")
    into(layout.buildDirectory.dir("generated/webAssets"))
}
val prepareSound by tasks.registering(Sync::class) {
    from(rootProject.projectDir.parentFile.resolve("alarm.mp3"))
    into(layout.buildDirectory.dir("generated/soundRes/raw"))
}
tasks.named("preBuild") { dependsOn(prepareWebAssets, prepareSound) }
dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
