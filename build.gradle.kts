plugins {
    id("java")
}

group = "oop.practical.techdeque"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("net.sourceforge.argparse4j:argparse4j:0.9.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0")) //5.14.2 latest
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}