package com.bitchat.android.pazabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.pazabit.domain.SafetyFlag
import com.bitchat.android.pazabit.domain.SafetyReport
import com.bitchat.android.pazabit.domain.VoteDirection

private val PazabitGreen = Color(0xFF19D939)
private val PazabitBlue = Color(0xFF1779FF)
private val PazabitRed = Color(0xFFFF4D40)
private val PazabitYellow = Color(0xFFFFC247)
private val Rule = Color(0xFF282828)

@Composable
fun PazabitScreen(viewModel: PazabitViewModel) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    var composer by remember { mutableStateOf(TextFieldValue()) }
    var flag by remember { mutableStateOf(SafetyFlag.NONE) }
    var selectedGroups by remember { mutableStateOf(emptyList<String>()) }
    var menuOpen by remember { mutableStateOf(false) }
    var createGroupOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.Black).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).border(width = 1.dp, color = Rule),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("pazabit/", color = PazabitGreen, fontFamily = FontFamily.Monospace, fontSize = 25.sp, modifier = Modifier.padding(start = 22.dp))
            Text("#mesh", color = PazabitBlue, fontFamily = FontFamily.Monospace, fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.Menu, "Open private groups", tint = Color.Gray) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Color(0xFF101010)) {
                    DropdownMenuItem(text = { Text("Public mesh", color = PazabitGreen, fontFamily = FontFamily.Monospace) }, onClick = { menuOpen = false })
                    groups.forEach { group -> DropdownMenuItem(text = { Text(group.tag, color = Color.White, fontFamily = FontFamily.Monospace) }, onClick = { menuOpen = false }) }
                    DropdownMenuItem(text = { Text("+ create private group", color = PazabitGreen, fontFamily = FontFamily.Monospace) }, onClick = { menuOpen = false; createGroupOpen = true })
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            items(reports, key = { it.id }) { report -> ReportRow(report, onVote = { viewModel.vote(report.id, it) }, onEscalate = { viewModel.escalate(report.id, groups.map { group -> group.tag }) }) }
        }
        Column(Modifier.fillMaxWidth().border(width = 1.dp, color = Rule).padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (flag == SafetyFlag.URGENT && groups.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { groups.take(3).forEach { group -> TagButton(group.tag, selectedGroups.contains(group.tag)) { selectedGroups = selectedGroups.toggle(group.tag) } } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = composer, onValueChange = { composer = it }, modifier = Modifier.weight(1f).padding(vertical = 8.dp), textStyle = androidx.compose.ui.text.TextStyle(color = PazabitGreen, fontFamily = FontFamily.Monospace, fontSize = 21.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(PazabitGreen), decorationBox = { inner -> if (composer.text.isEmpty()) Text("type a message...", color = Color(0xFF198F2E), fontFamily = FontFamily.Monospace, fontSize = 21.sp); inner() })
                IconButton(onClick = { if (composer.text.isNotBlank()) { viewModel.sendReport(composer.text, flag, selectedGroups); composer = TextFieldValue(); flag = SafetyFlag.NONE; selectedGroups = emptyList() } }) { Icon(Icons.Default.Send, "Send report", tint = PazabitGreen) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FlagButton("normal", flag == SafetyFlag.NONE, PazabitGreen) { flag = SafetyFlag.NONE; selectedGroups = emptyList() }; FlagButton("ongoing", flag == SafetyFlag.ONGOING, PazabitYellow) { flag = SafetyFlag.ONGOING; selectedGroups = emptyList() }; FlagButton("urgent", flag == SafetyFlag.URGENT, PazabitRed) { flag = SafetyFlag.URGENT } }
        }
    }
    if (createGroupOpen) CreateGroupDialog(onDismiss = { createGroupOpen = false }, onCreate = { tag -> viewModel.createGroup(tag); createGroupOpen = false })
}

@Composable private fun ReportRow(report: SafetyReport, onVote: (VoteDirection) -> Unit, onEscalate: () -> Unit) { Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { if (report.flag != SafetyFlag.NONE) Text(if (report.flag == SafetyFlag.URGENT) "[URGENT]" else "[ONGOING]", color = if (report.flag == SafetyFlag.URGENT) PazabitRed else PazabitYellow, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp); Spacer(Modifier.width(8.dp)); Text("mesh report", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }; Text(report.body, color = Color(0xFFE8E8E8), fontFamily = FontFamily.Monospace, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp)); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 9.dp)) { Vote("↑ ${report.upvotes}", Icons.Default.ArrowUpward) { onVote(VoteDirection.UP) }; Vote("↓ ${report.downvotes}", Icons.Default.ArrowDownward) { onVote(VoteDirection.DOWN) }; if (report.flag == SafetyFlag.URGENT || report.upvotes >= 5) Text("tag", color = PazabitGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp).clickable(onClick = onEscalate)) }; if (report.groups.isNotEmpty()) Text(report.groups.joinToString(" "), color = PazabitBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) } }
@Composable private fun Vote(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 10.dp)) { Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Text(label, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) } }
@Composable private fun FlagButton(label: String, selected: Boolean, color: Color, onClick: () -> Unit) { Text(label, color = if (selected) Color.Black else color, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.background(if (selected) color else Color.Transparent, RoundedCornerShape(3.dp)).border(1.dp, color, RoundedCornerShape(3.dp)).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 3.dp)) }
@Composable private fun TagButton(tag: String, selected: Boolean, onClick: () -> Unit) { Text(tag, color = if (selected) Color.Black else PazabitBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.background(if (selected) PazabitBlue else Color.Transparent, RoundedCornerShape(3.dp)).border(1.dp, PazabitBlue, RoundedCornerShape(3.dp)).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 4.dp)) }
@Composable private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) { var tag by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF111111), title = { Text("create private group", color = PazabitGreen, fontFamily = FontFamily.Monospace) }, text = { BasicTextField(value = tag, onValueChange = { tag = it }, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontFamily = FontFamily.Monospace), decorationBox = { inner -> if (tag.isEmpty()) Text("#medical", color = Color.Gray, fontFamily = FontFamily.Monospace); inner() }) }, confirmButton = { TextButton(onClick = { onCreate(tag) }) { Text("create", color = PazabitGreen, fontFamily = FontFamily.Monospace) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("cancel", color = Color.Gray, fontFamily = FontFamily.Monospace) } }) }
private fun List<String>.toggle(item: String) = if (contains(item)) filterNot { it == item } else this + item
