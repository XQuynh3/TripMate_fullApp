package com.example.notingapp.network

import com.example.notingapp.model.NoteDTO
import com.example.notingapp.model.ShareRequestDTO
import com.example.notingapp.model.SignupRequest
import com.example.notingapp.model.LoginRequest
import com.example.notingapp.model.TripMateAuthResponse
import com.example.notingapp.model.SuggestionResponse
import com.example.notingapp.model.Trip
import com.example.notingapp.model.CreateTripRequest
import com.example.notingapp.model.ChecklistUpdateRequest
import com.example.notingapp.model.ShareTripRequest
import com.example.notingapp.model.ShareTripResponse
import com.example.notingapp.model.AddChecklistRequest
import com.example.notingapp.model.DeleteChecklistRequest
import com.example.notingapp.model.DeleteChecklistResponse
import com.example.notingapp.model.UpdateTripRequest
import com.example.notingapp.model.AssignChecklistRequest
import com.example.notingapp.model.LocationReminderRequest
import com.example.notingapp.model.CreateLocationReminderResponse
import com.example.notingapp.model.ActiveReminder
import com.example.notingapp.model.TriggerReminderRequest
import com.example.notingapp.model.RatingRequest
import com.example.notingapp.model.RatingSummary
import com.example.notingapp.model.InviteCodeResponse
import com.example.notingapp.model.JoinTripRequestBody
import com.example.notingapp.model.JoinTripResponse
import com.example.notingapp.model.TripRequestsResponse
import com.example.notingapp.model.AcceptRejectRequestBody
import com.example.notingapp.model.TriggerReminderResponse
import com.example.notingapp.model.AddPlanItemRequest
import com.example.notingapp.model.UpdatePlanItemRequest
import com.example.notingapp.model.DeletePlanItemRequest
import com.example.notingapp.model.DeletePlanItemResponse
import com.example.notingapp.model.GeneratePlanRequest
import com.example.notingapp.model.GeneratePlanResponse
import com.example.notingapp.model.SearchResultItem
import com.example.notingapp.model.PlaceReviewDetail
import retrofit2.http.*

interface ApiService {

    @POST("notes")
    suspend fun createNote(@Body note: NoteDTO)

    @GET("notes")
    suspend fun getNotes(
        @Query("userId") userId: String
    ): List<NoteDTO>

    @POST("share-request")
    suspend fun sendShareRequest(@Body request: ShareRequestDTO)

    @GET("share-request")
    suspend fun getRequests(
        @Query("userId") userId: String
    ): List<ShareRequestDTO>

    @POST("share-request/accept")
    suspend fun acceptRequest(@Body body: Map<String, String>)

