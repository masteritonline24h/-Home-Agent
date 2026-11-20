package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgentInquiry
import com.example.data.model.AgentRatingSummary
import com.example.data.model.AgentReview
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMessageType
import com.example.data.model.ChatThread
import com.example.data.model.InquiryStatus
import com.example.data.model.InquiryType
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFilterState
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyStatus
import com.example.data.model.PropertyVisibility
import com.example.data.model.UserProfile
import com.example.data.repository.RealEstateRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class RealEstateViewModel(
    private val repository: RealEstateRepository,
    val currentUserId: String,
) : ViewModel() {

    private val _filterState = MutableStateFlow(PropertyFilterState())
    val filterState: StateFlow<PropertyFilterState> = _filterState.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "observeUserProfile")
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val publicPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        repository.observePublicProperties(currentUserId)
            .map<List<PropertyListing>, UiState<List<PropertyListing>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, "observePublicProperties")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        repository.observeMyProperties(currentUserId)
            .map<List<PropertyListing>, UiState<List<PropertyListing>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, "observeMyProperties")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoriteIds: StateFlow<Set<String>> =
        repository.observeFavoriteIds(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "observeFavoriteIds")
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val inquiriesState: StateFlow<UiState<List<AgentInquiry>>> =
        repository.observeInquiries(currentUserId)
            .map<List<AgentInquiry>, UiState<List<AgentInquiry>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, "observeInquiries")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val publicReviewsState: StateFlow<UiState<List<AgentReview>>> =
        repository.observePublicReviews(currentUserId)
            .map<List<AgentReview>, UiState<List<AgentReview>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, "observePublicReviews")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val agentRatingSummaries: StateFlow<Map<String, AgentRatingSummary>> =
        publicReviewsState.map { state ->
            val reviews = (state as? UiState.Success)?.data.orEmpty()
            reviews.groupBy { it.agentId }.mapValues { (_, list) ->
                val avg = if (list.isEmpty()) 0.0 else list.sumOf { it.rating }.toDouble() / list.size
                AgentRatingSummary(averageRating = avg, reviewCount = list.size)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    val chatThreadsState: StateFlow<UiState<List<ChatThread>>> =
        repository.observeChatThreads(currentUserId)
            .map<List<ChatThread>, UiState<List<ChatThread>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, "observeChatThreads")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val filteredPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        combine(publicPropertiesState, _filterState) { state, filter ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> {
                    val query = filter.searchQuery.trim().lowercase()
                    val districtQuery = filter.district?.trim()?.lowercase()
                    val filtered = state.data.filter { item ->
                        val matchesQuery = query.isEmpty() ||
                            item.title.lowercase().contains(query) ||
                            item.district.lowercase().contains(query) ||
                            item.city.lowercase().contains(query) ||
                            item.agentName.lowercase().contains(query)
                        val matchesListingType = filter.listingType == null ||
                            item.resolvedListingType() == filter.listingType
                        val matchesCategory = filter.category == null ||
                            item.resolvedCategory() == filter.category
                        val matchesCity = filter.city.isNullOrBlank() ||
                            item.city.equals(filter.city, ignoreCase = true)
                        val matchesDistrict = districtQuery.isNullOrEmpty() ||
                            item.district.lowercase().contains(districtQuery)
                        val matchesMinPrice = filter.minPriceUsd == null ||
                            item.priceUsd >= filter.minPriceUsd
                        val matchesMaxPrice = filter.maxPriceUsd == null ||
                            item.priceUsd <= filter.maxPriceUsd
                        val matchesBeds = filter.minBedrooms <= 0 ||
                            item.bedrooms >= filter.minBedrooms
                        val matchesBaths = filter.minBathrooms <= 0 ||
                            item.bathrooms >= filter.minBathrooms

                        matchesQuery && matchesListingType && matchesCategory &&
                            matchesCity && matchesDistrict &&
                            matchesMinPrice && matchesMaxPrice &&
                            matchesBeds && matchesBaths
                    }
                    UiState.Success(filtered)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val savedPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        combine(publicPropertiesState, myPropertiesState, favoriteIds) { pubState, myState, favs ->
            when {
                pubState is UiState.Loading -> UiState.Loading
                pubState is UiState.Error -> pubState
                else -> {
                    val pubList = (pubState as? UiState.Success)?.data.orEmpty()
                    val myList = (myState as? UiState.Success)?.data.orEmpty()
                    val merged = (pubList + myList).associateBy { it.id }
                    val saved = merged.values.filter { it.id in favs }
                    UiState.Success(saved)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile()
        }
    }

    fun observeMessagesForChat(chatId: String): Flow<UiState<List<ChatMessage>>> =
        repository.observeChatMessages(chatId, currentUserId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, "observeChatMessages")
                emit(UiState.Error(msg))
            }

    fun findPropertyById(propertyId: String): PropertyListing? {
        val pubList = (publicPropertiesState.value as? UiState.Success)?.data.orEmpty()
        val myList = (myPropertiesState.value as? UiState.Success)?.data.orEmpty()
        return pubList.firstOrNull { it.id == propertyId }
            ?: myList.firstOrNull { it.id == propertyId }
    }

    fun findChatById(chatId: String): ChatThread? {
        val chats = (chatThreadsState.value as? UiState.Success)?.data.orEmpty()
        return chats.firstOrNull { it.id == chatId }
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateListingTypeFilter(type: ListingType?) {
        _filterState.value = _filterState.value.copy(listingType = type)
    }

    fun updateCategoryFilter(category: PropertyCategory?) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun applyAdvancedFilters(
        category: PropertyCategory?,
        city: String?,
        district: String?,
        minPriceUsd: Long?,
        maxPriceUsd: Long?,
        minBedrooms: Int,
        minBathrooms: Int,
    ) {
        _filterState.value = _filterState.value.copy(
            category = category,
            city = city?.takeIf { it.isNotBlank() },
            district = district?.takeIf { it.isNotBlank() },
            minPriceUsd = minPriceUsd,
            maxPriceUsd = maxPriceUsd,
            minBedrooms = minBedrooms,
            minBathrooms = minBathrooms,
        )
    }

    fun clearAllFilters() {
        _filterState.value = PropertyFilterState()
    }

    fun toggleFavorite(propertyId: String) {
        val currentlyFav = propertyId in favoriteIds.value
        viewModelScope.launch {
            val result = repository.toggleFavorite(propertyId, currentlyFav)
            result.onSuccess {
                _snackbarMessages.tryEmit(
                    if (currentlyFav) "បានដកចេញពីបញ្ជីរក្សាទុក"
                    else "បានរក្សាទុកអចលនទ្រព្យ"
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចរក្សាទុកបានទេ")
            }
        }
    }

    fun saveProperty(
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
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = if (editingId.isNullOrBlank()) {
                repository.createProperty(
                    title = title,
                    listingType = listingType,
                    category = category,
                    priceUsd = priceUsd,
                    district = district,
                    city = city,
                    bedrooms = bedrooms,
                    bathrooms = bathrooms,
                    areaSqm = areaSqm,
                    description = description,
                    imageKey = imageKey,
                    agentName = agentName,
                    agentAgency = agentAgency,
                    businessHotline = businessHotline,
                    telegramHandle = telegramHandle,
                    visibility = visibility,
                )
            } else {
                repository.updateProperty(
                    propertyId = editingId,
                    title = title,
                    listingType = listingType,
                    category = category,
                    priceUsd = priceUsd,
                    district = district,
                    city = city,
                    bedrooms = bedrooms,
                    bathrooms = bathrooms,
                    areaSqm = areaSqm,
                    description = description,
                    imageKey = imageKey,
                    agentName = agentName,
                    agentAgency = agentAgency,
                    businessHotline = businessHotline,
                    telegramHandle = telegramHandle,
                    visibility = visibility,
                    status = status,
                )
            }
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit(
                    if (editingId.isNullOrBlank()) "បានដាក់ប្រកាសអចលនទ្រព្យជោគជ័យ"
                    else "បានកែប្រែព័ត៌មានអចលនទ្រព្យជោគជ័យ"
                )
                onComplete()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "រក្សាទុកបរាជ័យ")
            }
        }
    }

    fun updatePropertyStatus(propertyId: String, newStatus: PropertyStatus) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.updatePropertyStatus(propertyId, newStatus)
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានប្តូរស្ថានភាពទៅជា ${newStatus.labelKm}")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចប្តូរស្ថានភាពបានទេ")
            }
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.deleteProperty(propertyId)
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានលុបប្រកាសអចលនទ្រព្យរួចរាល់")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចលុបបានទេ")
            }
        }
    }

    fun sendInquiry(
        property: PropertyListing,
        buyerName: String,
        buyerPhone: String,
        inquiryType: InquiryType,
        preferredDate: String,
        message: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.sendInquiry(
                property = property,
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                inquiryType = inquiryType,
                preferredDate = preferredDate,
                message = message,
            )
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានផ្ញើសំណើទៅភ្នាក់ងារជោគជ័យ!")
                onComplete()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "ផ្ញើសំណើបរាជ័យ")
            }
        }
    }

    fun replyToInquiry(
        inquiryId: String,
        replyText: String,
        newStatus: InquiryStatus,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.replyToInquiry(inquiryId, replyText, newStatus)
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានឆ្លើយតបទៅអតិថិជនជោគជ័យ")
                onComplete()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "ឆ្លើយតបបរាជ័យ")
            }
        }
    }

    fun closeInquiryByBuyer(inquiryId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.closeInquiryByBuyer(inquiryId)
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានបិទសំណើទំនាក់ទំនង")
                onComplete()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចបិទសំណើបានទេ")
            }
        }
    }

    // --- AGENT REVIEWS ---

    fun submitAgentReview(
        agentId: String,
        agentName: String,
        rating: Int,
        comment: String,
        onComplete: () -> Unit,
    ) {
        val reviewerName = userProfile.value?.displayName ?: "អតិថិជន ផ្ទះខ្មែរ"
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.submitAgentReview(
                agentId = agentId,
                agentName = agentName,
                reviewerName = reviewerName,
                rating = rating,
                comment = comment,
            )
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("អរគុណសម្រាប់ការវាយតម្លៃភ្នាក់ងារ!")
                onComplete()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចផ្ញើការវាយតម្លៃបានទេ")
            }
        }
    }

    fun deleteAgentReview(reviewId: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.deleteAgentReview(reviewId)
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានលុបការវាយតម្លៃ")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចលុបបានទេ")
            }
        }
    }

    // --- DIRECT CHAT ---

    fun startPropertyChat(
        property: PropertyListing,
        onChatReady: (String) -> Unit,
    ) {
        val buyerName = userProfile.value?.displayName ?: "អតិថិជន"
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.openOrStartPropertyChat(property, buyerName)
            _isActionBusy.value = false
            result.onSuccess { chatId ->
                onChatReady(chatId)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចបើកការជជែកបានទេ")
            }
        }
    }

    fun sendChatMessage(
        chatThread: ChatThread,
        text: String,
        messageType: ChatMessageType = ChatMessageType.TEXT,
        preferredDate: String = "",
        onSent: () -> Unit = {},
    ) {
        val senderName = userProfile.value?.displayName ?: "អ្នកប្រើប្រាស់"
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.sendChatMessage(
                chatThread = chatThread,
                senderName = senderName,
                text = text,
                messageType = messageType,
                preferredDate = preferredDate,
            )
            _isActionBusy.value = false
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "មិនអាចផ្ញើសារបានទេ")
            }
        }
    }

    fun updateAgentProfile(
        displayName: String,
        agentTitle: String,
        businessHotline: String,
        telegramHandle: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.updateUserProfile(
                displayName = displayName,
                agentTitle = agentTitle,
                businessHotline = businessHotline,
                telegramHandle = telegramHandle,
            )
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានរក្សាទុកព័ត៌មានភ្នាក់ងារ")
                onComplete()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "រក្សាទុកបរាជ័យ")
            }
        }
    }

    fun seedStarterPortfolio() {
        val profile = userProfile.value
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.seedStarterPortfolio(
                agentName = profile?.displayName ?: "ភ្នាក់ងារ ផ្ទះខ្មែរ",
                businessHotline = profile?.businessHotline ?: "012 888 999",
                telegramHandle = profile?.telegramHandle ?: "@khmerhome_agent",
            )
            _isActionBusy.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("បានបន្ថែមបញ្ជីផ្ទះគំរូ ៥ កន្លែងទៅកាន់ Cloud")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "បន្ថែមបញ្ជីបរាជ័យ")
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable, source: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error in $source", rootError)
        return rootError.localizedMessage ?: "មានបញ្ហាក្នុងការទាញទិន្នន័យ"
    }

    private companion object {
        const val TAG = "RealEstateVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
