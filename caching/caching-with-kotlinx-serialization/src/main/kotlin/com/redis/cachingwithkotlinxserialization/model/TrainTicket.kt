package com.redis.cachingwithkotlinxserialization.model

import kotlinx.serialization.Serializable

@Serializable
data class TrainTicket(
    val trainName: String,
    val trainType: TrainType,
    val trainNumber: Int,
    val isDelayed: Boolean,
)

@Serializable
enum class TrainType {
    LOCAL,
    EXPRESS,
    INTERCITY,
    FREIGHT,
    HIGH_SPEED,
    REGIONAL,
    OTHER
}