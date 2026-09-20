package com.bitchat.android.pazabit.domain

enum class SafetyFlag { NONE, ONGOING, URGENT }
enum class VoteDirection { UP, DOWN }
enum class DeliveryPath { DIRECT, COMMUNITY }

sealed interface SafetyEvent {
    val eventId: String
    val createdAtMillis: Long

    data class Report(
        override val eventId: String,
        override val createdAtMillis: Long,
        val body: String,
        val flag: SafetyFlag,
        val groups: List<String>,
        val deliveryPath: DeliveryPath?,
    ) : SafetyEvent

    data class Vote(
        override val eventId: String,
        override val createdAtMillis: Long,
        val reportId: String,
        val direction: VoteDirection,
    ) : SafetyEvent

    data class Escalation(
        override val eventId: String,
        override val createdAtMillis: Long,
        val reportId: String,
        val groups: List<String>,
    ) : SafetyEvent

    data class GroupCreated(
        override val eventId: String,
        override val createdAtMillis: Long,
        val groupId: String,
        val tag: String,
    ) : SafetyEvent
}

data class SafetyReport(
    val id: String,
    val body: String,
    val flag: SafetyFlag,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val groups: Set<String> = emptySet(),
    val deliveryPath: DeliveryPath? = null,
    val createdAtMillis: Long,
)

data class PrivateGroup(val id: String, val tag: String)
