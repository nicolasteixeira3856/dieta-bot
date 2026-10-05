import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
    // A11 (ADR-014): Firebase for the dev flavor only. prod has no google-services.json and no Firebase deps.
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val local = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { local.load(it) }
}
fun localProp(key: String): String? = local.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

// Flavors (A10, ADR-014). dev: dev.* keys, falling back to the pre-A10 unprefixed keys.
// -PAPI_PUBLIC_URL=... overrides dev only (QA against a local fake server).
val devApiUrl = (project.findProperty("API_PUBLIC_URL") as String?)
    ?: localProp("dev.API_PUBLIC_URL") ?: localProp("API_PUBLIC_URL") ?: "http://127.0.0.1:8080"
val devInvite = localProp("dev.INVITE_CODE") ?: localProp("INVITE_CODE") ?: "troca-isto"
// prod: no fallback. A prod variant does not build without both keys (guard at the end of this file).
val prodApiUrl = localProp("prod.API_PUBLIC_URL")
val prodInvite = localProp("prod.INVITE_CODE")

// Release signing (A9): key.properties + fibrai-release.jks at the repo root, never in git.
// Without key.properties the release is left unsigned; it never falls back to the debug key.
val keyPropsFile = rootProject.file("../../key.properties")
val keyProps = Properties()
if (keyPropsFile.exists()) {
    keyPropsFile.inputStream().use { keyProps.load(it) }
} else {
    logger.warn("key.properties not found at ${keyPropsFile.path}: release APK will be unsigned.")
}

// Version (A16): apps/android/version.properties, bumped only by tools/distribute-dev.ps1.
// versionName = 0.0.N (the dev flavor adds -dev), versionCode = N. Until 1.0.0 (its own plan).
val versionFile = rootProject.file("version.properties")
val versionPatch: Int = run {
    if (!versionFile.exists()) throw GradleException("Missing ${versionFile.path} (VERSION_PATCH=N). See docs/android/README.md, Distribution.")
    val props = Properties().apply { versionFile.inputStream().use { load(it) } }
    val raw = props.getProperty("VERSION_PATCH")?.trim()
    raw?.toIntOrNull()?.takeIf { it >= 1 }
        ?: throw GradleException("VERSION_PATCH in ${versionFile.path} must be a positive integer, got '$raw'.")
}

android {
    namespace = "app.fibrai.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.fibrai.android"
        minSdk = 26
        targetSdk = 36
        versionCode = versionPatch
        versionName = "0.0.$versionPatch"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "ENV", "\"dev\"")
            buildConfigField("String", "API_PUBLIC_URL", "\"$devApiUrl\"")
            buildConfigField("String", "INVITE_CODE", "\"$devInvite\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "ENV", "\"prod\"")
            buildConfigField("String", "API_PUBLIC_URL", "\"${prodApiUrl.orEmpty()}\"")
            buildConfigField("String", "INVITE_CODE", "\"${prodInvite.orEmpty()}\"")
        }
    }

    signingConfigs {
        if (keyPropsFile.exists()) {
            create("release") {
                // storeFile in key.properties is relative to apps/android/ (the Gradle root).
                storeFile = rootProject.file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    // A39: backdrop blur of Aero glass on API 31+ (owner-approved 2026-10-04).
    implementation(libs.haze)
    // @Preview in main sources: the annotation must exist in release too (A9).
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.retrofit.serialization)
    implementation(libs.coroutines)
    implementation(libs.datastore)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // A11: Firebase only in dev. prod gets NoopTelemetry (src/prod) and no Firebase classes.
    "devImplementation"(platform(libs.firebase.bom))
    "devImplementation"(libs.firebase.crashlytics)
    "devImplementation"(libs.firebase.analytics)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp)
    testImplementation(libs.serialization.json)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)
}

