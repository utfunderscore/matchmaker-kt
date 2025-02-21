package org.readutf.matchmaker.utils

fun <T> List<T>.partition(n: Int): List<List<T>> {
    val size = this.size
    val partSize = size / n
    val remainingItems = size % n

    return (0 until n).map { i ->
        val start = i * partSize + minOf(i, remainingItems)
        val end = (i + 1) * partSize + minOf(i + 1, remainingItems)
        this.subList(start, end)
    }
}
