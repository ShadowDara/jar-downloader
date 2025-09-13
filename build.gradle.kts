plugins {
    id("java")
}

group = "de.shadowdara.jardownloader"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "de.shadowdara.jardownloader.Main"
        }

        // nur dein Code rein, keine dependencies
        from(sourceSets.main.get().output)

        // optional: verhindern, dass ungewollt was reingepackt wird
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}
