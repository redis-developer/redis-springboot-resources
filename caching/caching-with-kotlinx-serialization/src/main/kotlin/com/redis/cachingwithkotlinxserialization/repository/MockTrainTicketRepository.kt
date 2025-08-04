package com.redis.cachingwithkotlinxserialization.repository

import com.redis.cachingwithkotlinxserialization.model.TrainTicket
import com.redis.cachingwithkotlinxserialization.model.TrainType
import org.springframework.stereotype.Repository

@Repository
class MockTrainTicketRepository : TrainTicketRepository {
    override fun getSchedule(trainNumber: String): TrainTicket {
        println("💾 REPOSITORY was called for $trainNumber")
        return TrainTicket(
            trainName = "Mock Express",
            trainType = TrainType.EXPRESS,
            trainNumber = trainNumber.toInt(),
            isDelayed = false
        )
    }
}