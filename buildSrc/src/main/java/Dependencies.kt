object Dependencies {
    const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompatVersion}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlinVersion}"
    const val materialDesign = "com.google.android.material:material:${Versions.materialVersion}"
    const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}"
    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtxVersion}"

    const val jUnit = "junit:junit:${Versions.jUnitVersion}"
    const val testJUnit = "androidx.test.ext:junit:${Versions.testJunitVersion}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCoreVersion}"

    const val navigationFragment =
        "androidx.navigation:navigation-fragment-ktx:${Versions.navigationVersion}"
    const val navigationUi = "androidx.navigation:navigation-ui-ktx:${Versions.navigationVersion}"

    const val glide = "com.github.bumptech.glide:glide:${Versions.glideVersion}"
    const val glideKapt = "com.github.bumptech.glide:compiler:${Versions.glideVersion}"

    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hiltVersion}"
    const val hiltKapt = "com.google.dagger:hilt-android-compiler:${Versions.hiltVersion}"

    const val lifecycleCommon =
        "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycleVersion}"

    const val sdp = "com.intuit.sdp:sdp-android:${Versions.sdpVersion}"
    const val ssp = "com.intuit.ssp:ssp-android:${Versions.sspVersion}"

    //AGConnect
    const val agconnect = "com.huawei.agconnect:agconnect-core:${Versions.agcVersion}"

    //CloudDB
    const val cloudDB = "com.huawei.agconnect:agconnect-cloud-database:${Versions.cloudDBVersion}"

    //Auth
    const val auth = "com.huawei.agconnect:agconnect-auth:${Versions.authVersion}"
    const val huaweiAuth = "com.huawei.agconnect:agconnect-auth-huawei:${Versions.huaweiAuthVersion}"

    //WebRTC
    const val webRTC = "org.webrtc:google-webrtc:${Versions.webRtcVersion}"

    // Ktor
    const val ktorAndroid = "io.ktor:ktor-client-android:${Versions.ktorVersion}"
    const val ktorWebSocket = "io.ktor:ktor-client-websocket:${Versions.ktorVersion}"
    const val ktorCio = "io.ktor:ktor-client-cio:${Versions.ktorVersion}"
    const val ktorJson = "io.ktor:ktor-client-gson:${Versions.ktorVersion}"
}