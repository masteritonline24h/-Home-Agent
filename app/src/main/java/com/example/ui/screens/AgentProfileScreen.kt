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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AgentReview
import com.example.data.model.PropertyListing
import com.example.data.model.formatUsdPrice
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentProfileScreen(
    agentId: String,
    agentName: String,
    reviewsState: UiState<List<AgentReview>>,
    agentProperties: List<PropertyListing>,
    currentUserId: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSubmitReview: (rating: Int, comment: String, onDone: () -> Unit) -> Unit,
    onDeleteReview: (reviewId: String) -> Unit,
    onOpenProperty: (propertyId: String) -> Unit,
) {
    var showReviewDialog by remember { mutableStateOf(false) }

    val allReviews = (reviewsState as? UiState.Success)?.data.orEmpty()
    val agentReviews = remember(allReviews, agentId) {
        allReviews.filter { it.agentId == agentId }
    }
    val averageRating = remember(agentReviews) {
        if (agentReviews.isEmpty()) 0.0 else agentReviews.sumOf { it.rating }.toDouble() / agentReviews.size
    }
    val resolvedAgentName = remember(agentName, agentProperties, agentReviews) {
        agentName.ifBlank {
            agentProperties.firstOrNull()?.agentName
                ?: agentReviews.firstOrNull()?.agentName
                ?: "ភ្នាក់ងារ ផ្ទះខ្មែរ"
        }
    }
    val resolvedAgency = agentProperties.firstOrNull()?.agentAgency ?: "ផ្ទះខ្មែរ អចលនទ្រព្យ"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ប្រវត្តិ និងការវាយតម្លៃភ្នាក់ងារ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("agent_profile_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ត្រឡប់ក្រោយ",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Agent Header + Rating Breakdown
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = resolvedAgentName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = resolvedAgency,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (agentReviews.isEmpty()) "0.0" else "%.1f".format(averageRating),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        StarRatingRow(
                                            rating = averageRating.toInt().coerceIn(0, 5),
                                            starSize = 16.dp,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${agentReviews.size} មតិវាយតម្លៃ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        (5 downTo 1).forEach { stars ->
                                            val count = agentReviews.count { it.rating.toInt() == stars }
                                            val ratio = if (agentReviews.isEmpty()) 0f else count.toFloat() / agentReviews.size
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = "$stars★",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.width(24.dp),
                                                )
                                                LinearProgressIndicator(
                                                    progress = { ratio },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(7.dp)
                                                        .clip(RoundedCornerShape(50)),
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                )
                                                Text(
                                                    text = count.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.width(20.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showReviewDialog = true },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("write_agent_review_button"),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RateReview,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("សរសេរការវាយតម្លៃភ្នាក់ងារ")
                            }
                        }
                    }
                }

                // Client Reviews Section
                item {
                    Text(
                        text = "មតិវាយតម្លៃពីអតិថិជន (${agentReviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                when (reviewsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            Text(
                                text = reviewsState.message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (agentReviews.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "មិនទាន់មានមតិវាយតម្លៃសម្រាប់ភ្នាក់ងារនេះនៅឡើយទេ។ ចុច「សរសេរការវាយតម្លៃភ្នាក់ងារ」ដើម្បីចែករំលែកបទពិសោធន៍របស់អ្នក!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(18.dp),
                                    )
                                }
                            }
                        } else {
                            items(agentReviews, key = { it.id }) { review ->
                                AgentReviewCard(
                                    review = review,
                                    isOwnReview = review.reviewerId == currentUserId,
                                    isBusy = isBusy,
                                    onDelete = { onDeleteReview(review.id) },
                                )
                            }
                        }
                    }
                }

                // Agent Active Listings
                if (agentProperties.isNotEmpty()) {
                    item {
                        Text(
                            text = "អចលនទ្រព្យរបស់ភ្នាក់ងារនេះ (${agentProperties.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    items(agentProperties, key = { it.id }) { listing ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("agent_profile_property_${listing.id}")
                                .clickable { onOpenProperty(listing.id) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Image(
                                    painter = painterResource(id = listing.resolvedImagePreset().drawableRes),
                                    contentDescription = listing.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(MaterialTheme.shapes.small),
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = listing.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${listing.district}, ${listing.city}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = formatUsdPrice(listing.priceUsd, listing.resolvedListingType()),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        WriteAgentReviewDialog(
            agentName = resolvedAgentName,
            isBusy = isBusy,
            onSubmit = { rating, comment ->
                onSubmitReview(rating, comment) {
                    showReviewDialog = false
                }
            },
            onDismiss = { showReviewDialog = false },
        )
    }
}

@Composable
private fun StarRatingRow(
    rating: Int,
    starSize: androidx.compose.ui.unit.Dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

@Composable
private fun AgentReviewCard(
    review: AgentReview,
    isOwnReview: Boolean,
    isBusy: Boolean,
    onDelete: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateLabel = review.createdAt?.toDate()?.let { dateFormatter.format(it) }.orEmpty()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_review_card_${review.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.reviewerName.ifBlank { "អតិថិជន" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StarRatingRow(
                            rating = review.rating.toInt().coerceIn(1, 5),
                            starSize = 15.dp,
                        )
                        if (dateLabel.isNotBlank()) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (isOwnReview) {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_review_${review.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "លុបការវាយតម្លៃ",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun WriteAgentReviewDialog(
    agentName: String,
    isBusy: Boolean,
    onSubmit: (rating: Int, comment: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedRating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "វាយតម្លៃភ្នាក់ងារ៖ $agentName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ជ្រើសរើសផ្កាយ (${selectedRating}/5)",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("review_star_$star"),
                        ) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$star ផ្កាយ",
                                tint = if (star <= selectedRating) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("មតិយោបល់របស់អ្នក *") },
                    placeholder = { Text("តើភ្នាក់ងារផ្តល់សេវាកម្ម និងព័ត៌មានល្អកម្រិតណា?") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input"),
                )

                if (errorText != null) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.trim().isEmpty()) {
                        errorText = "សូមសរសេរមតិយោបល់មុននឹងផ្ញើ"
                    } else {
                        errorText = null
                        onSubmit(selectedRating, comment)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.testTag("submit_review_button"),
            ) {
                Text(if (isBusy) "កំពុងផ្ញើ..." else "ផ្ញើការវាយតម្លៃ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("បោះបង់")
            }
        },
    )
}
