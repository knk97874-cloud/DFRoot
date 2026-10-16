import java.util.Properties
import java.security.MessageDigest
import java.net.URI
import java.io.File

plugins {
    id("com.android.application")
}

val signingProps = Properties()
val signingPropsFile = rootProject.file("signing.properties")
if (signingPropsFile.exists()) signingProps.load(signingPropsFile.inputStream())

android {
    namespace = "df.root"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "df.root"
        minSdk = 32
        targetSdk = 37
        versionCode = 405
        versionName = "4.5"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    val hasReleaseSigning = signingPropsFile.exists() && signingProps.getProperty("KEYSTORE_FILE") != null
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingProps.getProperty("KEYSTORE_FILE"))
                storePassword = signingProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = signingProps.getProperty("KEY_ALIAS")
                keyPassword = signingProps.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        checkReleaseBuilds = false
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf(
                "**/libksud.so",
                "**/libksud_standard.so",
                "**/libksud_samsung.so"
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(output.versionName.map { vn -> "DFRoot_${vn}.apk" })
        }
    }
}

dependencies {
}

tasks.register("ensureDualKsudBinaries") {
    description = "Ensures verified dual ksud binaries (standard and samsung) are present in jniLibs/arm64-v8a"
    val jniLibsDir = file("src/main/jniLibs/arm64-v8a")
    val standardDst = file("$jniLibsDir/libksud_standard.so")
    val samsungDst = file("$jniLibsDir/libksud_samsung.so")

    val standardUrl = "https://github.com/tiann/KernelSU/releases/download/v3.3.1/ksud-aarch64-linux-android"
    val standardExpectedSha = "049491e2b4cc0a6c913d3920cb90b8c94c9e8ca96c0ee92818293304973bfb1b"

    val samsungUrl = "https://github.com/diabl0w/KernelSU/releases/download/samsung-v2.2/ksud-aarch64-linux-android"
    val samsungExpectedSha = "3934878b5d01ab9f1dfdadbbb67505c7524090de1574719308211b26f0988f72"

    doLast {
        jniLibsDir.mkdirs()
        fun checkSha256(f: File, expected: String): Boolean {
            if (!f.exists() || f.length() == 0L) return false
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = f.readBytes()
            val digest = md.digest(bytes)
            val hex = digest.joinToString("") { b -> "%02x".format(b) }
            return hex.equals(expected, ignoreCase = true)
        }

        fun downloadAndVerify(urlStr: String, dst: File, expectedSha: String, name: String) {
            if (checkSha256(dst, expectedSha)) {
                println("[ensureDualKsudBinaries] $name present and SHA-256 verified.")
                return
            }
            println("[ensureDualKsudBinaries] Downloading $name from $urlStr...")
            val url = URI(urlStr).toURL()
            val conn = url.openConnection()
            conn.getInputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!checkSha256(dst, expectedSha)) {
                dst.delete()
                throw GradleException("SHA-256 mismatch for downloaded $name! Expected $expectedSha")
            }
            println("[ensureDualKsudBinaries] $name downloaded and verified successfully.")
        }

        downloadAndVerify(standardUrl, standardDst, standardExpectedSha, "Standard KernelSU v3.3.1 ksud")
        downloadAndVerify(samsungUrl, samsungDst, samsungExpectedSha, "Samsung KernelSU v2.2 ksud")
    }
}

tasks.named("preBuild").configure {
    dependsOn("ensureDualKsudBinaries")
}
