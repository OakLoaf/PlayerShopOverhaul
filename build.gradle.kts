plugins {
    java
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = "me.xemor"
version = "1.3.4"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io/")
    maven("https://repo.minebench.de/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("me.clip:placeholderapi:2.11.2")
    implementation("net.kyori:adventure-text-minimessage:4.11.0")
    implementation("net.kyori:adventure-platform-bukkit:4.1.1")
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("com.github.Xemorr:ConfigurationData:537df1d205")
    implementation("me.xemor:UserInterface:2.0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    shadowJar {
        val folder = System.getenv("pluginFolder")
        if (folder != null) {
            destinationDirectory.set(file(folder))
        }

        archiveFileName.set("${project.name}-${project.version}.jar")
    }

    processResources{
        expand(project.properties)

        inputs.property("version", rootProject.version)
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }
}