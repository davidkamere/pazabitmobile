package com.bitchat.android.pazabit.data

import com.bitchat.android.mesh.MeshService
import com.bitchat.android.pazabit.domain.DeliveryPath
import com.bitchat.android.pazabit.domain.PrivateGroup
import com.bitchat.android.pazabit.domain.SafetyEvent
import com.bitchat.android.pazabit.domain.SafetyFlag
import com.bitchat.android.pazabit.domain.SafetyReport
import com.bitchat.android.pazabit.domain.VoteDirection
import com.bitchat.android.pazabit.protocol.PazabitEventBus
import com.bitchat.android.pazabit.protocol.PazabitEventCodec
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SafetyRepository(
    private val mesh: MeshService,
    scope: CoroutineScope,
) {
    private val mutableReports = MutableStateFlow<List<SafetyReport>>(emptyList())
    private val mutableGroups = MutableStateFlow<List<PrivateGroup>>(emptyList())
    val reports: StateFlow<List<SafetyReport>> = mutableReports.asStateFlow()
    val groups: StateFlow<List<PrivateGroup>> = mutableGroups.asStateFlow()

    init { scope.launch { PazabitEventBus.events.collect { apply(it.event) } } }

    fun publishReport(body: String, flag: SafetyFlag, groups: List<String>) {
        require(body.isNotBlank())
        require(flag == SafetyFlag.URGENT || groups.isEmpty())
        send(SafetyEvent.Report(UUID.randomUUID().toString(), System.currentTimeMillis(), body.trim(), flag, groups, if (groups.isEmpty()) null else DeliveryPath.DIRECT))
    }

    fun vote(reportId: String, direction: VoteDirection) = send(
        SafetyEvent.Vote(UUID.randomUUID().toString(), System.currentTimeMillis(), reportId, direction),
    )

    fun escalate(reportId: String, groups: List<String>) {
        require(groups.isNotEmpty())
        send(SafetyEvent.Escalation(UUID.randomUUID().toString(), System.currentTimeMillis(), reportId, groups))
    }

    fun createGroup(tag: String) {
        val normalized = tag.trim().removePrefix("#").replace(" ", "")
        require(normalized.isNotBlank())
        send(SafetyEvent.GroupCreated(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString(), "#$normalized"))
    }

    private fun send(event: SafetyEvent) {
        apply(event)
        mesh.sendPazabitEvent(PazabitEventCodec.encode(event))
    }

    private fun apply(event: SafetyEvent) {
        when (event) {
            is SafetyEvent.Report -> mutableReports.update { reports ->
                if (reports.any { it.id == event.eventId }) reports else listOf(
                    SafetyReport(event.eventId, event.body, event.flag, groups = event.groups.toSet(), deliveryPath = event.deliveryPath, createdAtMillis = event.createdAtMillis),
                ) + reports
            }
            is SafetyEvent.Vote -> mutableReports.update { reports -> reports.map { report ->
                if (report.id != event.reportId) report else report.copy(
                    upvotes = report.upvotes + if (event.direction == VoteDirection.UP) 1 else 0,
                    downvotes = report.downvotes + if (event.direction == VoteDirection.DOWN) 1 else 0,
                )
            } }
            is SafetyEvent.Escalation -> mutableReports.update { reports -> reports.map { report ->
                if (report.id != event.reportId) report else report.copy(groups = report.groups + event.groups, deliveryPath = DeliveryPath.COMMUNITY)
            } }
            is SafetyEvent.GroupCreated -> mutableGroups.update { groups ->
                if (groups.any { it.id == event.groupId }) groups else groups + PrivateGroup(event.groupId, event.tag)
            }
        }
    }
}
