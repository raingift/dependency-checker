package com.hermes.dependency.checker

import org.gradle.api.Plugin
import org.gradle.api.Project

const val GROUP_VERIFICATION = "verification"

class DependencyCheckerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("layerRules", DependencyLayerExtension::class.java)

        printProjectInfo(project)

        val hasSubProjects = project.subprojects.isNotEmpty()

        if (hasSubProjects) {
            project.subprojects { subProject ->
                subProject.afterEvaluate {
                    println(" subProject: " + subProject.displayName)
                    subProject.addTask(getLayerExtension(project))
                }
            }
        } else {
            project.afterEvaluate {
                println(" project: " + project.displayName)
                project.addTask(getLayerExtension(project))
            }
        }
    }

    private fun getLayerExtension(project: Project): DependencyLayerExtension? {
        return project.extensions.findByType(DependencyLayerExtension::class.java)
    }

    fun Project.addTask(extension: DependencyLayerExtension?) {
        tasks.register("analyzeDependenciesChecker", DependencyCheckTask::class.java) {
            it.description = " Run Dependency Check"
            it.group = GROUP_VERIFICATION
            it.sortLayers = extension?.layers ?: mutableListOf<String>()
            it.layerModules = extension?.layerModules?: mutableMapOf()
            it.crossLogicLayerModule = extension?.crossLogicLayerModule
        }
    }

    private fun printProjectInfo(project: Project) {
        println("-------- DependencyCheckerPlugin Current environment --------")
        println("Gradle Version ${project.gradle.gradleVersion}")
        println("JDK Version ${System.getProperty("java.version")}")
    }
}