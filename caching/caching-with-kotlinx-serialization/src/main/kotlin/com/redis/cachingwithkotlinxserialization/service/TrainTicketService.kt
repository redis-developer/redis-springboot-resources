package com.redis.cachingwithkotlinxserialization.service

import com.redis.cachingwithkotlinxserialization.model.TrainTicket
import com.redis.cachingwithkotlinxserialization.repository.TrainTicketRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class TrainTicketService(
    private val trainTicketRepository: TrainTicketRepository
) {

    @Cacheable("Schedule")
    fun getTrainSchedule(trainNumber: String): TrainTicket? {
        return trainTicketRepository.getSchedule(trainNumber)
    }
}