    @POST("share-request/reject")
    suspend fun rejectRequest(@Body body: Map<String, String>)

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): TripMateAuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TripMateAuthResponse

    @GET("suggestions")
    suspend fun getSuggestions(
        @Query("destination") destination: String
    ): SuggestionResponse

    @POST("trips/from-suggestion")
    suspend fun createTripFromSuggestion(
        @Body request: CreateTripRequest
    ): Trip

    @GET("trips")
    suspend fun getTrips(
        @Query("userId") userId: String
    ): List<Trip>

    @GET("trips/{id}")
    suspend fun getTripDetail(
        @Path("id") tripId: String
    ): Trip

    @PUT("trips/{tripId}")
    suspend fun updateTrip(
        @Path("tripId") tripId: String,
        @Body request: UpdateTripRequest
    ): Trip

    @DELETE("trips/{tripId}")
    suspend fun deleteTrip(
        @Path("tripId") tripId: String
    ): Map<String, String>

    @POST("trips/{tripId}/share")
    suspend fun shareTrip(
        @Path("tripId") tripId: String,
        @Body request: ShareTripRequest
    ): ShareTripResponse

    @POST("trips/{tripId}/checklist")
    suspend fun addChecklistItem(
        @Path("tripId") tripId: String,
        @Body request: AddChecklistRequest
    ): Trip

    @PATCH("trips/{tripId}/checklist/{itemId}")
    suspend fun updateChecklist(
        @Path("tripId") tripId: String,
        @Path("itemId") itemId: String,
        @Body request: ChecklistUpdateRequest
    ): Trip

    @PATCH("trips/{tripId}/checklist/{itemId}")
    suspend fun assignChecklistItem(
        @Path("tripId") tripId: String,
        @Path("itemId") itemId: String,
        @Body request: AssignChecklistRequest
    ): Trip

    @HTTP(method = "DELETE", path = "trips/{tripId}/checklist/{itemId}", hasBody = true)
    suspend fun deleteChecklistItem(
        @Path("tripId") tripId: String,
        @Path("itemId") itemId: String,
        @Body request: DeleteChecklistRequest
    ): DeleteChecklistResponse

    @POST("trips/{tripId}/location-reminders")
    suspend fun createLocationReminder(
        @Path("tripId") tripId: String,
        @Body request: LocationReminderRequest
    ): CreateLocationReminderResponse

    @PATCH("trips/{tripId}/location-reminders/{reminderId}")
    suspend fun updateLocationReminder(
        @Path("tripId") tripId: String,
        @Path("reminderId") reminderId: String,
        @Body request: LocationReminderRequest
    ): CreateLocationReminderResponse

    @GET("users/{userId}/location-reminders")
    suspend fun getActiveReminders(
        @Path("userId") userId: String
    ): List<ActiveReminder>

    @PATCH("trips/{tripId}/location-reminders/{reminderId}/trigger")
    suspend fun triggerReminder(
        @Path("tripId") tripId: String,
        @Path("reminderId") reminderId: String,
        @Body request: TriggerReminderRequest
    ): TriggerReminderResponse

    @POST("trips/{tripId}/ratings")
    suspend fun submitRating(
        @Path("tripId") tripId: String,
        @Body request: RatingRequest
    ): Trip

    @GET("ratings/summary")
    suspend fun getRatingSummary(): List<RatingSummary>

    @GET("ratings/summary")
    suspend fun getRatingSummaryForPlace(
        @Query("placeName") placeName: String
    ): List<RatingSummary>

    @GET("trips/{tripId}/invite-code")
    suspend fun getInviteCode(
        @Path("tripId") tripId: String
    ): InviteCodeResponse

    @POST("trips/join-request")
    suspend fun sendJoinRequest(
        @Body request: JoinTripRequestBody
    ): JoinTripResponse

    @GET("trip-requests")
    suspend fun getTripRequests(
        @Query("userId") userId: String
    ): TripRequestsResponse

    @POST("trip-requests/{requestId}/accept")
    suspend fun acceptTripRequest(
        @Path("requestId") requestId: String,
        @Body request: AcceptRejectRequestBody
    ): JoinTripResponse

    @POST("trip-requests/{requestId}/reject")
    suspend fun rejectTripRequest(
        @Path("requestId") requestId: String,
        @Body request: AcceptRejectRequestBody
    ): JoinTripResponse

    @POST("trips/{tripId}/plan-items")
    suspend fun addPlanItem(
        @Path("tripId") tripId: String,
        @Body request: AddPlanItemRequest
    ): Trip

    @PATCH("trips/{tripId}/plan-items/{planItemId}")
    suspend fun updatePlanItem(
        @Path("tripId") tripId: String,
        @Path("planItemId") planItemId: String,
        @Body request: UpdatePlanItemRequest
    ): Trip

    @HTTP(method = "DELETE", path = "trips/{tripId}/plan-items/{planItemId}", hasBody = true)
    suspend fun deletePlanItem(
        @Path("tripId") tripId: String,
        @Path("planItemId") planItemId: String,
        @Body request: DeletePlanItemRequest
    ): DeletePlanItemResponse

    @POST("trips/{tripId}/generate-plan")
    suspend fun generatePlan(
        @Path("tripId") tripId: String,
        @Body request: GeneratePlanRequest
    ): GeneratePlanResponse

    @GET("search")
    suspend fun smartSearch(
        @Query("q") query: String
    ): List<SearchResultItem>

    @GET("reviews/place")
    suspend fun getPlaceReviewDetail(
        @Query("placeName") placeName: String
    ): PlaceReviewDetail
}