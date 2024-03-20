package zsu.cacheable.kcp

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class CacheableGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        TODO("Not yet implemented")
    }

    override fun getCompilerPluginId(): String = "zsu.kotlin-cacheable-kcp"

    private val artifact = SubpluginArtifact("host.bytedance", "kotlin-cacheable-kcp")
    override fun getPluginArtifact(): SubpluginArtifact = artifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return true
    }
}