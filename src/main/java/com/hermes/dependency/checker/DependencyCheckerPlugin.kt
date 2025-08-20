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
                    subProject.addTask(getLayerExtension(project))
                }
            }
        } else {
            project.afterEvaluate {
                project.addTask(getLayerExtension(project))
            }
        }
    }

    private fun getLayerExtension(project: Project): DependencyLayerExtension? {
        return project.extensions.findByType(DependencyLayerExtension::class.java)
    }

    private fun Project.addTask(extension: DependencyLayerExtension?) {
        tasks.register("analyzeDependenciesChecker", DependencyCheckTask::class.java) {
            it.description = "Run Dependency Check"
            it.group = GROUP_VERIFICATION
            it.moduleLayers = extension?.moduleLayers ?: mutableListOf()
            it.modulesGroup = extension?.modulesGroup ?: mutableMapOf()
            it.crossLogicLayerModules = extension?.crossLogicLayerModules
            it.layersForbidConfig = extension?.layersForbidConfig
            it.enableDebug = extension?.enableDebug ?: false
        }
        tasks.register("cleanDependenciesCheckerReport") {
            it.description = "Clean Dependency Report"
            it.group = GROUP_VERIFICATION

            it.doLast {
                val reportFiles =
                    rootProject.layout.buildDirectory.file("outputs/reports/").get().asFile
                if (reportFiles.exists()) {
                    reportFiles.deleteRecursively()
                }
            }
        }
    }

    private fun printProjectInfo(project: Project) {
        println("-------- DependencyCheckerPlugin Current environment --------")
        println("Gradle Version ${project.gradle.gradleVersion}")
        println("JDK Version ${System.getProperty("java.version")}")
    }
}