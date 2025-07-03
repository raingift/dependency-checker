package com.hermes.dependency.checker

fun extractProjectInfo(input: String): String? {
    val parts = input.split('\'')
    return if (parts.size >= 2) parts[1] else input
}

fun generateHtml(violations: MutableSet<String>): String = """
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
        <meta charset="UTF-8">
        <title>模块禁止依赖关系</title>
        <style>
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
            tr:nth-child(even) { background-color: #f9f9f9; }
        </style>
    </head>
    <body>
        <h2>层级模块禁止依赖关系：当前模块 --禁止依赖--> 依赖模块</h2>
        <table>
            <tr>
                <th>当前模块</th>
                <th>禁止依赖的模块</th>
            </tr>
            ${
    violations.joinToString("") { violation ->
        val parts = violation.split("-->")
        val currentModule = parts.getOrNull(0)?.trim() ?: ""
        val forbiddenModule = parts.getOrNull(1)?.trim() ?: ""
        """
                <tr>
                    <td>$currentModule</td>
                    <td>$forbiddenModule</td>
                </tr>
                """
    }
}
        </table>
    </body>
    </html>
    """