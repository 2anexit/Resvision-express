package fr.example.rvisionscours

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

object QuizCsvParser {
    fun parse(line: String): QuizQuestion? {
        if (line.isBlank()) {
            return null
        }

        val columns = parseCells(line)
        if (columns.size < 6) {
            return null
        }

        val correctIndex = columns[5].trim().toIntOrNull()?.minus(1) ?: return null
        if (correctIndex !in 0..3) {
            return null
        }

        return QuizQuestion(
            question = columns[0].trim(),
            options = columns.subList(1, 5).map { it.trim() },
            correctIndex = correctIndex
        )
    }

    private fun parseCells(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < line.length) {
            val character = line[index]
            when {
                character == '"' && insideQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                character == '"' -> insideQuotes = !insideQuotes
                character == ',' && !insideQuotes -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(character)
            }
            index++
        }

        cells += current.toString()
        return cells
    }
}