kapt {
    correctErrorTypes = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test> {
    testLogging {
        events("passed", "failed")
    }
}

googleServices {
    // google-services.json lives only in src/dev (gitignored). prod and a clean clone build without it.
    missingGoogleServicesStrategy = com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
}

roborazzi {
    // Regression baselines. docs/qa/android/current/ holds emulator screencaps only.
    outputDir.set(file("src/test/snapshots"))
}

// A10: a prod variant never builds against a guessed server. Only prod tasks are blocked.
val prodConfigured = prodApiUrl != null && prodInvite != null
tasks.matching { it.name.startsWith("preProd") && it.name.endsWith("Build") }.configureEach {
    doFirst {
        if (!prodConfigured) {
            throw GradleException(
                "prod flavor: defina prod.API_PUBLIC_URL e prod.INVITE_CODE em apps/android/local.properties",
            )
        }
    }
}

// A39 (ADR-030/031): AeroTokens.kt is generated from docs/design/tokens.json, the mirror of the Figma `Design`
// variables and styles. No hand-written token values in Kotlin. A missing or invalid JSON fails the build.
abstract class GenerateAeroTokens : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tokensJson: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val file = tokensJson.get().asFile
        if (!file.exists()) throw GradleException("Aero tokens: ${file.path} is missing (ADR-031 § 8).")
        val root = try {
            groovy.json.JsonSlurper().parse(file) as Map<*, *>
        } catch (e: Exception) {
            throw GradleException("Aero tokens: ${file.path} is not valid JSON: ${e.message}")
        }
        val out = outputDir.get().asFile.resolve("app/fibrai/android/core/designsystem/aero/AeroTokens.kt")
        out.parentFile.mkdirs()
        out.writeText(AeroTokenWriter(file.path).write(root))
    }
}

class AeroTokenWriter(private val path: String) {
    private fun fail(msg: String): Nothing = throw GradleException("Aero tokens: $msg in $path")

    private fun list(m: Map<*, *>, key: String): List<Map<*, *>> =
        (m[key] as? List<*>)?.map { it as? Map<*, *> ?: fail("'$key' item is not an object") } ?: fail("'$key' missing")

    private fun ident(name: String): String =
        name.split('/', '-', ' ').filter { it.isNotEmpty() }.mapIndexed { i, p ->
            if (i == 0) p.replaceFirstChar { it.lowercase() } else p.replaceFirstChar { it.uppercase() }
        }.joinToString("")

    private fun argb(hex: Any?): String {
        val h = (hex as? String ?: fail("color expected, got '$hex'")).removePrefix("#")
        if (!Regex("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?").matches(h)) fail("bad color '$hex'")
        val a = if (h.length == 8) h.substring(6, 8) else "ff"
        return "Color(0x" + (a + h.substring(0, 6)).uppercase() + ")"
    }

    private fun num(v: Any?): String = (v as? Number)?.toFloat()?.toString()?.plus("f") ?: fail("number expected, got '$v'")

