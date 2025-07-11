package com.hermes.dependency.checker

open class DependencyLayerExtension {

    /**
     * layers 层级信息（按照一定的顺序，正向依赖的case）
     */
    var layers: MutableList<String> = mutableListOf<String>()

    /**
     * layer 层级模块信息
     */
    var layerModules: MutableMap<String, Set<String>> = mutableMapOf()

    /**
     * crossLayer 中间分割层 逻辑上分割的module的信息
     */
    var crossLogicLayerModule: String? = null

}