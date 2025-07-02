package com.hermes.dependency.checker

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import kotlin.collections.component1
import kotlin.collections.mutableListOf

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
    var crossLogicLayerModule: String? = null

    private lateinit var sortLayersList: List<String>
    private var crossLogicLayer: String? = null

    @TaskAction
    fun check() {
        val violations = mutableListOf<String>()
        initConfig()
        project.configurations.forEach { config ->
            val currentModule = project.displayName
            if (config.isCanBeResolved) {
                config.dependencies.forEach { dependency ->
                    if (dependency !is ProjectDependency) {
                        println("dependency is not project dependency, can ignore!!!")
                    }
                    val currentLayer = resolveLayer(currentModule)
                    val depModule = dependency.name
                    val depLayer = resolveLayer(depModule)

                    println("depModule: $depModule")
                    if (isCrossLayerViolation(currentLayer, depLayer)) {
                        violations.add("⛔ $currentModule ($currentLayer) → $depModule ($depLayer)")
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

    fun generateReport(violations: MutableList<String>) {
        val reportFile = project.layout.buildDirectory.file("reports/dependency.html").get().asFile
        reportFile.writeText(
            """
        <!DOCTYPE html>
        <html><body>
            ${violations.joinToString("<br>")}
        </body></html>
    """
        )
    }


    fun resolveLayer(module: String): String? {
        return layerModules.entries.find { (_, modules) ->
            modules.contains(module)
        }?.key
    }

    private fun isCrossLayerViolation(currentLayer: String?, depLayer: String?): Boolean {
        println("currentLayer: $currentLayer ; depLayer: $depLayer ; crossLogicLayer: $crossLogicLayer")
        val currentIndex = sortLayersList.indexOf(currentLayer)
        val depLayerIndex = sortLayersList.indexOf(depLayer)
        if (crossLogicLayer == null) {
            return currentIndex >= depLayerIndex
        }
        val crossLayerIndex = sortLayersList.indexOf(crossLogicLayer)
        return if (currentIndex > crossLayerIndex && depLayerIndex < crossLayerIndex) {
            false
        } else {
            currentIndex >= depLayerIndex
        }
    }
}