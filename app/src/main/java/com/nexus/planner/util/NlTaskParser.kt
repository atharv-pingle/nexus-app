package com.nexus.planner.util

data class ParsedTaskResult(
    val title: String,
    val startTime: String = "10:00",
    val durationMinutes: Int = 45,
    val category: String = "FOCUS",
    val priority: String = "NONE",
    val horizon: String = "TODAY",
    val waitingOn: String? = null,
    val isSequential: Boolean = false,
    val isTomorrow: Boolean = false
)

object NlTaskParser {

    private val timeRegex = Regex("""\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
    private val durRegex = Regex("""\b(\d+)\s*(?:m|min|mins|minutes|h|hour|hours)\b""", RegexOption.IGNORE_CASE)
    private val catRegex = Regex("""#([a-zA-Z0-9_]+)""")
    private val prioRegex = Regex("""!(high|med|medium|low|p1|p2|p3)\b""", RegexOption.IGNORE_CASE)
    private val waitingRegex = Regex("""\bwaiting\s+(?:for|on)\s+(@?[a-zA-Z0-9_\s]+?)(?=\s+(?:at|#|!|for|\z))""", RegexOption.IGNORE_CASE)

    fun parse(input: String, defaultHorizon: String = "TODAY"): ParsedTaskResult {
        var text = input.trim()
        var startTime = "10:00"
        var durationMinutes = 45
        var category = "FOCUS"
        var priority = "NONE"
        var horizon = defaultHorizon
        var waitingOn: String? = null
        var isSequential = false

        // 1. Check Sequential flag (!seq or !step)
        if (text.contains("!seq", ignoreCase = true) || text.contains("!step", ignoreCase = true)) {
            isSequential = true
            text = text.replace("!seq", "", ignoreCase = true).replace("!step", "", ignoreCase = true)
        }

        // 2. Parse Waiting On ("waiting for @Alex", "waiting on John")
        val waitingMatch = waitingRegex.find(text)
        if (waitingMatch != null) {
            waitingOn = waitingMatch.groupValues[1].trim()
            horizon = "WAITING"
            text = text.removeRange(waitingMatch.range).trim()
        }

        // 3. Parse Category (#work, #health, #deepwork)
        val catMatch = catRegex.find(text)
        if (catMatch != null) {
            category = catMatch.groupValues[1].uppercase()
            text = text.removeRange(catMatch.range).trim()
        }

        // 4. Parse Priority (!high, !med, !low, !p1)
        val prioMatch = prioRegex.find(text)
        if (prioMatch != null) {
            val pVal = prioMatch.groupValues[1].uppercase()
            priority = when (pVal) {
                "HIGH", "P1" -> "HIGH"
                "MED", "MEDIUM", "P2" -> "MEDIUM"
                "LOW", "P3" -> "LOW"
                else -> "NONE"
            }
            text = text.removeRange(prioMatch.range).trim()
        }

        // 5. Parse Start Time ("at 4pm", "at 16:30", "at 9am")
        val timeMatch = timeRegex.find(text)
        if (timeMatch != null) {
            val hourStr = timeMatch.groupValues[1]
            val minStr = timeMatch.groupValues[2].ifEmpty { "00" }
            val ampm = timeMatch.groupValues[3].lowercase()

            var hour = hourStr.toIntOrNull() ?: 10
            val min = minStr.toIntOrNull() ?: 0

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            startTime = String.format("%02d:%02d", hour, min)
            text = text.removeRange(timeMatch.range).trim()
        }

        // 6. Parse Duration ("45m", "30 mins", "1h", "2 hours")
        val durMatch = durRegex.find(text)
        if (durMatch != null) {
            val valStr = durMatch.groupValues[1]
            val matchedFull = durMatch.value.lowercase()
            val rawNum = valStr.toIntOrNull() ?: 45
            durationMinutes = if (matchedFull.contains("h")) rawNum * 60 else rawNum
            text = text.removeRange(durMatch.range).trim()
        }

        // 7. Parse Horizon Keywords
        when {
            waitingOn != null -> horizon = "WAITING"
            text.contains("#inbox", ignoreCase = true) || text.contains(" in inbox", ignoreCase = true) -> {
                horizon = "INBOX"
                text = text.replace("#inbox", "", ignoreCase = true).replace(" in inbox", "", ignoreCase = true)
            }
            text.contains("#someday", ignoreCase = true) || text.contains(" in someday", ignoreCase = true) -> {
                horizon = "SOMEDAY"
                text = text.replace("#someday", "", ignoreCase = true).replace(" in someday", "", ignoreCase = true)
            }
            text.contains("tomorrow", ignoreCase = true) -> {
                horizon = "TOMORROW"
                text = text.replace("tomorrow", "", ignoreCase = true)
            }
            text.contains("upcoming", ignoreCase = true) || text.contains("next week", ignoreCase = true) -> {
                horizon = "UPCOMING"
                text = text.replace("upcoming", "", ignoreCase = true).replace("next week", "", ignoreCase = true)
            }
        }

        // Clean extra punctuation and spaces
        val cleanTitle = text.replace(Regex("""\s+"""), " ").trim(' ', ',', '-', ':')
        val finalTitle = if (cleanTitle.isNotBlank()) cleanTitle else input.trim()

        return ParsedTaskResult(
            title = finalTitle,
            startTime = startTime,
            durationMinutes = durationMinutes,
            category = category,
            priority = priority,
            horizon = horizon,
            waitingOn = waitingOn,
            isSequential = isSequential,
            isTomorrow = horizon == "TOMORROW"
        )
    }
}
