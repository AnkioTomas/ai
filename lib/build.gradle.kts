plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = findProperty("GROUP")?.toString() ?: "com.github.AnkioTomas.ai"
version = findProperty("VERSION_NAME")?.toString() ?: "unspecified"

android {
    namespace = "net.ankio.ai.lib"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.ankio.theme)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                // 与 webdav 保持一致：对外 artifactId 用仓库名，而不是 module 名。
                artifactId = rootProject.name
                version = project.version.toString()

                pom {
                    name.set(findProperty("POM_NAME")?.toString() ?: rootProject.name)
                    description.set(findProperty("POM_DESCRIPTION")?.toString() ?: "")
                    url.set(findProperty("POM_URL")?.toString() ?: "")

                    licenses {
                        license {
                            name.set(
                                findProperty("POM_LICENCE_NAME")?.toString()
                                    ?: "Apache License, Version 2.0",
                            )
                            url.set(
                                findProperty("POM_LICENCE_URL")?.toString()
                                    ?: "https://www.apache.org/licenses/LICENSE-2.0",
                            )
                        }
                    }

                    developers {
                        developer {
                            id.set(findProperty("POM_DEVELOPER_ID")?.toString() ?: "AnkioTomas")
                            name.set(findProperty("POM_DEVELOPER_NAME")?.toString() ?: "Ankio")
                        }
                    }

                    scm {
                        url.set(findProperty("POM_SCM_URL")?.toString() ?: "")
                        connection.set(findProperty("POM_SCM_CONNECTION")?.toString() ?: "")
                        developerConnection.set(
                            findProperty("POM_SCM_DEV_CONNECTION")?.toString() ?: ""
                        )
                    }
                }
            }
        }
    }
}
