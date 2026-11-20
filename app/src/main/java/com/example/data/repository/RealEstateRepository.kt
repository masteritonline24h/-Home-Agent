package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.AgentInquiry
import com.example.data.model.AgentReview
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMessageType
import com.example.data.model.ChatThread
import com.example.data.model.FavoriteProperty
import com.example.data.model.ImagePreset
import com.example.data.model.InquiryStatus
import com.example.data.model.InquiryType
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyStatus
import com.example.data.model.PropertyVisibility
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RealEstateRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("ត្រូវចូលគណនី Google ជាមុនសិន។")

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val docPath = "users/$uid"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("Unauthenticated")
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeDisplayName = (user.displayName?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: user.email?.substringBefore("@")
                    ?: "ភ្នាក់ងារ ផ្ទះខ្មែរ").take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "email" to (user.email ?: "").take(120),
                    "agentTitle" to "ភ្នាក់ងារអចលនទ្រព្យ",
                    "businessHotline" to "012 888 999",
                    "telegramHandle" to "@khmerhome_agent",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        agentTitle: String,
        businessHotline: String,
        telegramHandle: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val docPath = "users/$uid"
        return try {
            val cleanName = displayName.trim()
            require(cleanName.length in 1..60) { "ឈ្មោះត្រូវមានពី 1 ដល់ 60 តួអក្សរ" }
            val cleanTitle = agentTitle.trim().take(60)
            val cleanHotline = businessHotline.trim().take(30)
            val cleanTelegram = telegramHandle.trim().take(50)

            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "agentTitle" to cleanTitle,
                    "businessHotline" to cleanHotline,
                    "telegramHandle" to cleanTelegram,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- PROPERTIES ---

    fun observePublicProperties(currentUserId: String): Flow<List<PropertyListing>> = flow {
        val path = "properties"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", PropertyVisibility.PUBLIC.raw)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, currentUserId)
                    throw error
                }
        )
    }

    fun observeMyProperties(userId: String): Flow<List<PropertyListing>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "properties"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PropertyListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun createProperty(
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
    ): Result<Unit> {
        var docPath = "properties"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanDistrict = district.trim()
            val cleanCity = city.trim()
            val cleanAgentName = agentName.trim()
            val cleanAgency = agentAgency.trim()
            val cleanHotline = businessHotline.trim()

            require(cleanTitle.length in 2..120) { "ចំណងជើងត្រូវមានពី 2 ដល់ 120 តួអក្សរ" }
            require(priceUsd in 1..100_000_000) { "តម្លៃមិនត្រឹមត្រូវ" }
            require(cleanDistrict.length in 1..60) { "សូមបញ្ចូលខណ្ឌ/ស្រុក" }
            require(cleanCity.length in 1..60) { "សូមបញ្ចូលរាជធានី/ខេត្ត" }
            require(bedrooms in 0..30) { "ចំនួនបន្ទប់គេងមិនត្រឹមត្រូវ" }
            require(bathrooms in 0..30) { "ចំនួនបន្ទប់ទឹកមិនត្រឹមត្រូវ" }
            require(areaSqm in 1..100_000) { "ទំហំត្រូវធំជាង 0 m²" }
            require(cleanAgentName.length in 1..60) { "សូមបញ្ចូលឈ្មោះភ្នាក់ងារ" }
            require(cleanAgency.length in 1..60) { "សូមបញ្ចូលឈ្មោះក្រុមហ៊ុន/ភ្នាក់ងារ" }
            require(cleanHotline.length in 1..30) { "សូមបញ្ចូលលេខទូរសព្ទទំនាក់ទំនង" }

            val validImageKey = ImagePreset.fromKey(imageKey).key
            val docRef = db.collection("properties").document()
            docPath = docRef.path

            val payload = mapOf(
                "ownerId" to uid,
                "title" to cleanTitle,
                "listingType" to listingType.raw,
                "propertyType" to category.raw,
                "priceUsd" to priceUsd,
                "district" to cleanDistrict,
                "city" to cleanCity,
                "bedrooms" to bedrooms.toLong(),
                "bathrooms" to bathrooms.toLong(),
                "areaSqm" to areaSqm.toLong(),
                "description" to description.trim().take(1000),
                "imageKey" to validImageKey,
                "agentName" to cleanAgentName,
                "agentAgency" to cleanAgency,
                "businessHotline" to cleanHotline,
                "telegramHandle" to telegramHandle.trim().take(50),
                "visibility" to visibility.raw,
                "status" to PropertyStatus.AVAILABLE.raw,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProperty(
        propertyId: String,
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
    ): Result<Unit> {
        val docPath = "properties/$propertyId"
        return try {
            requireUserId()
            require(propertyId.isNotBlank()) { "Property ID missing" }
            val cleanTitle = title.trim()
            val cleanDistrict = district.trim()
            val cleanCity = city.trim()
            val cleanAgentName = agentName.trim()
            val cleanAgency = agentAgency.trim()
            val cleanHotline = businessHotline.trim()

            require(cleanTitle.length in 2..120) { "ចំណងជើងត្រូវមានពី 2 ដល់ 120 តួអក្សរ" }
            require(priceUsd in 1..100_000_000) { "តម្លៃមិនត្រឹមត្រូវ" }
            require(cleanDistrict.length in 1..60) { "សូមបញ្ចូលខណ្ឌ/ស្រុក" }
            require(cleanCity.length in 1..60) { "សូមបញ្ចូលរាជធានី/ខេត្ត" }
            require(bedrooms in 0..30) { "ចំនួនបន្ទប់គេងមិនត្រឹមត្រូវ" }
            require(bathrooms in 0..30) { "ចំនួនបន្ទប់ទឹកមិនត្រឹមត្រូវ" }
            require(areaSqm in 1..100_000) { "ទំហំត្រូវធំជាង 0 m²" }
            require(cleanAgentName.length in 1..60) { "សូមបញ្ចូលឈ្មោះភ្នាក់ងារ" }
            require(cleanAgency.length in 1..60) { "សូមបញ្ចូលឈ្មោះក្រុមហ៊ុន/ភ្នាក់ងារ" }
            require(cleanHotline.length in 1..30) { "សូមបញ្ចូលលេខទូរសព្ទ" }

            db.collection("properties").document(propertyId).update(
                mapOf(
                    "title" to cleanTitle,
                    "listingType" to listingType.raw,
                    "propertyType" to category.raw,
                    "priceUsd" to priceUsd,
                    "district" to cleanDistrict,
                    "city" to cleanCity,
                    "bedrooms" to bedrooms.toLong(),
                    "bathrooms" to bathrooms.toLong(),
                    "areaSqm" to areaSqm.toLong(),
                    "description" to description.trim().take(1000),
                    "imageKey" to ImagePreset.fromKey(imageKey).key,
                    "agentName" to cleanAgentName,
                    "agentAgency" to cleanAgency,
                    "businessHotline" to cleanHotline,
                    "telegramHandle" to telegramHandle.trim().take(50),
                    "visibility" to visibility.raw,
                    "status" to status.raw,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePropertyStatus(propertyId: String, status: PropertyStatus): Result<Unit> {
        val docPath = "properties/$propertyId"
        return try {
            requireUserId()
            require(propertyId.isNotBlank()) { "Property ID missing" }
            db.collection("properties").document(propertyId).update(
                mapOf(
                    "status" to status.raw,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProperty(propertyId: String): Result<Unit> {
        val docPath = "properties/$propertyId"
        return try {
            requireUserId()
            require(propertyId.isNotBlank()) { "Property ID missing" }
            db.collection("properties").document(propertyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- FAVORITES ---

    fun observeFavoriteIds(userId: String): Flow<Set<String>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteProperty::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.propertyId.ifBlank { it.id } }.toSet()
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun toggleFavorite(propertyId: String, currentlyFavorited: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val docPath = "users/$uid/favorites/$propertyId"
        return try {
            require(propertyId.isNotBlank()) { "Property ID missing" }
            val docRef = db.collection("users").document(uid)
                .collection("favorites").document(propertyId)
            if (currentlyFavorited) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "propertyId" to propertyId,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (currentlyFavorited) OperationType.DELETE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    // --- INQUIRIES / VIEWING REQUESTS ---

    fun observeInquiries(userId: String): Flow<List<AgentInquiry>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "inquiries"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AgentInquiry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun sendInquiry(
        property: PropertyListing,
        buyerName: String,
        buyerPhone: String,
        inquiryType: InquiryType,
        preferredDate: String,
        message: String,
    ): Result<Unit> {
        var docPath = "inquiries"
        return try {
            val uid = requireUserId()
            val cleanBuyerName = buyerName.trim()
            val cleanPhone = buyerPhone.trim()
            val cleanMessage = message.trim()
            val cleanDate = preferredDate.trim().take(40)

            require(property.id.isNotBlank() && property.ownerId.isNotBlank()) { "ព័ត៌មានអចលនទ្រព្យមិនពេញលេញ" }
            require(cleanBuyerName.length in 1..60) { "សូមបញ្ចូលឈ្មោះរបស់អ្នក" }
            require(cleanPhone.length in 1..30) { "សូមបញ្ចូលលេខទូរសព្ទទំនាក់ទំនង" }
            require(cleanMessage.length in 1..500) { "សូមសរសេរសារទៅកាន់ភ្នាក់ងារ" }

            val docRef = db.collection("inquiries").document()
            docPath = docRef.path
            val participants = listOf(uid, property.ownerId).distinct()

            val payload = mapOf(
                "buyerId" to uid,
                "buyerName" to cleanBuyerName,
                "buyerPhone" to cleanPhone,
                "agentId" to property.ownerId,
                "agentName" to property.agentName.ifBlank { "ភ្នាក់ងារ" }.take(60),
                "propertyId" to property.id,
                "propertyTitle" to property.title.take(120),
                "inquiryType" to inquiryType.raw,
                "preferredDate" to cleanDate,
                "message" to cleanMessage,
                "status" to InquiryStatus.PENDING.raw,
                "participantIds" to participants,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun replyToInquiry(
        inquiryId: String,
        agentReply: String,
        newStatus: InquiryStatus,
    ): Result<Unit> {
        val docPath = "inquiries/$inquiryId"
        return try {
            requireUserId()
            require(inquiryId.isNotBlank()) { "Inquiry ID missing" }
            val cleanReply = agentReply.trim().take(500)
            db.collection("inquiries").document(inquiryId).update(
                mapOf(
                    "agentReply" to cleanReply,
                    "status" to newStatus.raw,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun closeInquiryByBuyer(inquiryId: String): Result<Unit> {
        val docPath = "inquiries/$inquiryId"
        return try {
            requireUserId()
            require(inquiryId.isNotBlank()) { "Inquiry ID missing" }
            db.collection("inquiries").document(inquiryId).update(
                mapOf(
                    "status" to InquiryStatus.CLOSED.raw,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- AGENT REVIEWS & RATINGS ---

    fun observePublicReviews(currentUserId: String): Flow<List<AgentReview>> = flow {
        val path = "agent_reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AgentReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, currentUserId)
                    throw error
                }
        )
    }

    suspend fun submitAgentReview(
        agentId: String,
        agentName: String,
        reviewerName: String,
        rating: Int,
        comment: String,
    ): Result<Unit> {
        var docPath = "agent_reviews"
        return try {
            val uid = requireUserId()
            val cleanReviewer = reviewerName.trim().ifBlank { "អតិថិជន" }.take(60)
            val cleanAgent = agentName.trim().ifBlank { "ភ្នាក់ងារ" }.take(60)
            val cleanComment = comment.trim()

            require(agentId.isNotBlank()) { "Agent ID missing" }
            require(rating in 1..5) { "សូមជ្រើសរើសផ្កាយពី 1 ដល់ 5" }
            require(cleanComment.length in 1..500) { "សូមសរសេរមតិយោបល់ (1-500 តួអក្សរ)" }

            val docRef = db.collection("agent_reviews").document()
            docPath = docRef.path
            val payload = mapOf(
                "agentId" to agentId,
                "agentName" to cleanAgent,
                "reviewerId" to uid,
                "reviewerName" to cleanReviewer,
                "rating" to rating.toLong(),
                "comment" to cleanComment,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAgentReview(reviewId: String): Result<Unit> {
        val docPath = "agent_reviews/$reviewId"
        return try {
            requireUserId()
            require(reviewId.isNotBlank()) { "Review ID missing" }
            db.collection("agent_reviews").document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- DIRECT MESSAGING (REAL-TIME CHAT) ---

    fun observeChatThreads(userId: String): Flow<List<ChatThread>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeChatMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        require(chatId.isNotBlank() && userId.isNotBlank()) { "Chat ID and User ID must not be blank" }
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun openOrStartPropertyChat(
        property: PropertyListing,
        buyerName: String,
    ): Result<String> {
        var docPath = "chats"
        return try {
            val uid = requireUserId()
            require(property.id.isNotBlank() && property.ownerId.isNotBlank()) { "ព័ត៌មានអចលនទ្រព្យមិនពេញលេញ" }

            val safePropId = property.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val safeBuyerId = uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val safeAgentId = property.ownerId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val chatId = "${safePropId}__${safeBuyerId}__${safeAgentId}".take(128)
            docPath = "chats/$chatId"

            val chatRef = db.collection("chats").document(chatId)
            val existing = chatRef.get().await()
            if (!existing.exists()) {
                val participants = listOf(uid, property.ownerId).distinct()
                val payload = mapOf(
                    "buyerId" to uid,
                    "buyerName" to buyerName.trim().ifBlank { "អតិថិជន" }.take(60),
                    "agentId" to property.ownerId,
                    "agentName" to property.agentName.trim().ifBlank { "ភ្នាក់ងារ" }.take(60),
                    "propertyId" to property.id,
                    "propertyTitle" to property.title.take(120),
                    "lastMessage" to "បានចាប់ផ្តើមការសន្ទនាអំពី ${property.title}".take(500),
                    "lastSenderId" to uid,
                    "participantIds" to participants,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                chatRef.set(payload).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chatThread: ChatThread,
        senderName: String,
        text: String,
        messageType: ChatMessageType = ChatMessageType.TEXT,
        preferredDate: String = "",
    ): Result<Unit> {
        val docPath = "chats/${chatThread.id}/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(chatThread.id.isNotBlank()) { "Chat ID missing" }
            require(cleanText.length in 1..1000) { "សូមបញ្ចូលសារ (1-1000 តួអក្សរ)" }

            val cleanSender = senderName.trim().ifBlank { "អ្នកប្រើប្រាស់" }.take(60)
            val cleanDate = preferredDate.trim().take(40)
            val participants = chatThread.participantIds.ifEmpty {
                listOf(chatThread.buyerId, chatThread.agentId).distinct()
            }

            val chatRef = db.collection("chats").document(chatThread.id)
            val msgRef = chatRef.collection("messages").document()

            val msgPayload = mapOf(
                "chatId" to chatThread.id,
                "senderId" to uid,
                "senderName" to cleanSender,
                "text" to cleanText,
                "messageType" to messageType.raw,
                "preferredDate" to cleanDate.takeIf { it.isNotBlank() },
                "participantIds" to participants,
                "createdAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null }

            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- STARTER PORTFOLIO ---

    suspend fun seedStarterPortfolio(
        agentName: String,
        businessHotline: String,
        telegramHandle: String,
    ): Result<Unit> {
        val path = "properties"
        return try {
            val uid = requireUserId()
            val cleanAgent = agentName.trim().ifBlank { "ភ្នាក់ងារ ផ្ទះខ្មែរ" }.take(60)
            val cleanHotline = businessHotline.trim().ifBlank { "012 888 999" }.take(30)
            val cleanTelegram = telegramHandle.trim().ifBlank { "@khmerhome_agent" }.take(50)

            val starterItems = listOf(
                mapOf(
                    "title" to "វីឡាទំនើបមានអាងហែលទឹក បឹងកេងកង ១",
                    "listingType" to ListingType.SALE.raw,
                    "propertyType" to PropertyCategory.VILLA.raw,
                    "priceUsd" to 420000L,
                    "district" to "បឹងកេងកង ១",
                    "city" to "ភ្នំពេញ",
                    "bedrooms" to 5L,
                    "bathrooms" to 6L,
                    "areaSqm" to 380L,
                    "description" to "វីឡាប្រណីតកណ្តាលក្រុងភ្នំពេញ មានអាងហែលទឹកឯកជន សួនច្បារបៃតង ចំណតឡាន ៣ គ្រឿង និងសន្តិសុខ ២៤ ម៉ោង។ មានប្លង់រឹងស្របច្បាប់។",
                    "imageKey" to ImagePreset.VILLA_LUXURY.key,
                ),
                mapOf(
                    "title" to "ខុនដូទេសភាពទន្លេ ជ្រោយចង្វារ",
                    "listingType" to ListingType.RENT.raw,
                    "propertyType" to PropertyCategory.CONDO.raw,
                    "priceUsd" to 850L,
                    "district" to "ជ្រោយចង្វារ",
                    "city" to "ភ្នំពេញ",
                    "bedrooms" to 2L,
                    "bathrooms" to 2L,
                    "areaSqm" to 92L,
                    "description" to "បន្ទប់ជួលជាន់ទី ១៨ បំពាក់គ្រឿងសង្ហារិមពេញលេញ វេរ៉ង់ដាធំទូលាយមើលឃើញទន្លេមេគង្គ រួមទាំងក្លឹបហាត់ប្រាណ និងអាងហែលទឹកលើដំបូល។",
                    "imageKey" to ImagePreset.CONDO_SKYLINE.key,
                ),
                mapOf(
                    "title" to "ផ្ទះវីឡាកូនកាត់ បុរីសែនសុខ",
                    "listingType" to ListingType.SALE.raw,
                    "propertyType" to PropertyCategory.BOREY.raw,
                    "priceUsd" to 165000L,
                    "district" to "សែនសុខ",
                    "city" to "ភ្នំពេញ",
                    "bedrooms" to 4L,
                    "bathrooms" to 4L,
                    "areaSqm" to 180L,
                    "description" to "ផ្ទះថ្មីស្អាតជិតផ្សារទំនើប AEON Mall 2 សាលារៀនអន្តរជាតិ និងមន្ទីរពេទ្យ។ ផ្លូវធំ ១២ ម៉ែត្រ ងាយស្រួលរស់នៅជាលក្ខណៈគ្រួសារ។",
                    "imageKey" to ImagePreset.BOREY_TOWNHOUSE.key,
                ),
                mapOf(
                    "title" to "អាផាតមិនប្រណីត ទួលគោក",
                    "listingType" to ListingType.RENT.raw,
                    "propertyType" to PropertyCategory.APARTMENT.raw,
                    "priceUsd" to 480L,
                    "district" to "ទួលគោក",
                    "city" to "ភ្នំពេញ",
                    "bedrooms" to 1L,
                    "bathrooms" to 1L,
                    "areaSqm" to 58L,
                    "description" to "អាផាតមិនស្អាត ស្ងប់ស្ងាត់ មានចំណតម៉ូតូ-ឡាន សេវាសម្អាតប្រចាំសប្តាហ៍ និងជិតសាកលវិទ្យាល័យ។",
                    "imageKey" to ImagePreset.CONDO_SKYLINE.key,
                ),
                mapOf(
                    "title" to "ដីឡូតិ៍ជាប់ផ្លូវធំ សាលាកំរើក",
                    "listingType" to ListingType.SALE.raw,
                    "propertyType" to PropertyCategory.LAND.raw,
                    "priceUsd" to 95000L,
                    "district" to "សាលាកំរើក",
                    "city" to "សៀមរាប",
                    "bedrooms" to 0L,
                    "bathrooms" to 0L,
                    "areaSqm" to 500L,
                    "description" to "ដីទំហំ ២០ម x ២៥ម ជាប់ផ្លូវបេតុង ១៥ ម៉ែត្រ ទីតាំងល្អសម្រាប់សាងសង់វីឡា ឬប៊ូទិកហូតែល នៅកណ្តាលក្រុងសៀមរាប។",
                    "imageKey" to ImagePreset.LAND_PLOT.key,
                ),
            )

            val batch = db.batch()
            for (item in starterItems) {
                val docRef = db.collection(path).document()
                val payload = item + mapOf(
                    "ownerId" to uid,
                    "agentName" to cleanAgent,
                    "agentAgency" to "ផ្ទះខ្មែរ អចលនទ្រព្យ",
                    "businessHotline" to cleanHotline,
                    "telegramHandle" to cleanTelegram,
                    "visibility" to PropertyVisibility.PUBLIC.raw,
                    "status" to PropertyStatus.AVAILABLE.raw,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, operationType, path)
        }
    }
}
