package com.redis.cachingwithkotlinxserialization

import com.redis.cachingwithkotlinxserialization.service.TrainTicketService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TrainScheduleTestRunner(
    private val trainScheduleService: TrainTicketService
) : CommandLineRunner {

    override fun run(vararg args: String?) {

            val trainNumber = "12345"

            println("Fetching train schedule first time...")
            val firstCall = trainScheduleService.getTrainSchedule(trainNumber)
            println("Result: $firstCall")

            println("Fetching train schedule second time (should hit cache)...")
            val secondCall = trainScheduleService.getTrainSchedule(trainNumber)
            println("Result: $secondCall")
        }

}