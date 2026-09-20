package com.bitchat.android.pazabit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.android.mesh.MeshService
import com.bitchat.android.pazabit.data.SafetyRepository
import com.bitchat.android.pazabit.domain.SafetyFlag
import com.bitchat.android.pazabit.domain.VoteDirection

class PazabitViewModel(application: Application, mesh: MeshService) : AndroidViewModel(application) {
    private val repository = SafetyRepository(mesh, viewModelScope)
    val reports = repository.reports
    val groups = repository.groups

    fun sendReport(body: String, flag: SafetyFlag, groups: List<String>) = repository.publishReport(body, flag, groups)
    fun vote(reportId: String, direction: VoteDirection) = repository.vote(reportId, direction)
    fun escalate(reportId: String, groups: List<String>) = repository.escalate(reportId, groups)
    fun createGroup(tag: String) = repository.createGroup(tag)
}
