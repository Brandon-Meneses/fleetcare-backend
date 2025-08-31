package com.tuorg.fleetcare.api.dto


data class NotificationDto(
    val id: Long,
    val title: String,
    val content: String,
    val link: String?,
    val createdAt: String // ISO8601
)

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)