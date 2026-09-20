package com.bitchat.android.pazabit.protocol

import com.bitchat.android.pazabit.domain.DeliveryPath
import com.bitchat.android.pazabit.domain.SafetyEvent
import com.bitchat.android.pazabit.domain.SafetyFlag
import com.bitchat.android.pazabit.domain.VoteDirection
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Compact, versioned payload carried inside Bitchat's Pazabit extension packet. */
object PazabitEventCodec {
    private const val VERSION = 1
    private const val REPORT = 1
    private const val VOTE = 2
    private const val ESCALATION = 3
    private const val GROUP_CREATED = 4

    fun encode(event: SafetyEvent): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeByte(VERSION)
            when (event) {
                is SafetyEvent.Report -> {
                    output.writeByte(REPORT); writeBase(output, event)
                    output.writeUTF(event.body); output.writeInt(event.flag.ordinal)
                    writeStrings(output, event.groups); output.writeInt(event.deliveryPath?.ordinal ?: -1)
                }
                is SafetyEvent.Vote -> {
                    output.writeByte(VOTE); writeBase(output, event)
                    output.writeUTF(event.reportId); output.writeInt(event.direction.ordinal)
                }
                is SafetyEvent.Escalation -> {
                    output.writeByte(ESCALATION); writeBase(output, event)
                    output.writeUTF(event.reportId); writeStrings(output, event.groups)
                }
                is SafetyEvent.GroupCreated -> {
                    output.writeByte(GROUP_CREATED); writeBase(output, event)
                    output.writeUTF(event.groupId); output.writeUTF(event.tag)
                }
            }
        }
        bytes.toByteArray()
    }

    fun decode(payload: ByteArray): SafetyEvent? = runCatching {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readUnsignedByte() == VERSION)
            when (input.readUnsignedByte()) {
                REPORT -> SafetyEvent.Report(
                    eventId = input.readUTF(), createdAtMillis = input.readLong(), body = input.readUTF(),
                    flag = SafetyFlag.entries[input.readInt()], groups = readStrings(input),
                    deliveryPath = input.readInt().takeIf { it >= 0 }?.let(DeliveryPath.entries::get),
                )
                VOTE -> SafetyEvent.Vote(input.readUTF(), input.readLong(), input.readUTF(), VoteDirection.entries[input.readInt()])
                ESCALATION -> SafetyEvent.Escalation(input.readUTF(), input.readLong(), input.readUTF(), readStrings(input))
                GROUP_CREATED -> SafetyEvent.GroupCreated(input.readUTF(), input.readLong(), input.readUTF(), input.readUTF())
                else -> null
            }
        }
    }.getOrNull()

    private fun writeBase(output: DataOutputStream, event: SafetyEvent) { output.writeUTF(event.eventId); output.writeLong(event.createdAtMillis) }
    private fun writeStrings(output: DataOutputStream, values: List<String>) { output.writeInt(values.size); values.forEach(output::writeUTF) }
    private fun readStrings(input: DataInputStream): List<String> = List(input.readInt()) { input.readUTF() }
}
