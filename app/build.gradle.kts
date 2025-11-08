plugins {
  id("java")
  application
}

group = "com.abdaemon"
version = "0.1.0"

java {
  // New API: use toolchains instead of sourceCompatibility/targetCompatibility
  toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
  withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
  // Reproducible, strict builds
  options.encoding = "UTF-8"
  options.release.set(21)
}

application {
  // We'll add this Main later; for now you can leave a placeholder
  mainClass.set("com.abdaemon.Main")
}

dependencies {
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
