plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
//    id("kotlin-kapt") // 启用 Kapt
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.heartape.whisper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.heartape.whisper"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        getByName("debug") {
            // ★ 2. 测试环境：注入测试版的下载落地页 (注意字符串内必须带有转义的双引号 \")
            buildConfigField("String", "FALLBACK_WEB_URL", "\"https://test.whisper-app.com/download\"")

            // (您甚至可以在这里配置测试服的 API Base_URL)
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
        }

        getByName("release") {
            // ★ 3. 正式环境：注入生产版的真实下载落地页
            buildConfigField("String", "FALLBACK_WEB_URL", "\"https://www.whisper-app.com/download\"")
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8080\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // 图片懒加载库
    implementation(libs.coil.compose)
    // 扩展图标 (用于填充的爱心等)
    implementation(libs.androidx.compose.material.icons.extended)
    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Paging 3 核心库
    implementation("androidx.paging:paging-runtime:3.2.1")
    // Paging 3 Compose 扩展
    implementation("androidx.paging:paging-compose:3.2.1")
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Krossbow STOMP 核心库
    implementation("org.hildan.krossbow:krossbow-stomp-core:7.3.0")
    // Krossbow WebSocket OkHttp 实现库
    implementation("org.hildan.krossbow:krossbow-websocket-okhttp:7.3.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// 允许引用生成的代码
//kapt {
//    correctErrorTypes = true
//}