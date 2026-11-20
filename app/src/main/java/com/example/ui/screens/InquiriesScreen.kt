package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AgentInquiry
import com.example.data.model.ChatThread
import com.example.data.model.InquiryStatus
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiriesScreen(
    chatThreadsState: UiState<List<ChatThread>>,
    inquiriesState: UiState<List<AgentInquiry>>,
    currentUserId: String,
    isBusy: Boolean,
    onOpenChat: (String) -> Unit,
    onOpenProperty: (String) -> Unit,
    onReplyInquiry: (inquiryId: String, replyText: String, status: InquiryStatus, onDone: () -> Unit) -> Unit,
    onCloseInquiry: (inquiryId: String, onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeInquiry by remember { mutableStateOf<AgentInquiry?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
        ) {
            val tabLabels = listOf("ជជែកផ្ទាល់", "សំណើរបស់ខ្ញុំ", "អតិថិជនសួរ")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                tabLabels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index, tabLabels.size),
                        modifier = Modifier.testTag("inquiry_tab_$index"),
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            if (selectedTab == 0) {
                when (chatThreadsState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(text = chatThreadsState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is UiState.Success -> {
                        val threads = chatThreadsState.data
                        if (threads.isEmpty()) {
                            EmptyCommunicationCard(
                                title = "មិនទាន់មានការជជែកផ្ទាល់",
                                subtitle = "ចូលទៅកាន់ប្រកាសអចលនទ្រព្យណាមួយ រួចចុច「ជជែកផ្ទាល់ជាមួយភ្នាក់ងារ」ដើម្បីសួរព័ត៌មាន ឬណាត់មើលផ្ទះ។",
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 96.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(threads, key = { it.id }) { thread ->
                                    ChatThreadCard(
                                        thread = thread,
                                        currentUserId = currentUserId,
                                        onClick = { onOpenChat(thread.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when (inquiriesState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(text = inquiriesState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is UiState.Success -> {
                        val displayed = if (selectedTab == 1) {
                            inquiriesState.data.filter { it.buyerId == currentUserId }
                        } else {
                            inquiriesState.data.filter { it.agentId == currentUserId }
                        }

                        if (displayed.isEmpty()) {
                            EmptyCommunicationCard(
                                title = if (selectedTab == 1) {
                                    "មិនទាន់មានសំណើណាត់មើលផ្ទះ"
                                } else {
                                    "មិនទាន់មានសំណើពីអតិថិជន"
                                },
                                subtitle = if (selectedTab == 1) {
                                    "ចូលមើលផ្ទះណាមួយ រួចចុច「ស្នើសុំមើលផ្ទះ」ដើម្បីផ្ញើព័ត៌មានទៅភ្នាក់ងារ។"
                                } else {
                                    "ពេលអតិថិជនស្នើសុំមើលផ្ទះលើប្រកាសរបស់អ្នក សំណើនឹងបង្ហាញនៅទីនេះ។"
                                },
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 96.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(displayed, key = { it.id }) { inquiry ->
                                    InquiryCard(
                                        inquiry = inquiry,
                                        isAgentPerspective = selectedTab == 2,
                                        onClick = { activeInquiry = inquiry },
                                        onOpenProperty = { onOpenProperty(inquiry.propertyId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeInquiry?.let { inquiry ->
        InquiryDetailDialog(
            inquiry = inquiry,
            isAgent = inquiry.agentId == currentUserId,
            isBuyer = inquiry.buyerId == currentUserId,
            isBusy = isBusy,
            onReply = { replyText, newStatus ->
                onReplyInquiry(inquiry.id, replyText, newStatus) {
                    activeInquiry = null
                }
            },
            onCloseByBuyer = {
                onCloseInquiry(inquiry.id) {
                    activeInquiry = null
                }
            },
            onOpenProperty = {
                activeInquiry = null
                onOpenProperty(inquiry.propertyId)
            },
            onDismiss = { activeInquiry = null },
        )
    }
}

@Composable
private fun EmptyCommunicationCard(
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ChatThreadCard(
    thread: ChatThread,
    currentUserId: String,
    onClick: () -> Unit,
) {
    val otherName = if (thread.buyerId == currentUserId) {
        "ភ្នាក់ងារ៖ ${thread.agentName}"
    } else {
        "អតិថិជន៖ ${thread.buyerName}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_thread_${thread.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Forum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = otherName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = thread.propertyTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = thread.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "ជជែក →",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InquiryCard(
    inquiry: AgentInquiry,
    isAgentPerspective: Boolean,
    onClick: () -> Unit,
    onOpenProperty: () -> Unit,
) {
    val status = inquiry.resolvedStatus()
    val statusColors = when (status) {
        InquiryStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        InquiryStatus.SCHEDULED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        InquiryStatus.REPLIED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        InquiryStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inquiry_card_${inquiry.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = statusColors.first,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = status.labelKm,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColors.second,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = inquiry.resolvedType().labelKm,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = inquiry.propertyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAgentPerspective) {
                        "អតិថិជន៖ ${inquiry.buyerName} (${inquiry.buyerPhone})"
                    } else {
                        "ភ្នាក់ងារ៖ ${inquiry.agentName}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (inquiry.preferredDate.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = inquiry.preferredDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Text(
                text = inquiry.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (inquiry.agentReply.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "ការឆ្លើយតបពីភ្នាក់ងារ៖ ${inquiry.agentReply}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenProperty) {
                    Text("មើលប្រកាសផ្ទះ")
                }
                TextButton(onClick = onClick) {
                    Text(if (isAgentPerspective && status != InquiryStatus.CLOSED) "ឆ្លើយតប / ណាត់ជួប" else "មើលព័ត៌មាន")
                }
            }
        }
    }
}

@Composable
private fun InquiryDetailDialog(
    inquiry: AgentInquiry,
    isAgent: Boolean,
    isBuyer: Boolean,
    isBusy: Boolean,
    onReply: (String, InquiryStatus) -> Unit,
    onCloseByBuyer: () -> Unit,
    onOpenProperty: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentStatus = inquiry.resolvedStatus()
    var replyText by remember { mutableStateOf(inquiry.agentReply) }
    var targetStatus by remember {
        mutableStateOf(
            if (currentStatus == InquiryStatus.PENDING) InquiryStatus.SCHEDULED else currentStatus
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = inquiry.propertyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "អ្នកស្នើ៖ ${inquiry.buyerName} (${inquiry.buyerPhone})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (inquiry.preferredDate.isNotBlank()) {
                    Text(
                        text = "កាលបរិច្ឆេទចង់មើល៖ ${inquiry.preferredDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = inquiry.message,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (isAgent && inquiry.buyerPhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val dialIntent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${inquiry.buyerPhone.trim()}")
                            )
                            context.startActivity(dialIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ហៅទៅអតិថិជន (${inquiry.buyerPhone})")
                    }
                }

                if (isAgent && currentStatus != InquiryStatus.CLOSED) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("សារឆ្លើយតបទៅអតិថិជន") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("agent_reply_input"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            InquiryStatus.SCHEDULED,
                            InquiryStatus.REPLIED,
                            InquiryStatus.CLOSED,
                        ).forEach { st ->
                            FilterChip(
                                selected = targetStatus == st,
                                onClick = { targetStatus = st },
                                label = { Text(st.labelKm) },
                            )
                        }
                    }
                } else if (currentStatus == InquiryStatus.CLOSED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "សំណើនេះត្រូវបានបិទរួចរាល់",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isAgent && currentStatus != InquiryStatus.CLOSED) {
                Button(
                    onClick = { onReply(replyText, targetStatus) },
                    enabled = !isBusy,
                    modifier = Modifier.testTag("confirm_agent_reply_button"),
                ) {
                    Text("រក្សាទុកការឆ្លើយតប")
                }
            } else if (isBuyer && currentStatus != InquiryStatus.CLOSED) {
                OutlinedButton(
                    onClick = onCloseByBuyer,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("close_inquiry_button"),
                ) {
                    Text("បិទសំណើនេះ")
                }
            } else {
                TextButton(onClick = onOpenProperty) {
                    Text("មើលផ្ទះ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("បិទ")
            }
        },
    )
}
