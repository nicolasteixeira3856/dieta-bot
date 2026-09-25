package com.nutri.android.domain

data class StableLog(val window: String, val stable: Boolean)

data class Chip(val window: String, val question: Boolean)

/**
 * Chip appears on the 2nd stable log of the same window.
 * Asked once. Removable.
 * App day 1 shows no chip.
 */
fun chipForWindow(
    appDay: Int,
    currentWindow: String,
    logs: List<StableLog>,
    removed: Set<String>,
    asked: Set<String>,
): Chip? {
    if (appDay <= 1) return null
    val counts = logs.filter { it.stable }.groupingBy { it.window }.eachCount()
    val candidates = counts.filter { (window, n) -> n >= 2 && window !in removed }.keys
    if (candidates.isEmpty()) return null
    val window = if (currentWindow in candidates) currentWindow else candidates.first()
    return Chip(window = window, question = window !in asked)
}
