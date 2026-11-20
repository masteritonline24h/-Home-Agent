package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHomeWork
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFilterState
import com.example.data.model.PropertyListing
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.PropertyCard
import com.example.ui.viewmodel.UiState

@Composable
fun ExploreScreen(
    propertiesState: UiState<List<PropertyListing>>,
    totalPublicCount: Int,
    favoriteIds: Set<String>,
    filterState: PropertyFilterState,
    isActionBusy: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onListingTypeChange: (ListingType?) -> Unit,
    onCategoryChange: (PropertyCategory?) -> Unit,
    onApplyAdvancedFilters: (
        category: PropertyCategory?,
        city: String?,
        district: String?,
        minPriceUsd: Long?,
        maxPriceUsd: Long?,
        minBedrooms: Int,
        minBathrooms: Int,
    ) -> Unit,
    onClearFilters: () -> Unit,
    onPropertyClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenCreateProperty: () -> Unit,
    onSeedStarterPortfolio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 900.dp -> 2
            else -> 1
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(columns) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Search + Filter button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = filterState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "ស្វែងរក",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                if (filterState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = "សម្អាតពាក្យស្វែងរក",
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_property_input"),
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            IconButton(
                                onClick = { showFilterSheet = true },
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("open_filter_sheet_button"),
                            ) {
                                BadgedBox(
                                    badge = {
                                        val count = filterState.activeFilterCount()
                                        if (count > 0) {
                                            Badge { Text(count.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "តម្រងលម្អិត",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }

                    // Sale / Rent Segmented Row
                    val listingOptions = listOf<Pair<ListingType?, String>>(
                        null to stringResource(id = R.string.filter_all),
                        ListingType.SALE to stringResource(id = R.string.filter_sale),
                        ListingType.RENT to stringResource(id = R.string.filter_rent),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listingOptions.forEachIndexed { index, (type, label) ->
                            SegmentedButton(
                                selected = filterState.listingType == type,
                                onClick = { onListingTypeChange(type) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = listingOptions.size,
                                ),
                                modifier = Modifier.testTag("listing_type_segment_${type?.raw ?: "ALL"}"),
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }

                    // Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = filterState.category == null,
                            onClick = { onCategoryChange(null) },
                            label = { Text("គ្រប់ប្រភេទ") },
                            modifier = Modifier.testTag("category_chip_ALL"),
                        )
                        PropertyCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = filterState.category == cat,
                                onClick = {
                                    onCategoryChange(if (filterState.category == cat) null else cat)
                                },
                                label = { Text(cat.labelKm) },
                                modifier = Modifier.testTag("category_chip_${cat.raw}"),
                            )
                        }
                    }
                }
            }

            when (propertiesState) {
                is UiState.Loading -> {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(span = { GridItemSpan(columns) }) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = propertiesState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val itemsList = propertiesState.data
                    if (itemsList.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            ExploreEmptyState(
                                hasExistingListings = totalPublicCount > 0,
                                isBusy = isActionBusy,
                                onClearFilters = onClearFilters,
                                onOpenCreateProperty = onOpenCreateProperty,
                                onSeedStarterPortfolio = onSeedStarterPortfolio,
                            )
                        }
                    } else {
                        item(span = { GridItemSpan(columns) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "លទ្ធផលអចលនទ្រព្យ (${itemsList.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (filterState.activeFilterCount() > 0 ||
                                    filterState.searchQuery.isNotBlank() ||
                                    filterState.listingType != null ||
                                    filterState.category != null
                                ) {
                                    TextButton(onClick = onClearFilters) {
                                        Text("បង្ហាញទាំងអស់")
                                    }
                                }
                            }
                        }

                        items(itemsList, key = { it.id }) { property ->
                            PropertyCard(
                                property = property,
                                isFavorited = property.id in favoriteIds,
                                onCardClick = { onPropertyClick(property.id) },
                                onToggleFavorite = { onToggleFavorite(property.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = filterState,
            onApply = onApplyAdvancedFilters,
            onReset = onClearFilters,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun ExploreEmptyState(
    hasExistingListings: Boolean,
    isBusy: Boolean,
    onClearFilters: () -> Unit,
    onOpenCreateProperty: () -> Unit,
    onSeedStarterPortfolio: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasExistingListings) {
                Image(
                    painter = painterResource(id = R.drawable.img_agent_hero),
                    contentDescription = "ស្វាគមន៍មកកាន់ ផ្ទះខ្មែរ",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Text(
                text = if (hasExistingListings) {
                    "មិនមានអចលនទ្រព្យត្រូវនឹងការស្វែងរក"
                } else {
                    "មិនទាន់មានបញ្ជីអចលនទ្រព្យនៅឡើយទេ"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = if (hasExistingListings) {
                    "សាកល្បងសម្អាតតម្រង ឬប្តូរពាក្យស្វែងរកផ្សេង"
                } else {
                    "លោកអ្នកអាចដាក់ប្រកាសលក់/ជួលផ្ទះថ្មី ឬបន្ថែមបញ្ជីផ្ទះគំរូចំនួន ៥ កន្លែងដើម្បីសាកល្បងស្វែងរក និងទំនាក់ទំនងភ្នាក់ងារ។"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (hasExistingListings) {
                Button(
                    onClick = onClearFilters,
                    modifier = Modifier.testTag("empty_clear_filters_button"),
                ) {
                    Text("សម្អាតតម្រង")
                }
            } else {
                Button(
                    onClick = onOpenCreateProperty,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("empty_post_property_button"),
                ) {
                    Icon(Icons.Filled.AddHomeWork, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.post_property))
                }
                OutlinedButton(
                    onClick = onSeedStarterPortfolio,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("seed_starter_portfolio_button"),
                ) {
                    Text(if (isBusy) "កំពុងបន្ថែម..." else stringResource(id = R.string.load_starter_portfolio))
                }
            }
        }
    }
}
