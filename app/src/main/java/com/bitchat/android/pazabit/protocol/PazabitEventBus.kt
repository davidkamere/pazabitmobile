package com.bitchat.android.pazabit.protocol

import com.bitchat.android.pazabit.domain.SafetyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ReceivedSafetyEvent(val event: SafetyEvent, val sourcePeerId: String)

object PazabitEventBus {
    private val mutableEvents = MutableSharedFlow<ReceivedSafetyEvent>(extraBufferCapacity = 64)
    val events = mutableEvents.asSharedFlow()

    fun publish(payload: ByteArray, sourcePeerId: String): Boolean {
        val event = PazabitEventCodec.decode(payload) ?: return false
        return mutableEvents.tryEmit(ReceivedSafetyEvent(event, sourcePeerId))
    }
}
