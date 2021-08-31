plugins {
    java
    application
    id("edu.sc.seis.launch4j") version "2.5.0"
}

group = "org.glavo"
version = "1.1"


val img2baseMainClass = "org.glavo.img2base.Main"

tasks.withType<JavaCompile> {
    options.release.set(8)
}

tasks.jar {
    archiveFileName.set("img2base.jar")
    manifest {
        attributes(
            "Main-Class" to img2baseMainClass
        )
    }
}

application {
    mainClass.set(img2baseMainClass)
}

launch4j {
    mainClassName = img2baseMainClass
    outputDir = "libs"
    jreMinVersion = "1.8"
    bundledJrePath = "%JAVA_HOME%"
    bundledJreAsFallback = true
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}