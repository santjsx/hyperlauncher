package com.hyprlauncher.core.search

import com.hyprlauncher.data.database.entity.AppEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

data class SearchMatchResult(
    val app: AppEntity,
    val score: Int,
    val matchType: MatchType
)

enum class MatchType {
    EXACT,
    PREFIX,
    WORD_PREFIX,
    ACRONYM,
    SUBSEQUENCE,
    FUZZY,
    PACKAGE_NAME,
    NONE
}

interface AppSearchEngine {
    fun rankApps(query: String, apps: List<AppEntity>): List<SearchMatchResult>
}

@Singleton
class DefaultAppSearchEngine @Inject constructor() : AppSearchEngine {

    override fun rankApps(query: String, apps: List<AppEntity>): List<SearchMatchResult> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) {
            return apps.map { app ->
                SearchMatchResult(
                    app = app,
                    score = calculateDefaultScore(app),
                    matchType = MatchType.NONE
                )
            }.sortedByDescending { it.score }
        }

        val results = mutableListOf<SearchMatchResult>()
        val currentTime = System.currentTimeMillis()

        for (app in apps) {
            val labelLower = app.label.lowercase()
            val packageLower = app.packageName.lowercase()

            val (baseScore, matchType) = when {
                // 1. Exact Match
                labelLower == trimmed -> 1000 to MatchType.EXACT

                // 2. Prefix Match
                labelLower.startsWith(trimmed) -> 800 to MatchType.PREFIX

                // 3. Word Prefix Match (e.g. "play" matches "Google Play Store")
                isWordPrefix(labelLower, trimmed) -> 700 to MatchType.WORD_PREFIX

                // 4. Acronym Match (e.g. "yt" matches "YouTube", "gpm" matches "Google Play Music")
                isAcronymMatch(app.label, trimmed) -> 600 to MatchType.ACRONYM

                // 5. Subsequence Match (e.g. "fxf" matches "firefox")
                isSubsequenceMatch(labelLower, trimmed) -> {
                    val gapPenalty = min(100, (labelLower.length - trimmed.length) * 5)
                    (500 - gapPenalty) to MatchType.SUBSEQUENCE
                }

                // 6. Fuzzy Match (Levenshtein distance <= 2 for queries of length >= 3)
                trimmed.length >= 3 && isFuzzyMatch(labelLower, trimmed) -> 350 to MatchType.FUZZY

                // 7. Package Name Match
                packageLower.contains(trimmed) -> 200 to MatchType.PACKAGE_NAME

                else -> 0 to MatchType.NONE
            }

            if (baseScore > 0) {
                // Apply Deterministic Boosts (PRD Section 15)
                val usageBoost = min(app.launchCount * 2, 50)
                val favoriteBoost = if (app.isFavorite) 40 else 0
                val recencyBoost = calculateRecencyBoost(app.lastUsedTimestamp, currentTime)

                val finalScore = baseScore + usageBoost + favoriteBoost + recencyBoost

                results.add(
                    SearchMatchResult(
                        app = app,
                        score = finalScore,
                        matchType = matchType
                    )
                )
            }
        }

        return results.sortedWith(
            compareByDescending<SearchMatchResult> { it.score }
                .thenBy { it.app.label.lowercase() }
        )
    }

    private fun calculateDefaultScore(app: AppEntity): Int {
        var score = 0
        if (app.isFavorite) score += 500
        score += min(app.launchCount * 2, 200)
        return score
    }

    private fun isWordPrefix(label: String, query: String): Boolean {
        val words = label.split(" ", "-", "_", ".")
        return words.any { it.startsWith(query) }
    }

    private fun isAcronymMatch(label: String, query: String): Boolean {
        // Collect capitalized initials or word boundaries
        val words = label.split(" ", "-", "_", ".")
        val initials = if (words.size > 1) {
            words.mapNotNull { it.firstOrNull() }.joinToString("").lowercase()
        } else {
            // Check camelCase initials (e.g. YouTube -> Y, T)
            label.filter { it.isUpperCase() }.lowercase()
        }

        return initials.isNotEmpty() && (initials == query || initials.startsWith(query))
    }

    private fun isSubsequenceMatch(label: String, query: String): Boolean {
        var queryIdx = 0
        var labelIdx = 0
        while (queryIdx < query.length && labelIdx < label.length) {
            if (query[queryIdx] == label[labelIdx]) {
                queryIdx++
            }
            labelIdx++
        }
        return queryIdx == query.length
    }

    private fun isFuzzyMatch(label: String, query: String): Boolean {
        // Only run edit distance against words or full label
        val words = label.split(" ")
        return words.any { word ->
            val dist = levenshteinDistance(word, query)
            dist <= when {
                query.length <= 4 -> 1
                else -> 2
            }
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1, // deletion
                    min(
                        dp[i][j - 1] + 1, // insertion
                        dp[i - 1][j - 1] + cost // substitution
                    )
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    private fun calculateRecencyBoost(lastUsedTimestamp: Long, currentTime: Long): Int {
        if (lastUsedTimestamp <= 0) return 0
        val diffHours = max(0L, (currentTime - lastUsedTimestamp) / (1000 * 60 * 60))
        return when {
            diffHours <= 24 -> 30
            diffHours <= 24 * 7 -> 15
            else -> 0
        }
    }
}
