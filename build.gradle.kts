
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
}

group = "pl.merskip"
version = "0.1"

apply(plugin = "kotlin")
apply(from = "grammar-generator.gradle.kts")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.61")
    implementation(group = "org.bytedeco", name = "llvm-platform", version = "10.0.1-1.5.4")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.6.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation(group = "io.arrow-kt", name = "arrow-core", version = "0.11.0")
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
