
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
    implementation(group = "org.bytedeco", name = "llvm-platform", version = "10.0.1-1.5.4")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.6.0")
    implementation(group = "com.xenomachina", name = "kotlin-argparser", version = "2.0.7")
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
}

val archs = listOf(
    "linux-x86",
    "linux-x86_64",
    "linux-armhf",
    "linux-arm64",
    "linux-ppc64le",
    "windows-x86",
    "windows-x86_64",
    "macosx-x86_64",
    "android-arm",
    "android-arm64",
    "android-x86",
    "android-x86_64",
    "ios-arm64",
    "ios-x86_64"
)

tasks {

    fun createJarTask(arch: String) {
        register("jar-$arch", Jar::class.java) {
            group = "build jar"
            manifest {
                attributes["Main-Class"] = "pl.merskip.keklang.MainKt"
            }
            from(sourceSets.main.get().output)
            dependsOn(configurations.runtimeClasspath)

            from({
                configurations.runtimeClasspath.get()
                    .filter { file -> file.name.endsWith("jar") }
                    .filter { file ->
                        val containsArch = archs.any { arch ->
                            file.name.endsWith("$arch.jar")
                        }
                        if (containsArch) file.name.endsWith("$arch.jar")
                        else true
                    }.map { zipTree(it) }
            })
            archiveFileName.set("kek-${arch}.jar")
        }
    }

    for (arch in archs) createJarTask(arch)

    create("jar-all") {
        group = "build jar"
        for (arch in archs) dependsOn("jar-$arch")
    }
}