    fun write(root: Map<*, *>): String {
        val collections = list(root, "collections")
        fun collection(name: String) = collections.firstOrNull { it["name"] == name } ?: fail("collection '$name' missing")
        val colorVars = list(collection("Color"), "variables")
        val shapeVars = list(collection("Shape"), "variables")
        val motionVars = list(collection("Motion"), "variables")
        val colorNames = colorVars.map { it["name"] as String }
        fun colorRef(variable: Any?): String {
            val n = variable as? String ?: fail("variable name expected")
            if (n !in colorNames) fail("unknown color variable '$n'")
            return "c.${ident(n)}"
        }
        fun value(v: Map<*, *>, mode: String) = (v["values"] as? Map<*, *>)?.get(mode) ?: fail("$mode value missing for ${v["name"]}")

        val sb = StringBuilder()
        sb.appendLine("// Generated by the generateAeroTokens Gradle task from docs/design/tokens.json. Do not edit.")
        sb.appendLine("package app.fibrai.android.core.designsystem.aero")
        sb.appendLine()
        sb.appendLine("import androidx.compose.animation.core.CubicBezierEasing")
        sb.appendLine("import androidx.compose.runtime.Immutable")
        sb.appendLine("import androidx.compose.ui.graphics.Color")
        sb.appendLine("import androidx.compose.ui.unit.dp")
        sb.appendLine()
        sb.appendLine("/** Figma `Color` collection, one instance per mode. */")
        sb.appendLine("@Immutable")
        sb.appendLine("data class AeroColors(")
        colorVars.forEach { sb.appendLine("    val ${ident(it["name"] as String)}: Color,") }
        sb.appendLine("    val isDark: Boolean,")
        sb.appendLine(")")
        for (mode in listOf("Light", "Dark")) {
            sb.appendLine()
            sb.appendLine("val Aero${mode}Colors = AeroColors(")
            colorVars.forEach { sb.appendLine("    ${ident(it["name"] as String)} = ${argb(value(it, mode))},") }
            sb.appendLine("    isDark = ${mode == "Dark"},")
            sb.appendLine(")")
        }
        sb.appendLine()
        sb.appendLine("/** Figma `Shape` collection (dp). */")
        sb.appendLine("object AeroDimens {")
        shapeVars.forEach { sb.appendLine("    val ${ident(it["name"] as String)} = ${num(value(it, "Value"))}.dp") }
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("/** Figma `Motion` collection: durations in ms and cubic-bézier easings. */")
        sb.appendLine("object AeroMotion {")
        motionVars.forEach {
            val v = value(it, "Value")
            val n = ident((it["name"] as String).substringAfter('/'))
            if (it["type"] == "FLOAT") {
                sb.appendLine("    const val ${n}Ms = ${(v as Number).toInt()}")
            } else {
                val m = Regex("cubic-bezier\\(([^)]*)\\)").find(v.toString()) ?: fail("bad easing '$v'")
                val p = m.groupValues[1].split(',').map { it.trim().toFloat().toString() + "f" }
                if (p.size != 4) fail("bad easing '$v'")
                sb.appendLine("    val easing${n.replaceFirstChar { it.uppercase() }} = CubicBezierEasing(${p.joinToString(", ")})")
            }
        }
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("/** One Figma text style: weight 100..900, size and line height in sp, letter spacing in % of the size, text case. */")
        sb.appendLine("@Immutable")
        sb.appendLine("data class AeroTextToken(val weight: Int, val italic: Boolean, val size: Float, val lineHeight: Float, val letterSpacingPercent: Float, val uppercase: Boolean)")
        sb.appendLine()
        sb.appendLine("object AeroTextTokens {")
        val weights = mapOf("Regular" to 400, "Medium" to 500, "SemiBold" to 600, "Bold" to 700, "ExtraBold" to 800)
        list(root, "textStyles").forEach {
            if (it["family"] != "Nunito Sans") fail("text style ${it["name"]} is not Nunito Sans")
            val style = it["style"] as String
            val italic = style.endsWith("Italic")
            val w = weights[style.removeSuffix("Italic").trim().ifEmpty { "Regular" }] ?: fail("unknown weight '$style'")
            val upper = when (it["textCase"]) {
                null, "ORIGINAL" -> false
                "UPPER" -> true
                else -> fail("unsupported textCase '${it["textCase"]}'")
            }
            sb.appendLine("    val ${ident(it["name"] as String)} = AeroTextToken($w, $italic, ${num(it["size"])}, ${num(it["lineHeight"])}, ${num(it["letterSpacingPercent"])}, $upper)")
        }
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("/** A gradient stop: position 0..1 and its colour. */")
        sb.appendLine("@Immutable")
        sb.appendLine("data class AeroStop(val position: Float, val color: Color)")
        sb.appendLine()
        sb.appendLine("/** Figma paint styles, vertical gradients (top to bottom) resolved against a mode. */")
        sb.appendLine("object AeroPaints {")
        list(root, "paintStyles").forEach { style ->
            val name = ident((style["name"] as String).replace('/', ' '))
            list(style, "paints").filter { it["type"] == "GRADIENT_LINEAR" }.forEach { paint ->
                if (paint["direction"] != "topToBottom") fail("only topToBottom gradients are supported")
                val stops = list(paint, "stops").joinToString(", ") { s ->
                    "AeroStop(${num(s["position"])}, ${if (s["variable"] != null) colorRef(s["variable"]) else argb(s["color"])})"
                }
                sb.appendLine("    fun ${name}Stops(c: AeroColors): List<AeroStop> = listOf($stops)")
            }
        }
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("/** Figma effect styles. A shadow radius is the Figma blur radius (dp). */")
        sb.appendLine("object AeroEffects {")
        list(root, "effectStyles").forEach { style ->
            val name = ident((style["name"] as String).replace('/', ' '))
            list(style, "effects").forEach { e ->
                when (e["type"]) {
                    "BACKGROUND_BLUR" -> sb.appendLine("    val ${name}Blur = ${num(e["radius"])}.dp")
                    "DROP_SHADOW" -> {
                        sb.appendLine("    val ${name}ShadowRadius = ${num(e["radius"])}.dp")
                        sb.appendLine("    val ${name}ShadowOffsetX = ${num(e["offsetX"])}.dp")
                        sb.appendLine("    val ${name}ShadowOffsetY = ${num(e["offsetY"])}.dp")
                        sb.appendLine("    val ${name}ShadowSpread = ${num(e["spread"])}.dp")
                        val color = if (e["variable"] != null) colorRef(e["variable"]) else argb(e["color"])
                        sb.appendLine("    fun ${name}ShadowColor(c: AeroColors): Color = $color")
                    }
                    else -> fail("unsupported effect ${e["type"]}")
                }
            }
        }
        sb.appendLine("}")
        return sb.toString()
    }
}

val generateAeroTokens = tasks.register<GenerateAeroTokens>("generateAeroTokens") {
    tokensJson.set(rootProject.layout.projectDirectory.file("../../docs/design/tokens.json"))
    outputDir.set(layout.buildDirectory.dir("generated/aero/kotlin"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(generateAeroTokens, GenerateAeroTokens::outputDir)
    }
}
