package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ImagePreset
import com.example.data.model.InquiryType
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFilterState
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyStatus
import com.example.data.model.PropertyVisibility
import com.example.data.model.UserProfile
import com.example.data.model.formatUsdPrice

@Composable
fun PropertyCard(
    property: PropertyListing,
    isFavorited: Boolean,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listingType = property.resolvedListingType()
    val status = property.resolvedStatus()
    val category = property.resolvedCategory()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("property_card_${property.id}")
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            ) {
                Image(
                    painter = painterResource(id = property.resolvedImagePreset().drawableRes),
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = if (listingType == ListingType.SALE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = listingType.labelKm,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }

                        if (status != PropertyStatus.AVAILABLE) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = status.labelKm,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape,
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_button_${property.id}"),
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorited) "ដកចេញពីបញ្ជីរក្សាទុក" else "រក្សាទុកអចលនទ្រព្យ",
                                tint = if (isFavorited) Color(0xFFFF6B6B) else Color.White,
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    shadowElevation = 3.dp,
                ) {
                    Text(
                        text = formatUsdPrice(property.priceUsd, listingType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.58f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                ) {
                    Text(
                        text = category.labelKm,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.district} • ${property.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (property.bedrooms > 0) {
                        SpecBadge(icon = Icons.Filled.Bed, text = "${property.bedrooms} បន្ទប់គេង")
                    }
                    if (property.bathrooms > 0) {
                        SpecBadge(icon = Icons.Filled.Bathtub, text = "${property.bathrooms} បន្ទប់ទឹក")
                    }
                    SpecBadge(icon = Icons.Filled.SquareFoot, text = "${property.areaSqm} m²")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = property.agentName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = "មើលលម្អិត →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun SpecBadge(
    icon: ImageVector,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: PropertyFilterState,
    onApply: (
        category: PropertyCategory?,
        city: String?,
        district: String?,
        minPriceUsd: Long?,
        maxPriceUsd: Long?,
        minBedrooms: Int,
        minBathrooms: Int,
    ) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf(currentFilter.category) }
    var selectedCity by remember { mutableStateOf(currentFilter.city) }
    var districtText by remember { mutableStateOf(currentFilter.district.orEmpty()) }
    var minPriceText by remember { mutableStateOf(currentFilter.minPriceUsd?.toString().orEmpty()) }
    var maxPriceText by remember { mutableStateOf(currentFilter.maxPriceUsd?.toString().orEmpty()) }
    var selectedMinBeds by remember { mutableIntStateOf(currentFilter.minBedrooms) }
    var selectedMinBaths by remember { mutableIntStateOf(currentFilter.minBathrooms) }

    val cities = listOf("ភ្នំពេញ", "សៀមរាប", "ព្រះសីហនុ", "កំពត", "កណ្តាល")
    val neighborhoods = listOf("បឹងកេងកង ១", "ជ្រោយចង្វារ", "សែនសុខ", "ទួលគោក", "ចំការមន", "សាលាកំរើក")
    val countOptions = listOf(0 to "ទាំងអស់", 1 to "1+", 2 to "2+", 3 to "3+", 4 to "4+")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "តម្រងស្វែងរកលម្អិត",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("reset_filters_button"),
                ) {
                    Text("សម្អាតទាំងអស់")
                }
            }

            // Property Type
            Text(
                text = "ប្រភេទអចលនទ្រព្យ",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("ទាំងអស់") },
                )
                PropertyCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.labelKm) },
                    )
                }
            }

            // Price Min / Max
            Text(
                text = "កម្រិតតម្លៃ (USD)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minPriceText,
                    onValueChange = { minPriceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("តម្លៃអប្បបរមា ($)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("filter_min_price_input"),
                )
                OutlinedTextField(
                    value = maxPriceText,
                    onValueChange = { maxPriceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("តម្លៃអតិបរមា ($)") },
                    placeholder = { Text("គ្មានកំណត់") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("filter_max_price_input"),
                )
            }

            // Bedrooms
            Text(
                text = "ចំនួនបន្ទប់គេងអប្បបរមា",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                countOptions.forEach { (beds, label) ->
                    FilterChip(
                        selected = selectedMinBeds == beds,
                        onClick = { selectedMinBeds = beds },
                        label = { Text(label) },
                    )
                }
            }

            // Bathrooms
            Text(
                text = "ចំនួនបន្ទប់ទឹកអប្បបរមា",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                countOptions.forEach { (baths, label) ->
                    FilterChip(
                        selected = selectedMinBaths == baths,
                        onClick = { selectedMinBaths = baths },
                        label = { Text(label) },
                    )
                }
            }

            // City / Province
            Text(
                text = "រាជធានី / ខេត្ត",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedCity == null,
                    onClick = { selectedCity = null },
                    label = { Text("ទាំងអស់") },
                )
                cities.forEach { city ->
                    FilterChip(
                        selected = selectedCity == city,
                        onClick = { selectedCity = if (selectedCity == city) null else city },
                        label = { Text(city) },
                    )
                }
            }

            // Neighborhood / Khan
            Text(
                text = "តំបន់ / ខណ្ឌ / សង្កាត់",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                neighborhoods.forEach { hood ->
                    FilterChip(
                        selected = districtText.equals(hood, ignoreCase = true),
                        onClick = {
                            districtText = if (districtText.equals(hood, ignoreCase = true)) "" else hood
                        },
                        label = { Text(hood) },
                    )
                }
            }
            OutlinedTextField(
                value = districtText,
                onValueChange = { districtText = it },
                label = { Text("វាយឈ្មោះតំបន់ ឬខណ្ឌជាក់លាក់") },
                placeholder = { Text("ឧ. ច្បារអំពៅ, ដូនពេញ...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("filter_neighborhood_input"),
            )

            Button(
                onClick = {
                    val minP = minPriceText.toLongOrNull()
                    val maxP = maxPriceText.toLongOrNull()
                    onApply(
                        selectedCategory,
                        selectedCity,
                        districtText.trim().takeIf { it.isNotEmpty() },
                        minP,
                        maxP,
                        selectedMinBeds,
                        selectedMinBaths,
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_filters_button"),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("អនុវត្តតម្រង")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryBottomSheet(
    property: PropertyListing,
    userProfile: UserProfile?,
    isBusy: Boolean,
    onSubmit: (
        buyerName: String,
        buyerPhone: String,
        inquiryType: InquiryType,
        preferredDate: String,
        message: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var buyerName by remember { mutableStateOf(userProfile?.displayName.orEmpty()) }
    var buyerPhone by remember { mutableStateOf("") }
    var inquiryType by remember { mutableStateOf(InquiryType.VIEWING) }
    var preferredDate by remember { mutableStateOf("ចុងសប្តាហ៍នេះ ម៉ោង 9:00 ព្រឹក") }
    var message by remember {
        mutableStateOf("សួស្តីភ្នាក់ងារ ${property.agentName}! ខ្ញុំចាប់អារម្មណ៍លើ「${property.title}」 សូមផ្តល់ព័ត៌មានបន្ថែម។")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "ស្នើសុំមើលផ្ទះ / ផ្ញើសំណើ៖ ${property.agentName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = property.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InquiryType.entries.forEach { type ->
                    FilterChip(
                        selected = inquiryType == type,
                        onClick = { inquiryType = type },
                        label = { Text(type.labelKm) },
                    )
                }
            }

            OutlinedTextField(
                value = buyerName,
                onValueChange = { buyerName = it },
                label = { Text("ឈ្មោះរបស់អ្នក *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inquiry_buyer_name_input"),
            )

            OutlinedTextField(
                value = buyerPhone,
                onValueChange = { buyerPhone = it },
                label = { Text("លេខទូរសព្ទ / Telegram របស់អ្នក *") },
                placeholder = { Text("ឧ. 012 345 678") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inquiry_phone_input"),
            )

            if (inquiryType == InquiryType.VIEWING) {
                OutlinedTextField(
                    value = preferredDate,
                    onValueChange = { preferredDate = it },
                    label = { Text("ថ្ងៃ និងម៉ោងដែលងាយស្រួលមើលផ្ទះ") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inquiry_date_input"),
                )
            }

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("សារទៅកាន់ភ្នាក់ងារ *") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inquiry_message_input"),
            )

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("បោះបង់")
                }
                Button(
                    onClick = {
                        when {
                            buyerName.isBlank() -> validationError = "សូមបញ្ចូលឈ្មោះរបស់អ្នក"
                            buyerPhone.isBlank() -> validationError = "សូមបញ្ចូលលេខទូរសព្ទទំនាក់ទំនង"
                            message.isBlank() -> validationError = "សូមសរសេរសារទៅកាន់ភ្នាក់ងារ"
                            else -> {
                                validationError = null
                                onSubmit(buyerName, buyerPhone, inquiryType, preferredDate, message)
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("submit_inquiry_button"),
                ) {
                    Text(if (isBusy) "កំពុងផ្ញើ..." else "ផ្ញើសំណើ")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyFormBottomSheet(
    editingProperty: PropertyListing?,
    userProfile: UserProfile?,
    isBusy: Boolean,
    onSave: (
        editingId: String?,
        title: String,
        listingType: ListingType,
        category: PropertyCategory,
        priceUsd: Long,
        district: String,
        city: String,
        bedrooms: Int,
        bathrooms: Int,
        areaSqm: Int,
        description: String,
        imageKey: String,
        agentName: String,
        agentAgency: String,
        businessHotline: String,
        telegramHandle: String,
        visibility: PropertyVisibility,
        status: PropertyStatus,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(editingProperty?.title.orEmpty()) }
    var listingType by remember {
        mutableStateOf(editingProperty?.resolvedListingType() ?: ListingType.SALE)
    }
    var category by remember {
        mutableStateOf(editingProperty?.resolvedCategory() ?: PropertyCategory.BOREY)
    }
    var priceText by remember {
        mutableStateOf(editingProperty?.priceUsd?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var district by remember { mutableStateOf(editingProperty?.district.orEmpty()) }
    var city by remember { mutableStateOf(editingProperty?.city ?: "ភ្នំពេញ") }
    var bedroomsText by remember {
        mutableStateOf((editingProperty?.bedrooms ?: 3L).toString())
    }
    var bathroomsText by remember {
        mutableStateOf((editingProperty?.bathrooms ?: 3L).toString())
    }
    var areaText by remember {
        mutableStateOf((editingProperty?.areaSqm?.takeIf { it > 0 } ?: 120L).toString())
    }
    var description by remember { mutableStateOf(editingProperty?.description.orEmpty()) }
    var imageKey by remember {
        mutableStateOf(editingProperty?.imageKey ?: category.defaultImageKey)
    }
    var agentName by remember {
        mutableStateOf(editingProperty?.agentName ?: userProfile?.displayName.orEmpty())
    }
    var agentAgency by remember {
        mutableStateOf(editingProperty?.agentAgency ?: "ផ្ទះខ្មែរ អចលនទ្រព្យ")
    }
    var hotline by remember {
        mutableStateOf(editingProperty?.businessHotline ?: userProfile?.businessHotline ?: "012 888 999")
    }
    var telegram by remember {
        mutableStateOf(editingProperty?.telegramHandle ?: userProfile?.telegramHandle ?: "@khmerhome_agent")
    }
    var visibility by remember {
        mutableStateOf(editingProperty?.resolvedVisibility() ?: PropertyVisibility.PUBLIC)
    }
    var status by remember {
        mutableStateOf(editingProperty?.resolvedStatus() ?: PropertyStatus.AVAILABLE)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editingProperty == null) "ដាក់ប្រកាសលក់ / ជួលថ្មី" else "កែប្រែព័ត៌មានអចលនទ្រព្យ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text("ប្រភេទប្រកាស", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListingType.entries.forEach { type ->
                    FilterChip(
                        selected = listingType == type,
                        onClick = { listingType = type },
                        label = { Text(type.labelKm) },
                    )
                }
            }

            Text("ប្រភេទអចលនទ្រព្យ", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PropertyCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = {
                            category = cat
                            if (editingProperty == null) {
                                imageKey = cat.defaultImageKey
                            }
                        },
                        label = { Text(cat.labelKm) },
                    )
                }
            }

            Text("រូបភាពតំណាង", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ImagePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = imageKey == preset.key,
                        onClick = { imageKey = preset.key },
                        label = { Text(preset.labelKm) },
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("ចំណងជើងប្រកាស *") },
                placeholder = { Text("ឧ. វីឡាទំនើប បឹងកេងកង ១") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("property_title_input"),
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                label = {
                    Text(if (listingType == ListingType.RENT) "តម្លៃជួលប្រចាំខែ (USD) *" else "តម្លៃលក់ (USD) *")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("property_price_input"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("ខណ្ឌ / ស្រុក *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("property_district_input"),
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("រាជធានី / ខេត្ត *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("property_city_input"),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bedroomsText,
                    onValueChange = { bedroomsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("បន្ទប់គេង") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = bathroomsText,
                    onValueChange = { bathroomsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("បន្ទប់ទឹក") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = areaText,
                    onValueChange = { areaText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ទំហំ (m²)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("ព័ត៌មានលម្អិតអំពីផ្ទះ/ដី") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth().testTag("property_description_input"),
            )

            OutlinedTextField(
                value = agentName,
                onValueChange = { agentName = it },
                label = { Text("ឈ្មោះភ្នាក់ងារ *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hotline,
                    onValueChange = { hotline = it },
                    label = { Text("លេខទូរសព្ទ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = telegram,
                    onValueChange = { telegram = it },
                    label = { Text("Telegram (@...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text("ការបង្ហាញប្រកាស", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyVisibility.entries.forEach { vis ->
                    FilterChip(
                        selected = visibility == vis,
                        onClick = { visibility = vis },
                        label = { Text(vis.labelKm) },
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 0L
                    val beds = (bedroomsText.toIntOrNull() ?: 0).coerceIn(0, 30)
                    val baths = (bathroomsText.toIntOrNull() ?: 0).coerceIn(0, 30)
                    val area = areaText.toIntOrNull() ?: 0
                    when {
                        title.trim().length < 2 -> validationError = "សូមបញ្ចូលចំណងជើងយ៉ាងតិច ២ តួអក្សរ"
                        price <= 0L -> validationError = "សូមបញ្ចូលតម្លៃធំជាង 0"
                        district.isBlank() -> validationError = "សូមបញ្ចូលខណ្ឌ/ស្រុក"
                        city.isBlank() -> validationError = "សូមបញ្ចូលរាជធានី/ខេត្ត"
                        area <= 0 -> validationError = "សូមបញ្ចូលទំហំ (m²) ធំជាង 0"
                        agentName.isBlank() -> validationError = "សូមបញ្ចូលឈ្មោះភ្នាក់ងារ"
                        hotline.isBlank() -> validationError = "សូមបញ្ចូលលេខទូរសព្ទ"
                        else -> {
                            validationError = null
                            onSave(
                                editingProperty?.id,
                                title,
                                listingType,
                                category,
                                price,
                                district,
                                city,
                                beds,
                                baths,
                                area,
                                description,
                                imageKey,
                                agentName,
                                agentAgency,
                                hotline,
                                telegram,
                                visibility,
                                status,
                            )
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_property_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(if (isBusy) "កំពុងរក្សាទុក..." else "រក្សាទុក និងផ្សព្វផ្សាយ")
            }
        }
    }
}
