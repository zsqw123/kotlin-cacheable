# kotlin-cacheable

## Usage

Cache a function, compute functions only the first time.

```kotlin
class Foo {
    private var bar: Int = 0
    
    @Cacheable
    fun getBar() = ++bar
}

fun main() {
    val foo = Foo()
    println(foo.getBar()) // 1
    println(foo.getBar()) // 1
    println(foo.getBar()) // 1
}
```

## Apply plugin

Recommended version alignments:

| Kotlin | kotlin-cacheable-gradle |
|--------|-------------------------|
| 1.9.22 | \>= 0.0.5-beta          |

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

## More Usages

1. Always cache
    ```kotlin
    var a = 1
    // ignores thread safety(no lock, better performance)
    @Cacheable(CacheMode.NONE)
    fun foo(): Int = ++a
    // with lock
    @Cacheable(CacheMode.SYNCHRONIZED)
    fun bar(): Int = ++a
    
    // test function to call cacheable
    fun test() {
        // always same, compute only once
        foo() // 2
        foo() // 2
        foo() // 2
    }
    ```
2. Cache when input not changed. (default logic)
    ```kotlin
    var a = 0
    
    // [TRACK_ARGS_SYNCHRONIZED] is the default logic
    @Cacheable(cacheMode = CacheMode.TRACK_ARGS_SYNCHRONIZED)
    fun bar(param0: String, param1: Int): Int = ++a
    
    @Cacheable(cacheMode = CacheMode.TRACK_ARGS)
    fun foo(param0: Int, param1: Boolean): Int = ++a
    
    // test function to call cacheable
    // only changed when argument changed (through `equals`)
    fun test() {
        bar("a", 0) // 1
        bar("a", 0) // 1
        bar("b", 1) // 2
        bar("b", 1) // 2
        bar("c", 2) // 3
        bar("c", 2) // 3
    }
    ```

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
