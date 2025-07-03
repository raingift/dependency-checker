package com.hermes.dependency.checker

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import kotlin.collections.component1

/**
 * 1、解析 layers：按照一定的层级规则排序 【L1 -> L2 -> L3 -> L4 -> L5 -> app】
 * 2、解析 layerModules： 每个层级依赖的配置项 【L1:(...), L2:(...), .... 】
 * 3、解析 crossLayerModule： 交叉依赖关注的层级，必须经过该层级进行依赖的配置;; 这里配置需要强制通过一个层级
 */
abstract class DependencyCheckTask : DefaultTask() {
    @get:Input
    lateinit var sortLayers: MutableList<String>

    @get:Input
    lateinit var layerModules: MutableMap<String, Set<String>>

    @get:Input
    @Optional
    var crossLogicLayerModule: String? = null

    private lateinit var sortLayersList: List<String>
    private var crossLogicLayer: String? = null

    @TaskAction
    fun check() {
        val violations = mutableSetOf<String>()
        initConfig()
        project.configurations.forEach { config ->
            val currentModule = project.displayName
            if (config.isCanBeResolved) {
                config.dependencies.forEach { dependency ->
                    if (dependency !is ProjectDependency) {
                        return@forEach
                    }
                    val currentLayer = resolveLayer(extractProjectInfo(currentModule))
                    val dependencyProjectDisplayName = dependency.dependencyProject.displayName
                    val dependencyLayer =
                        resolveLayer(extractProjectInfo(dependencyProjectDisplayName))

                    println("currentLayer-> ($currentLayer) | dependencyLayer-> ($dependencyLayer)")

                    if (isCrossLayerViolation(currentLayer, dependencyLayer)) {
                        violations.add(
                            "${extractProjectInfo(currentModule)} ($currentLayer) --> ${
                                extractProjectInfo(
                                    dependencyProjectDisplayName
                                )
                            } ($dependencyLayer)"
                        )
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            generateReport(violations)
        }
    }

    fun initConfig() {
        sortLayersList = sortLayers
        crossLogicLayerModule?.let {
            crossLogicLayer = resolveLayer(it)
        }
    }

    fun generateReport(violations: MutableSet<String>) {
        val reportFile =
            project.layout.buildDirectory.file("outputs/reports/forbid_dependency.html")
                .get().asFile
        if (!reportFile.parentFile.exists()) {
            reportFile.parentFile.mkdirs()
            logger.lifecycle("Created directories: ${reportFile.parent}")
        }

        if (!reportFile.exists()) {
            reportFile.createNewFile()
            logger.lifecycle("Created file: ${reportFile.path}")
        } else {
            logger.lifecycle("File already exists: ${reportFile.path}")
        }

        reportFile.writeText(generateHtml(violations).trimIndent(), Charsets.UTF_8)
    }

    fun resolveLayer(module: String?): String? {
        return layerModules.entries.find { (_, modules) ->
            modules.contains(module)
        }?.key
    }

    private fun isCrossLayerViolation(currentLayer: String?, depLayer: String?): Boolean {
        if (currentLayer == null || depLayer == null) {
            return false
        }
        println("currentLayer($currentLayer) dependencyLayer($depLayer) crossLogicLayer($crossLogicLayer)")
        val currentIndex = sortLayersList.indexOf(currentLayer)
        val dependencyLayerIndex = sortLayersList.indexOf(depLayer)
        if (crossLogicLayer == null) {
            return dependencyLayerIndex > currentIndex
        }
        val crossLayerIndex = sortLayersList.indexOf(crossLogicLayer)
        return if (currentIndex >= crossLayerIndex && dependencyLayerIndex <= crossLayerIndex) {
            false
        } else {
            dependencyLayerIndex > currentIndex
        }
    }
}