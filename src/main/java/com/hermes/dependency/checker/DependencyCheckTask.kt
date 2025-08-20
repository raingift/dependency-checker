package com.hermes.dependency.checker

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import kotlin.collections.component1

/**
 * 1、解析 moduleLayers：按照一定的层级规则排序 【L1 -> L2 -> L3 -> L4 -> L5 -> app】
 * 2、解析配置文件 modulesGroup： 每个层级依赖的配置项 【L1:(...), L2:(...), .... 】
 * 3、解析 crossLayerModule： 交叉依赖关注的层级，必须经过该层级进行依赖的配置;; 这里配置需要强制通过一个层级
 */
abstract class DependencyCheckTask : DefaultTask() {
    @get:Input
    lateinit var moduleLayers: MutableList<List<String>>

    @get:Input
    lateinit var modulesGroup: MutableMap<String, Set<String>>

    @get:Input
    @Optional
    var crossLogicLayerModules: MutableMap<String, String>? = null

    @get:Input
    @Optional
    var layersForbidConfig: MutableMap<String, List<String>>? = null

    @get:Input
    @Optional
    var enableDebug: Boolean? = false

    private lateinit var layersRule: Map<String, Int>
    private var crossLogicLayer: String? = null

    @TaskAction
    fun check() {
        val violations = mutableSetOf<String>()
        initConfig()
        project.configurations.forEach { config ->
            val currentModule = project.displayName
            if (config.isCanBeResolved) {
                config.dependencies.forEach dependLabel@{ dependency ->
                    if (dependency !is ProjectDependency) {
                        return@dependLabel
                    }
                    val currentLayer = resolveLayer(extractProjectInfo(currentModule))
                    val dependencyProjectDisplayName = dependency.dependencyProject.displayName
                    val dependencyModule = extractProjectInfo(dependencyProjectDisplayName)
                    val dependencyLayer = resolveLayer(dependencyModule)
                    val crossLogicLayer =
                        crossLogicLayerModules?.get(currentLayer)?.also { resolveLayer(it) }

                    logInfo(
                        "currentLayer-> ($currentLayer) | dependencyLayer-> ($dependencyLayer)"
                                + if (crossLogicLayer != null) " | crossLogicLayer-> ($crossLogicLayer)" else ""
                    )

                    if (isCrossLayerViolation(currentLayer, dependencyLayer, dependencyModule)) {
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

        val relativePath = getGenerateFileName(project.displayName)
        if (violations.isNotEmpty()) {
            generateReport(relativePath, violations)
        }
    }

    private fun initConfig() {
        layersRule = moduleLayers.flatMapIndexed { index, set ->
            set.map { it to index }
        }.toMap()
    }

    /**
     * generate report
     */
    private fun generateReport(relativePath: String, violations: MutableSet<String>) {
        val reportFile =
            project.rootProject.layout.buildDirectory.file("outputs/reports${relativePath}/forbid_dependency.html")
                .get().asFile
        if (!reportFile.parentFile.exists()) {
            reportFile.parentFile.mkdirs()
            logInfo("created directories: ${reportFile.parent}")
        }

        if (!reportFile.exists()) {
            reportFile.createNewFile()
            logInfo("created file: ${reportFile.path}")
        } else {
            logInfo("file already exists: ${reportFile.path}")
        }

        reportFile.writeText(generateHtml(violations).trimIndent(), Charsets.UTF_8)
    }

    private fun resolveLayer(module: String?): String? {
        return modulesGroup.entries.find { (_, modules) ->
            modules.contains(module)
        }?.key
    }

    /**
     * cross layer violation logic
     * @param currentLayer
     * @param depLayer
     * @param dependencyModule
     */
    private fun isCrossLayerViolation(
        currentLayer: String?,
        depLayer: String?,
        dependencyModule: String,
    ): Boolean {
        if (currentLayer == null || depLayer == null) {
            return false
        }
        val isLayersDisabled = isLayersDisabled(currentLayer, dependencyModule)
        if (isLayersDisabled) {
            logInfo("currentLayer($currentLayer) dependencyModule($dependencyModule) isLayersDisabled:$isLayersDisabled")
            return true
        }

        logInfo("currentLayer($currentLayer) dependencyLayer($depLayer) " + if (crossLogicLayer != null) "crossLogicLayer($crossLogicLayer)" else "")
        val triple = findLayerIndexCached(currentLayer, depLayer, crossLogicLayer)
        val currentIndex = triple.first
        val dependencyLayerIndex = triple.second
        val crossLayerIndex = triple.third

        if (crossLogicLayer == null || crossLayerIndex == -1) {
            return dependencyLayerIndex > currentIndex
        }

        return if (crossLayerIndex in dependencyLayerIndex..currentIndex) {
            false
        } else {
            dependencyLayerIndex > currentIndex
        }
    }

    /**
     * find layer in layerRule for index
     */
    private fun findLayerIndexCached(
        currentLayer: String,
        depLayer: String,
        crossLogicLayer: String?,
    ) = Triple(
        layersRule[currentLayer] ?: -1,
        layersRule[depLayer] ?: -1,
        layersRule[crossLogicLayer] ?: -1
    )

    /**
     * disable layers yes or not
     */
    private fun isLayersDisabled(currentLayer: String, dependencyModule: String): Boolean {
        return layersForbidConfig?.get(currentLayer)?.contains(dependencyModule) ?: false
    }

    private fun logInfo(logInfo: String?) {
        if (enableDebug == true) {
            println(logInfo)
        }
    }
}