package com.redis.cachingwithkotlinxserialization.repository

import com.redis.cachingwithkotlinxserialization.model.TrainTicket

interface TrainTicketRepository {
    fun getSchedule(trainNumber: String): TrainTicket?
}