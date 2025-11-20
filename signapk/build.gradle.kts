plugins {
    application
    alias(libs.plugins.shadow)
}

repositories {
    google()
    mavenCentral()
}

// Custom configuration to handle AAR extraction
configurations {
    create("conscryptAar") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

// Task to extract both classes.jar and native libraries from AAR
val extractConscryptAar by tasks.registering {
    val aarConfig = configurations["conscryptAar"]
    val outputDir = layout.buildDirectory.dir("conscrypt-extracted").get().asFile
    
    inputs.files(aarConfig)
    outputs.dir(outputDir)
    
    doLast {
        aarConfig.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            if (artifact.file.name.endsWith(".aar")) {
                outputDir.mkdirs()
                
                // Extract classes.jar and jni folder from AAR
                project.copy {
                    from(zipTree(artifact.file))
                    include("classes.jar", "jni/**/*.so")
                    into(outputDir)
                }
            }
        }
    }
}

dependencies {
    implementation(libs.apksig)
    implementation(libs.bcprov)
    implementation(libs.bcpkix)
    
    // Add conscrypt-android to the custom configuration
    "conscryptAar"(libs.conscrypt)
    
    // Add extracted classes.jar as implementation dependency
    implementation(files(layout.buildDirectory.dir("conscrypt-extracted/classes.jar")) {
        builtBy(extractConscryptAar)
    })
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.android.signapk.SignApk")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        addMultiReleaseAttribute = false
        
        // Include native libraries from the extracted AAR
        from(layout.buildDirectory.dir("conscrypt-extracted/jni")) {
            into("jni")
        }
        
        dependsOn(extractConscryptAar)
    }
    
    named<Test>("test") {
        useJUnitPlatform()
    }
}
