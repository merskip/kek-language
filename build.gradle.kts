
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.21"
}

group = "pl.merskip"
version = "0.1"

apply(plugin = "kotlin")
apply(from = "grammar-generator.gradle.kts")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
    implementation(group = "org.bytedeco", name = "llvm-platform", version = "10.0.1-1.5.4")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.6.0")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "12"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "12"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {

    manifest {
        attributes["Main-Class"] = "pl.merskip.keklang.MainKt"
    }

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter {
            // Skip other than linux-x86_64
            if (it.name.endsWith("linux-x86.jar")
                || it.name.endsWith("linux-linux-x86_64.jar")
                || it.name.endsWith("linux-armhf.jar")
                || it.name.endsWith("linux-arm64.jar")
                || it.name.endsWith("linux-ppc64le.jar")
                || it.name.endsWith("windows-x86.jar")
                || it.name.endsWith("windows-x86_64.jar")
                || it.name.endsWith("android-arm.jar")
                || it.name.endsWith("android-arm64.jar")
                || it.name.endsWith("android-x86.jar")
                || it.name.endsWith("android-x86_64.jar")
                || it.name.endsWith("ios-arm64.jar")
                || it.name.endsWith("ios-x86_64.jar")
                || it.name.endsWith("linux-armhf.jar")
                || it.name.endsWith("linux-arm64.jar")
                || it.name.endsWith("linux-ppc64le.jar")
                || it.name.endsWith("linux-x86.jar")
            ) false
            else it.name.endsWith("jar")
        }.map { zipTree(it) }
    })

    archiveFileName.set("kek-macos-x86_64.jar")
}