package org.readutf.matchmaker.utils

object AddendFinder {
    data class BacktrackState(
        val remaining: Int,
        val currentCombination: MutableList<Int>,
        val start: Int,
    )

    fun findUniqueAddends(target: Int): Set<List<Int>> {
        // Store the resulting sets of unique addends.
        val result = mutableSetOf<List<Int>>()

        // Use a stack to simulate the recursive calls. Each element in the stack
        // represents a state of the backtracking process.
        val stack = ArrayDeque<BacktrackState>()

        // Initialize the stack with the initial state.
        stack.addFirst(BacktrackState(target, mutableListOf(), 1))

        // Process the stack until it's empty.
        while (stack.isNotEmpty()) {
            // Get the current state from the top of the stack.
            val (remaining, currentCombination, start) =
                stack.removeFirst().let {
                    Triple(it.remaining, it.currentCombination, it.start)
                }

            // If the remaining value is 0, we've found a valid combination.
            if (remaining == 0) {
                // Add a copy of the current combination to the result set.
                result.add(currentCombination.toList())
                continue // Continue to the next state in the stack.
            }

            // If the remaining value is negative, the current combination is invalid.
            if (remaining < 0) {
                continue // Continue to the next state in the stack.
            }

            // Iterate through possible addends, starting from the 'start' value up to
            // the 'remaining' value.
            for (i in start..remaining) {
                // Create a new combination by adding the current addend 'i' to the
                // existing combination.
                val newCombination = currentCombination.toMutableList()
                newCombination.add(i)

                // Push a new state onto the stack, representing the next step in the
                // backtracking process.
                stack.addFirst(BacktrackState(remaining - i, newCombination, i))
            }
        }

        // Return the set of unique addend combinations.
        return result
    }
}

fun main() {
    val targetNumber = 5
    val uniqueAddends = AddendFinder.findUniqueAddends(targetNumber)
    println("Unique addends for $targetNumber:")
    uniqueAddends.forEach { println(it) }
}
