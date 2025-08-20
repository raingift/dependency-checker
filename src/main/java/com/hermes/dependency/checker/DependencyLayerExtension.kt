package com.hermes.dependency.checker

open class DependencyLayerExtension {

    /**
     * layers 层级信息（peers layers 层级信息，按照一定的顺序，正向依赖的case）
     */
    var moduleLayers: MutableList<List<String>> = mutableListOf()

    /**
     * 模块信息
     */
    var modulesGroup: MutableMap<String, Set<String>> = mutableMapOf()

    /**
     * crossLayer 中间分割层 逻辑上分割的module的信息
     */
    var crossLogicLayerModules: MutableMap<String, String>? = null

    /**
     * 层级禁用关系, 某个层级 强制指定不能依赖层级，不涉及直接依赖的case
     */
    var layersForbidConfig: MutableMap<String, List<String>>? = null

    /**
     * enable debug log info
     */
    var enableDebug: Boolean? = false

}