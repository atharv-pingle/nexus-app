package com.nexus.planner

import android.app.Application
import com.nexus.planner.data.local.NexusDatabase
import com.nexus.planner.data.remote.OpenRouterClient
import com.nexus.planner.data.repository.NexusRepository
import com.nexus.planner.service.VoiceInteractionManager

class NexusApp : Application() {

    lateinit var database: NexusDatabase
        private set

    lateinit var openRouterClient: OpenRouterClient
        private set

    lateinit var repository: NexusRepository
        private set

    lateinit var voiceManager: VoiceInteractionManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = NexusDatabase.getInstance(this)
        openRouterClient = OpenRouterClient(this)
        repository = NexusRepository(database, openRouterClient)
        voiceManager = VoiceInteractionManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceManager.shutdown()
    }
}
