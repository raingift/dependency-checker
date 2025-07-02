package com.hermes.dependency.checker

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

const val GROUP_VERIFICATION = "verification"

class DependencyCheckerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "layerRules",
            DependencyLayerExtension::class.java,
            project.objects
        )
        project.afterEvaluate {
            project.plugins.withId("com.android.application") {
                val androidComponents =
                    project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
                printProjectInfo(project, androidComponents)
                androidComponents.onVariants { variant ->
                    project.tasks.register(
                        "analyzeDependencies${variant.name.capitalize()}",
                        DependencyCheckTask::class.java
                    ) {
                        it.description = " Run Dependency Check"
                        it.group = GROUP_VERIFICATION
                        it.sortLayers = extension.layers
                        it.layerModules = extension.layerModules
                        it.crossLogicLayerModule = extension.crossLogicLayerModule
                    }
                }
            }
        }
    }

    private fun printProjectInfo(
        project: Project,
        androidComponents: ApplicationAndroidComponentsExtension,
    ) {
        println("-------- DependencyCheckerPlugin Current environment --------")
        println("Gradle Version ${project.gradle.gradleVersion}")
        println("${androidComponents.pluginVersion}")
        println("JDK Version ${System.getProperty("java.version")}")
    }
}