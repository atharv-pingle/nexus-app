package com.nexus.planner.data.remote

object AgentPrompts {

    val SYSTEM_CHIEF_OF_STAFF = """
        You are NEXUS, an autonomous, highly disciplined, science-backed Executive Chief-of-Staff AI.
        You design and dynamically adapt the user's daily life to eliminate procrastination, align with biological energy peaks, and drive compounding progress on their side-project and physical fitness.

        CORE BEHAVIOR RULES:
        1. Never output generic advice or boilerplate. Always give concrete, actionable time-blocks.
        2. Every single time-block must have a 'microAction': a frictionless 2-minute starter step (Peter Gollwitzer implementation intention) that makes starting effortless.
        3. Respect Ultradian Rhythms (Kleitman/Huberman): deep work blocks are maximum 75-90 minutes, followed by buffer/recovery.
        4. Respect the 3:30 PM Transition Interceptor: prevent post-work exhaustion crashes. Offer a 20-minute power nap or active reset.
        5. The user has dumbbells and a bench at home for strength training. Keep workouts structured and progressive.
        6. Always output valid JSON strictly matching the requested schema.
    """.trimIndent()

    fun buildNextDayPrompt(
        currentDate: String,
        targetDate: String,
        energyLevel: String,
        frictionTags: List<String>,
        priorityGoal: String,
        userProfileJson: String
    ): String {
        return """
            The user completed their day ($currentDate).
            Energy Level logged: $energyLevel
            Frictions experienced: ${frictionTags.joinToString(", ")}
            User's top priority for tomorrow ($targetDate): $priorityGoal
            User Cognitive Profile: $userProfileJson

            Generate the optimized next-day master schedule in JSON format:
            {
              "reasoning": "1 concise sentence explaining the schedule strategy",
              "dateIso": "$targetDate",
              "blocks": [
                {
                  "startTime": "08:00",
                  "endTime": "15:30",
                  "title": "WFH Work Block",
                  "description": "Core job deliverables",
                  "category": "JOB",
                  "durationMinutes": 450,
                  "microAction": "Open IDE and review PR #42"
                },
                ...
              ]
            }
        """.trimIndent()
    }

    fun buildVoiceIntentPrompt(userSpeech: String): String {
        return """
            Analyze the following spoken user command:
            "$userSpeech"

            Determine the user's intent and extract parameters into JSON:
            {
              "action": "RESCHEDULE" | "POWER_NAP" | "LOG_WORKOUT" | "COMPLETE_STEP" | "QUERY",
              "minutesShift": <integer, e.g. 15, 30, 45, or 0>,
              "details": "<concise summary of action>",
              "spokenConfirmation": "<short, calm 1-sentence audio response to read aloud>"
            }
        """.trimIndent()
    }
}
