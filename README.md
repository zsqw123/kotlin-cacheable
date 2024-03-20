# MetaReflect

Fast and lite Kotlin reflect tool built by
[kotlinx-metadata](https://github.com/JetBrains/kotlin/blob/master/libraries/kotlinx-metadata/jvm/ReadMe.md).

## Usages

Here is an example shows getting sealed subclasses through `MetaReflect`.

```kotlin
sealed interface SealedParent
class ChildA : SealedParent
class ChildB : SealedParent

fun main() {
    // preload is recommended, **NOT REQUIRED**
    // only needed call once in whole application lifecycle.
    MReflect.preload()
    
    val reflect = MReflect.get()
    reflect.mClass<SealedParent>().sealedSubclasses // [class ChildA, class ChildB]
}
```

**Note: Support only Kotlin classes, Not support Java classes currently!**

## Dependencies

Latest version:
[![Maven Central](https://img.shields.io/maven-central/v/host.bytedance/meta-reflect)](https://central.sonatype.com/artifact/host.bytedance/meta-reflect)

```kotlin
dependencies {
    implementation("host.bytedance:meta-reflect:<latest>")
}
```

Recommended kotlin version as follows:

| Recommend | Possible Compatible Version Range | meta-reflect   |
|-----------|-----------------------------------|----------------|
| 1.9.22    | <= 2.0.X                          | \>= 0.0.1-beta |

## Benchmark

- Meta Reflect: 0.0.2-beta, Kotlin Reflect: 1.9.22
- Test codes: [MrBenchmark.kt](demo/src/main/kotlin/zsu/meta/reflect/benchmark/MrBenchmark.kt)
- Time unit: us (microsecond), lower is better.

| Benchmark Type              | Fully Resolve |  Only Names |
|-----------------------------|--------------:|------------:|
| Kotlin Reflect (warmup)     |   182,641.966 | 178,144,197 |
| Kotlin Reflect (first time) |   115,888.439 |  30,819.065 |
| Meta Reflect (warmup)       |    79,842.884 |  79,617.180 |
| Meta Reflect (first time)   |    66,865.890 |   5,193.286 |

1. Only Names: Meta Reflect supports resolve class names without resolve it as a full `MClass`.
   e.g. `MClass.sealedSubclassNames` will only return sealed subclasses names, but not resolve them as a `MClass`.
2. Fully Resolve: e.g. resolve sealed subclasses as `MClass` rather than only names.
3. Reflection operation will be fast when access it at the second time, so we only calculate the first time cost. 

In general, Meta Reflect has better performance(~1.73x) than Kotlin Reflect, and will be faster(~5.95x) if not resolve 

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
