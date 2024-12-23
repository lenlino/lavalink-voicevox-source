plugins {
    java
    alias(libs.plugins.lavalink)
}

group = "com.lenlino"
version = "0.1.0"

lavalinkPlugin {
    name = "lavalink-raw-source"
    apiVersion = libs.versions.lavalink.api
    serverVersion = libs.versions.lavalink.server
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}

dependencies {
    // add your dependencies here
}
