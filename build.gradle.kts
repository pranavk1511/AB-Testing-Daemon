plugins {
  id("java")
  application
}

group = "com.abdaemon"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("app/src/main/java"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("app/src/test/java"))
        }
    }
}


tasks.withType<JavaCompile>().configureEach {
  // Reproducible, strict builds
  options.encoding = "UTF-8"
  options.release.set(21)
}

application {
    mainClass.set("com.abdaemon.infrastructure.Main")
}

repositories {
  mavenCentral()
}

dependencies {
    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    // Logging (SLF4J API + Logback backend)
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.7")
    implementation("ch.qos.logback:logback-core:1.5.7")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

// New testing API: testing suites (instead of configuring Test task directly)
testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
      targets { all { testTask.configure { shouldRunAfter("check") } } }
    }
  }
}

// Friendly defaults for reproducible jars
tasks.jar {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}
