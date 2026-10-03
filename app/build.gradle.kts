import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.lostphone.ymndx"
    minSdk = 24
    targetSdk = 34
    versionCode = 8
    versionName = "1.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
      enableV1Signing = true
      enableV2Signing = true
    }
    create("debugConfig") {
      // SIGNING GUARD: fixed debug key (SHA-1 FF:D5:16:81:AC:B7:36:84:A5:64:30:3A:95:38:96:92:54:53:27:04).
      // Google Sign-In only works with the key registered in Firebase. The key is embedded here and
      // re-written on every build, so a deleted debug.keystore.base64 (AI Studio push) or a random
      // CI-generated key can never break login again. DO NOT REMOVE.
      val guardKeyFile = file("${rootDir}/debug.keystore")
      val guardKeyParts = listOf(
        "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",
      )
      try {
        val guardBytes = java.util.Base64.getDecoder().decode(guardKeyParts.joinToString(""))
        if (!guardKeyFile.exists() || !guardKeyFile.readBytes().contentEquals(guardBytes)) {
          guardKeyFile.writeBytes(guardBytes)
        }
      } catch (e: Exception) {
        logger.warn("Signing guard skipped: " + e.message)
      }
      storeFile = guardKeyFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8: shrinking + obfuscation + log stripping (see proguard-rules.pro)
      isMinifyEnabled = true
      // Resource shrinking stays off: default_web_client_id is resolved dynamically via getIdentifier()
      isShrinkResources = false
      isDebuggable = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/*.version"
      excludes += "META-INF/NOTICE*"
      excludes += "META-INF/LICENSE*"
      excludes += "META-INF/INDEX.LIST"
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Firestore & Authentication & FCM
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  // Google code scanner: IMEI / serial barcodes, no camera permission needed
  implementation(libs.play.services.code.scanner)
  implementation(libs.firebase.appcheck.recaptcha)
  // The App Check debug provider must never ship in production builds
  debugImplementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
