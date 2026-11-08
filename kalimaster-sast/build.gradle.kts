plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "com.resumeapp.kalimaster"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-io:1.3.2")
    implementation("org.apache.commons:commons-text:1.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named("jar") {
    manifest {
        attributes(
            "Main-Class" to "com.resumeapp.sast.KalimasterSast",
            "Implementation-Version" to project.version,
            "Implementation-Title" to "Kalimaster SAST"
        )
    }
    from(configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) })
}