# kotlin-cacheable

## Apply plugin

```kotlin
// Using the plugins DSL
plugins {
    id("host.bytedance.kotlin-cacheable") version "<latest>"
}

// or using legacy plugin application
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("host.bytedance:kotlin-cacheable-gradle:<latest>")
    }
}

apply(plugin = "host.bytedance.kotlin-cacheable")
```

The plugin will be published both **gradle plugin portal** and **maven central**.

- maven central: [![Maven Central](https://img.shields.io/maven-central/v/host.bytedance/kotlin-cacheable-gradle)](https://central.sonatype.com/artifact/host.bytedance/kotlin-cacheable-gradle)
- gradle plugin portal: [![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/host.bytedance.kotlin-cacheable)](https://plugins.gradle.org/plugin/host.bytedance.kotlin-cacheable)

## License

```
Copyright 2024 zsqw123

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
