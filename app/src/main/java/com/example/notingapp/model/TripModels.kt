package com.example.notingapp.model

data class Trip(
    val _id: String,
    val destination: String,
    val title: String,
    val ownerId: String,
    val members: List<String> = emptyList(),
    val inviteCode: String? = null,
    val note: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val defaultCategories: List<String> = emptyList(),
    val customCategories: List<String> = emptyList(),
    val places: List<Place> = emptyList(),
    val foods: List<FoodItem> = emptyList(),
    val checklist: List<TripChecklistItem> = emptyList(),
    val locationReminders: List<LocationReminder> = emptyList(),
    val planItems: List<PlanItem> = emptyList(),
    val updatedBy: String? = null,
    val updatedAt: Long? = null,
    val createdAt: Long? = null
)

data class TripChecklistItem(
    val _id: String,
    val title: String? = null,
    val text: String,
    val content: String? = null,
    val category: String,
    val done: Boolean = false,
    val assignedTo: String? = null,
    val reminderId: String? = null,
    val sourceType: String? = null,
    val sourceName: String? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class PlanItem(
    val _id: String? = null,
    val title: String,
    val note: String? = null,
    val type: String? = null,
    val startTime: String,
    val endTime: String? = null,
    val locationName: String? = null,
    val assignedTo: String? = null,
    val sourceChecklistId: String? = null,
    val sourceName: String? = null,
    val done: Boolean = false,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class CreateTripRequest(
    val destination: String,
    val userId: String,
    val title: String,
    val memberIds: List<String> = emptyList(),
    val note: String? = null,
    val uncheckedTexts: List<String> = emptyList()
)

data class ChecklistUpdateRequest(
    val userId: String,
    val done: Boolean? = null,
    val title: String? = null,
    val text: String? = null,
    val content: String? = null,
    val category: String? = null,
    val assignedTo: String? = null
)

data class ShareTripRequest(
    val fromUserId: String,
    val targetUserId: String
)

data class ShareTripResponse(
    val message: String? = null,
    val trip: Trip? = null
)

data class AddChecklistRequest(
    val text: String,
    val category: String,
    val assignedTo: String,
    val userId: String
)

data class DeleteChecklistRequest(
    val userId: String
)

data class DeleteChecklistResponse(
    val message: String? = null,
    val linkedPlanCount: Int? = null,
    val trip: Trip? = null
)

data class UpdateTripRequest(
    val title: String? = null,
    val note: String? = null,
    val members: List<String>? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val customCategories: List<String>? = null,
    val planItems: List<PlanItem>? = null,
    val updatedBy: String
)

data class AssignChecklistRequest(
    val assignedTo: String,
    val userId: String
)

data class LocationReminderRequest(
    val title: String,
    val message: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val userId: String,
    val checklistItemId: String? = null
)

data class LocationReminder(
    val _id: String,
    val title: String,
    val message: String? = null,
    val checklistItemId: String? = null,
    val locationName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int? = null,
    val enabled: Boolean? = null,
    val triggered: Boolean? = null,
    val createdBy: String? = null,
    val triggeredBy: String? = null,
    val createdAt: Long? = null,
    val triggeredAt: Long? = null,
    val updatedAt: Long? = null
)

data class CreateLocationReminderResponse(
    val message: String? = null,
    val reminder: LocationReminder? = null,
    val trip: Trip? = null
)

data class ActiveReminder(
    val tripId: String,
    val tripTitle: String? = null,
    val destination: String? = null,
    val reminder: LocationReminder
)

data class TriggerReminderRequest(
    val userId: String
)

data class RatingRequest(
    val placeName: String,
    val userId: String,
    val score: Int,
    val comment: String,
    val imageUrls: List<String> = emptyList(),
    val source: String = "manual",
    val reminderId: String? = null,
    val checklistItemId: String? = null,
    val planItemId: String? = null
)

data class RatingSummary(
    val _id: String,
    val averageScore: Double,
    val totalReviews: Int,
    val comments: List<RatingComment>? = null
)

data class RatingComment(
    val userId: String? = null,
    val comment: String? = null,
    val score: Int? = null,
    val createdAt: Long? = null
)

data class InviteCodeResponse(
    val tripId: String? = null,
    val inviteCode: String? = null
)

data class JoinTripRequestBody(
    val inviteCode: String,
    val userId: String,
    val message: String = ""
)

data class TripPreview(
    val _id: String,
    val title: String? = null,
    val destination: String? = null,
    val ownerId: String? = null
)

data class TripJoinRequest(
    val _id: String,
    val tripId: String,
    val tripTitle: String? = null,
    val destination: String? = null,
    val fromUserId: String,
    val toUserId: String,
    val type: String? = null,
    val inviteCode: String? = null,
    val status: String,
    val message: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class JoinTripResponse(
    val message: String? = null,
    val request: TripJoinRequest? = null,
    val trip: TripPreview? = null
)

data class TripRequestsResponse(
    val ownerRequests: List<TripJoinRequest> = emptyList(),
    val myRequests: List<TripJoinRequest> = emptyList()
)

data class AcceptRejectRequestBody(
    val ownerId: String
)

data class ReviewPrompt(
    val shouldAskReview: Boolean = false,
    val placeName: String? = null,
    val destination: String? = null,
    val source: String? = null,
    val reminderId: String? = null,
    val checklistItemId: String? = null
)

data class TriggerReminderResponse(
    val message: String? = null,
    val reminder: LocationReminder? = null,
    val trip: Trip? = null,
    val reviewPrompt: ReviewPrompt? = null
)

data class AddPlanItemRequest(
    val title: String,
    val note: String = "",
    val type: String = "custom",
    val startTime: String,
    val endTime: String = "",
    val locationName: String = "",
    val assignedTo: String = "",
    val sourceChecklistId: String = "",
    val sourceName: String = "",
    val userId: String
)

data class UpdatePlanItemRequest(
    val title: String? = null,
    val note: String? = null,
    val type: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val locationName: String? = null,
    val assignedTo: String? = null,
    val done: Boolean? = null,
    val userId: String
)

data class DeletePlanItemRequest(
    val userId: String
)

data class DeletePlanItemResponse(
    val message: String? = null,
    val trip: Trip? = null
)

data class GeneratePlanRequest(
    val userId: String,
    val overwrite: Boolean = false,
    val startDate: String = ""
)

data class GeneratePlanResponse(
    val message: String? = null,
    val trip: Trip? = null,
    val planItems: List<PlanItem> = emptyList(),
    val startDate: String? = null
)

data class SearchResultItem(
    val type: String = "",
    val name: String = "",
    val destination: String? = null,
    val subtitle: String? = null,
    val estimatedCost: String? = null,
    val reason: String? = null,
    val ratingAverage: Double? = null,
    val ratingCount: Int? = null
)

data class PlaceReviewDetail(
    val placeName: String = "",
    val averageScore: Double = 0.0,
    val totalReviews: Int = 0,
    val reviews: List<PlaceReviewItem> = emptyList()
)

data class PlaceReviewItem(
    val userId: String? = null,
    val displayName: String? = null,
    val avatarColor: String? = null,
    val score: Int? = null,
    val comment: String? = null,
    val source: String? = null,
    val destination: String? = null,
    val tripStatusAtReview: String? = null,
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long? = null
)