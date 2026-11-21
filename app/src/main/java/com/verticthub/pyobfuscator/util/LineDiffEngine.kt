package com.verticthub.pyobfuscator.util

data class DiffLine(val oldLineNumber: Int?, val newLineNumber: Int?, val type: DiffLineType, val content: String)
enum class DiffLineType { UNCHANGED, ADDED, REMOVED }
data class DiffSummary(val lines: List<DiffLine>, val addedCount: Int, val removedCount: Int, val unchangedCount: Int)

object LineDiffEngine {
    fun computeDiff(original: String, transformed: String, maxLinesPerSide: Int = 400): DiffSummary {
        val a = original.lines().take(maxLinesPerSide)
        val b = transformed.lines().take(maxLinesPerSide)
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val out = mutableListOf<DiffLine>()
        var i = 0; var j = 0; var added = 0; var removed = 0; var unchanged = 0
        while (i < n && j < m) {
            if (a[i] == b[j]) { out.add(DiffLine(i + 1, j + 1, DiffLineType.UNCHANGED, a[i])); unchanged++; i++; j++ }
            else if (dp[i + 1][j] >= dp[i][j + 1]) { out.add(DiffLine(i + 1, null, DiffLineType.REMOVED, a[i])); removed++; i++ }
            else { out.add(DiffLine(null, j + 1, DiffLineType.ADDED, b[j])); added++; j++ }
        }
        while (i < n) { out.add(DiffLine(i + 1, null, DiffLineType.REMOVED, a[i])); removed++; i++ }
        while (j < m) { out.add(DiffLine(null, j + 1, DiffLineType.ADDED, b[j])); added++; j++ }
        return DiffSummary(out, added, removed, unchanged)
    }
}
