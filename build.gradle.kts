plugins {
    java
    `kotlin-dsl`
    id("com.github.johnrengelman.shadow") version("7.1.2")
}

group = "me.xemor"
version = "1.3.4"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://repo.minebench.de/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("me.xemor:UserInterface:1.6.6-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("me.clip:placeholderapi:2.11.2")
    shadow("net.kyori:adventure-text-minimessage:4.11.0")
    shadow("net.kyori:adventure-platform-bukkit:4.1.1")
    shadow("mysql:mysql-connector-java:8.0.29")
    shadow("com.zaxxer:HikariCP:5.0.1")
    shadow("org.xerial:sqlite-jdbc:3.36.0.3")
    shadow("me.xemor:configurationdata:2.0.0-SNAPSHOT")
}

java {
    configurations.compile.get().dependencies.remove(dependencies.gradleApi())
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.shadowJar {
    minimize()
    configurations = listOf(project.configurations.shadow.get())
    val folder = System.getenv("pluginFolder")
    destinationDirectory.set(file(folder))
}

tasks.processResources {
    expand(project.properties)
}