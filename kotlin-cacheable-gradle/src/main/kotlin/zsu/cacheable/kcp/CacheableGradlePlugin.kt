package zsu.cacheable.kcp

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused")
class CacheableGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = "zsu.kotlin-cacheable-kcp"

    private val artifact = SubpluginArtifact("host.bytedance", "kotlin-cacheable-kcp")
    override fun getPluginArtifact(): SubpluginArtifact = artifact

    // sadly, support jvm only currently.
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> true
            else -> false
        }
}