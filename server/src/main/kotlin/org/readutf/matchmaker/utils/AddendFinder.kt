package org.readutf.matchmaker.utils

object AddendFinder {
    data class BacktrackState(
        val remaining: Int,
        val currentCombination: MutableList<Int>,
        val start: Int,
    )

    fun findUniqueAddends(target: Int): Set<List<Int>> {
        val result = mutableSetOf<List<Int>>() // Store the unique addends as they are found

        // Create a stack to track the steps in the backtrack
        val stack = ArrayDeque<BacktrackState>()
        // Initialise the stack with the starting state
        stack.addFirst(BacktrackState(target, mutableListOf(), 1))
        // Process the stack untill it is empty
        while (stack.isNotEmpty()) {
            // Get the current state from the top of the stack.
            val (remaining, currentCombination, start) = stack.removeFirst()

            // If the remaining value is 0, we've found a valid combination.
            if (remaining == 0) {
                // Add a copy of the current combination to the result set.
                result.add(currentCombination.toList())
                continue
            }

            // If the remaining value is negative, the current combination is invalid.
            if (remaining < 0) {
                continue
            }

            // Iterate through possible addends
            for (i in start..remaining) {
                // Create a new combination by adding the 'i' to the existing combination.
                val newCombination = currentCombination.toMutableList()
                newCombination.add(i)

                // Push a new state onto the stack
                stack.addFirst(BacktrackState(remaining - i, newCombination, i))
            }
        }
        return result
    }
}

fun main() {
    val targetNumber = 4
    val uniqueAddends = AddendFinder.findUniqueAddends(targetNumber)
    println("Unique addends for $targetNumber:")
    uniqueAddends.forEach { println(it) }
}
