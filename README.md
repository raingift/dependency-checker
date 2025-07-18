# dependency-checker

# 依赖以及配置demo 示例如下：
```groovy
apply plugin: "com.hermes.dependency.checker.tools"

def moduleGroups = [:] as Map<String, Set<String>>
new File("${project.rootDir}/modules.txt").eachLine { line ->
    def trimmedLine = line.trim()
    // 跳过空行和注释
    if (trimmedLine.isEmpty() || trimmedLine.startsWith("//")) return

    // 校验模块格式
    if (!trimmedLine.startsWith(":")) {
        throw new IllegalArgumentException("Module must start with ':', but found: $trimmedLine")
    }

    // 提取层级标识（如 ":module_display:launcher_app" -> "module_display"）
    def segments = trimmedLine.split(":")
    def layerKey = segments.find { it.startsWith("module_") } ?: segments.last()

    // 按层级分组
    moduleGroups.computeIfAbsent(layerKey) { new HashSet<String>() }.add(trimmedLine)
}

// 打印分组结果（调试用） 测试的假数据
moduleGroups.each { layer, modules ->
    println "Layer '$layer' ==> "
    modules.each { println "  $it" }
}

layerRules {
    layers = ["module_resource",
              "module_foundation",
              "module_adapter",
              "module_core",
              "module_display",
              "app"]

    layerModules = moduleGroups

    crossLogicLayerModule = ":module_core:node_access_layer"
}
```