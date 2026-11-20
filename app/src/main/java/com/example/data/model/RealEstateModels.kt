package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ListingType(val raw: String, val labelKm: String) {
    SALE("SALE", "សម្រាប់លក់"),
    RENT("RENT", "សម្រាប់ជួល");

    companion object {
        fun fromRaw(value: String): ListingType =
            entries.firstOrNull { it.raw == value } ?: SALE
    }
}

enum class PropertyCategory(val raw: String, val labelKm: String, val defaultImageKey: String) {
    VILLA("VILLA", "វីឡា", "villa_luxury"),
    CONDO("CONDO", "ខុនដូ", "condo_skyline"),
    APARTMENT("APARTMENT", "អាផាតមិន", "condo_skyline"),
    BOREY("BOREY", "ផ្ទះបុរី/ល្វែង", "borey_townhouse"),
    LAND("LAND", "ដីឡូតិ៍", "land_plot");

    companion object {
        fun fromRaw(value: String): PropertyCategory =
            entries.firstOrNull { it.raw == value } ?: BOREY
    }
}

enum class PropertyStatus(val raw: String, val labelKm: String) {
    AVAILABLE("AVAILABLE", "កំពុងបើក"),
    RESERVED("RESERVED", "បានកក់"),
    CLOSED("CLOSED", "បានលក់/ជួលរួច");

    companion object {
        fun fromRaw(value: String): PropertyStatus =
            entries.firstOrNull { it.raw == value } ?: AVAILABLE
    }
}

enum class PropertyVisibility(val raw: String, val labelKm: String) {
    PUBLIC("PUBLIC", "បង្ហាញជាសាធារណៈ"),
    PRIVATE("PRIVATE", "រក្សាទុកផ្ទាល់ខ្លួន");

    companion object {
        fun fromRaw(value: String): PropertyVisibility =
            entries.firstOrNull { it.raw == value } ?: PUBLIC
    }
}

enum class ImagePreset(val key: String, val labelKm: String, @DrawableRes val drawableRes: Int) {
    VILLA_LUXURY("villa_luxury", "វីឡាទំនើប", R.drawable.img_villa_luxury),
    CONDO_SKYLINE("condo_skyline", "ខុនដូ/អាផាតមិន", R.drawable.img_condo_skyline),
    BOREY_TOWNHOUSE("borey_townhouse", "ផ្ទះល្វែងបុរី", R.drawable.img_borey_townhouse),
    LAND_PLOT("land_plot", "ដីគម្រោង", R.drawable.img_land_plot);

    companion object {
        fun fromKey(key: String): ImagePreset =
            entries.firstOrNull { it.key == key } ?: BOREY_TOWNHOUSE
    }
}

enum class InquiryType(val raw: String, val labelKm: String) {
    VIEWING("VIEWING", "ស្នើសុំមើលផ្ទះ"),
    QUESTION("QUESTION", "សួរព័ត៌មាន"),
    OFFER("OFFER", "ស្នើតម្លៃ");

    companion object {
        fun fromRaw(value: String): InquiryType =
            entries.firstOrNull { it.raw == value } ?: VIEWING
    }
}

enum class InquiryStatus(val raw: String, val labelKm: String) {
    PENDING("PENDING", "កំពុងរង់ចាំ"),
    SCHEDULED("SCHEDULED", "បានណាត់ជួប"),
    REPLIED("REPLIED", "ភ្នាក់ងារបានឆ្លើយ"),
    CLOSED("CLOSED", "បានបញ្ចប់");

    companion object {
        fun fromRaw(value: String): InquiryStatus =
            entries.firstOrNull { it.raw == value } ?: PENDING
    }
}

enum class ChatMessageType(val raw: String) {
    TEXT("TEXT"),
    VIEWING_REQUEST("VIEWING_REQUEST");

    companion object {
        fun fromRaw(value: String): ChatMessageType =
            entries.firstOrNull { it.raw == value } ?: TEXT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val agentTitle: String = "ភ្នាក់ងារអចលនទ្រព្យ",
    val businessHotline: String = "012 888 999",
    val telegramHandle: String = "@khmerhome_agent",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PropertyListing(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val listingType: String = ListingType.SALE.raw,
    val propertyType: String = PropertyCategory.BOREY.raw,
    val priceUsd: Long = 0L,
    val district: String = "",
    val city: String = "ភ្នំពេញ",
    val bedrooms: Long = 0L,
    val bathrooms: Long = 0L,
    val areaSqm: Long = 0L,
    val description: String = "",
    val imageKey: String = ImagePreset.BOREY_TOWNHOUSE.key,
    val agentName: String = "",
    val agentAgency: String = "ផ្ទះខ្មែរ អចលនទ្រព្យ",
    val businessHotline: String = "012 888 999",
    val telegramHandle: String = "",
    val visibility: String = PropertyVisibility.PUBLIC.raw,
    val status: String = PropertyStatus.AVAILABLE.raw,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedListingType(): ListingType = ListingType.fromRaw(listingType)
    fun resolvedCategory(): PropertyCategory = PropertyCategory.fromRaw(propertyType)
    fun resolvedStatus(): PropertyStatus = PropertyStatus.fromRaw(status)
    fun resolvedVisibility(): PropertyVisibility = PropertyVisibility.fromRaw(visibility)
    fun resolvedImagePreset(): ImagePreset = ImagePreset.fromKey(imageKey)
}

@Keep
@IgnoreExtraProperties
data class FavoriteProperty(
    @DocumentId val id: String = "",
    val userId: String = "",
    val propertyId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AgentInquiry(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val agentId: String = "",
    val agentName: String = "",
    val propertyId: String = "",
    val propertyTitle: String = "",
    val inquiryType: String = InquiryType.VIEWING.raw,
    val preferredDate: String = "",
    val message: String = "",
    val agentReply: String = "",
    val status: String = InquiryStatus.PENDING.raw,
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedType(): InquiryType = InquiryType.fromRaw(inquiryType)
    fun resolvedStatus(): InquiryStatus = InquiryStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class AgentReview(
    @DocumentId val id: String = "",
    val agentId: String = "",
    val agentName: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val agentId: String = "",
    val agentName: String = "",
    val propertyId: String = "",
    val propertyTitle: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val messageType: String = ChatMessageType.TEXT.raw,
    val preferredDate: String = "",
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun resolvedMessageType(): ChatMessageType = ChatMessageType.fromRaw(messageType)
}

data class AgentRatingSummary(
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
)

data class PropertyFilterState(
    val searchQuery: String = "",
    val listingType: ListingType? = null,
    val category: PropertyCategory? = null,
    val city: String? = null,
    val district: String? = null,
    val minPriceUsd: Long? = null,
    val maxPriceUsd: Long? = null,
    val minBedrooms: Int = 0,
    val minBathrooms: Int = 0,
) {
    fun activeFilterCount(): Int {
        var count = 0
        if (category != null) count++
        if (!city.isNullOrBlank()) count++
        if (!district.isNullOrBlank()) count++
        if (minPriceUsd != null) count++
        if (maxPriceUsd != null) count++
        if (minBedrooms > 0) count++
        if (minBathrooms > 0) count++
        return count
    }
}

fun formatUsdPrice(priceUsd: Long, listingType: ListingType): String {
    val formatted = "%,d".format(priceUsd)
    return if (listingType == ListingType.RENT) {
        "$$formatted / ខែ"
    } else {
        "$$formatted"
    }
}
