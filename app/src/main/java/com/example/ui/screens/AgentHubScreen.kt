package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddHomeWork
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AgentRatingSummary
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyStatus
import com.example.data.model.UserProfile
import com.example.data.model.formatUsdPrice
import com.example.ui.viewmodel.UiState

@Composable
fun AgentHubScreen(
    userProfile: UserProfile?,
    ratingSummary: AgentRatingSummary,
    myPropertiesState: UiState<List<PropertyListing>>,
    pendingClientLeadsCount: Int,
    isBusy: Boolean,
    onCreateProperty: () -> Unit,
    onEditProperty: (PropertyListing) -> Unit,
    onOpenProperty: (String) -> Unit,
    onOpenMyReviews: () -> Unit,
    onUpdatePropertyStatus: (String, PropertyStatus) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onUpdateProfile: (displayName: String, agentTitle: String, hotline: String, telegram: String, onDone: () -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var propertyToDelete by remember { mutableStateOf<PropertyListing?>(null) }
    var propertyToClose by remember { mutableStateOf<PropertyListing?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile?.displayName?.ifBlank { "ភ្នាក់ងារ ផ្ទះខ្មែរ" } ?: "ភ្នាក់ងារ ផ្ទះខ្មែរ",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = userProfile?.agentTitle?.ifBlank { "ភ្នាក់ងារអចលនទ្រព្យ" } ?: "ភ្នាក់ងារអចលនទ្រព្យ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (!userProfile?.email.isNullOrBlank()) {
                                    Text(
                                        text = userProfile?.email.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("edit_agent_profile_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "កែប្រែព័ត៌មានភ្នាក់ងារ",
                                )
                            }
                        }

                        // Agent Rating Banner
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("agent_hub_reviews_banner")
                                .clickable(onClick = onOpenMyReviews),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (ratingSummary.reviewCount > 0) {
                                            "ពិន្ទុវាយតម្លៃ %.1f / 5.0 (%d មតិ)".format(
                                                ratingSummary.averageRating,
                                                ratingSummary.reviewCount,
                                            )
                                        } else {
                                            "មិនទាន់មានការវាយតម្លៃពីអតិថិជន"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                                Text(
                                    text = "មើលមតិ →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Call,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = userProfile?.businessHotline ?: "012 888 999",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "TG: ${userProfile?.telegramHandle ?: "@khmerhome_agent"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onCreateProperty,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("agent_post_property_button"),
                            ) {
                                Icon(Icons.Filled.AddHomeWork, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.post_property))
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("sign_out_button"),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(id = R.string.sign_out),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                val myList = (myPropertiesState as? UiState.Success)?.data.orEmpty()
                val activeCount = myList.count { it.resolvedStatus() == PropertyStatus.AVAILABLE }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AgentStatCard(
                        value = myList.size.toString(),
                        label = "ប្រកាសសរុប",
                        modifier = Modifier.weight(1f),
                    )
                    AgentStatCard(
                        value = activeCount.toString(),
                        label = "កំពុងលក់/ជួល",
                        modifier = Modifier.weight(1f),
                    )
                    AgentStatCard(
                        value = pendingClientLeadsCount.toString(),
                        label = "សំណើរង់ចាំ",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text(
                    text = "បញ្ជីអចលនទ្រព្យរបស់ខ្ញុំ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            when (myPropertiesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Text(
                            text = myPropertiesState.message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                is UiState.Success -> {
                    val myListings = myPropertiesState.data
                    if (myListings.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "អ្នកមិនទាន់មានប្រកាសលក់/ជួលផ្ទាល់ខ្លួនទេ",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "ចុច「ដាក់លក់ / ជួលថ្មី」ខាងលើ ដើម្បីចាប់ផ្តើមផ្សព្វផ្សាយអចលនទ្រព្យរបស់អ្នក។",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(myListings, key = { it.id }) { listing ->
                            AgentListingManagementCard(
                                listing = listing,
                                isBusy = isBusy,
                                onOpen = { onOpenProperty(listing.id) },
                                onEdit = { onEditProperty(listing) },
                                onToggleReserve = {
                                    val nextStatus = if (listing.resolvedStatus() == PropertyStatus.AVAILABLE) {
                                        PropertyStatus.RESERVED
                                    } else {
                                        PropertyStatus.AVAILABLE
                                    }
                                    onUpdatePropertyStatus(listing.id, nextStatus)
                                },
                                onRequestClose = { propertyToClose = listing },
                                onRequestDelete = { propertyToDelete = listing },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditAgentProfileDialog(
            userProfile = userProfile,
            isBusy = isBusy,
            onSave = { name, title, hotline, tg ->
                onUpdateProfile(name, title, hotline, tg) {
                    showEditProfileDialog = false
                }
            },
            onDismiss = { showEditProfileDialog = false },
        )
    }

    propertyToDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { propertyToDelete = null },
            title = { Text("លុបប្រកាសអចលនទ្រព្យ?") },
            text = { Text("តើអ្នកពិតជាចង់លុប「${listing.title}」ចេញពីបញ្ជីមែនទេ?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProperty(listing.id)
                        propertyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("លុប")
                }
            },
            dismissButton = {
                TextButton(onClick = { propertyToDelete = null }) {
                    Text("បោះបង់")
                }
            },
        )
    }

    propertyToClose?.let { listing ->
        AlertDialog(
            onDismissRequest = { propertyToClose = null },
            title = { Text("បញ្ជាក់ការលក់/ជួលរួចរាល់?") },
            text = {
                Text("បន្ទាប់ពីបិទប្រកាស「${listing.title}」ជា「បានលក់/ជួលរួច」 អ្នកនឹងមិនអាចកែប្រែស្ថានភាពប្រកាសនេះវិញបានទេ។")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePropertyStatus(listing.id, PropertyStatus.CLOSED)
                        propertyToClose = null
                    },
                ) {
                    Text("បញ្ជាក់បិទប្រកាស")
                }
            },
            dismissButton = {
                TextButton(onClick = { propertyToClose = null }) {
                    Text("បោះបង់")
                }
            },
        )
    }
}

@Composable
private fun AgentStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AgentListingManagementCard(
    listing: PropertyListing,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleReserve: () -> Unit,
    onRequestClose: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val status = listing.resolvedStatus()
    val isClosed = status == PropertyStatus.CLOSED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = listing.resolvedImagePreset().drawableRes),
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(MaterialTheme.shapes.small),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = listing.resolvedListingType().labelKm,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Surface(
                            color = if (isClosed) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = status.labelKm,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatUsdPrice(listing.priceUsd, listing.resolvedListingType()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isClosed) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("កែប្រែ", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onToggleReserve,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (status == PropertyStatus.AVAILABLE) "ដាក់បានកក់" else "បើកលក់វិញ",
                            maxLines = 1,
                        )
                    }
                    TextButton(
                        onClick = onRequestClose,
                        enabled = !isBusy,
                    ) {
                        Text("បិទលក់/ជួល")
                    }
                } else {
                    Text(
                        text = "ប្រកាសត្រូវបានបិទជាស្ថាពរ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                IconButton(
                    onClick = onRequestDelete,
                    enabled = !isBusy,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "លុបប្រកាស",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditAgentProfileDialog(
    userProfile: UserProfile?,
    isBusy: Boolean,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by remember { mutableStateOf(userProfile?.displayName.orEmpty()) }
    var agentTitle by remember { mutableStateOf(userProfile?.agentTitle ?: "ភ្នាក់ងារអចលនទ្រព្យ") }
    var hotline by remember { mutableStateOf(userProfile?.businessHotline ?: "012 888 999") }
    var telegram by remember { mutableStateOf(userProfile?.telegramHandle ?: "@khmerhome_agent") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("កែប្រែព័ត៌មានភ្នាក់ងារ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("ឈ្មោះភ្នាក់ងារ *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = agentTitle,
                    onValueChange = { agentTitle = it },
                    label = { Text("តួនាទី / ជំនាញ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hotline,
                    onValueChange = { hotline = it },
                    label = { Text("លេខទូរសព្ទទំនាក់ទំនង") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = telegram,
                    onValueChange = { telegram = it },
                    label = { Text("Telegram (@username)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.isNotBlank()) {
                        onSave(displayName, agentTitle, hotline, telegram)
                    }
                },
                enabled = !isBusy && displayName.isNotBlank(),
            ) {
                Text("រក្សាទុក")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("បោះបង់")
            }
        },
    )
}
