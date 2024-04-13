package zsu.cacheable.kcp

import host.bytedance.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused")
class CacheableGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        super.apply(target)
        val runtimeArtifact = "${BuildConfig.GROUP}:${BuildConfig.RUNTIME_NAME}:${BuildConfig.VERSION}"
        target.dependencies.add("implementation", runtimeArtifact)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = "zsu.kotlin-cacheable-kcp"

    private val artifact = SubpluginArtifact(
        BuildConfig.GROUP, BuildConfig.KCP_NAME, BuildConfig.VERSION,
    )

    override fun getPluginArtifact(): SubpluginArtifact = artifact

    // sadly, support jvm only currently.
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> true
            else -> false
        }
}