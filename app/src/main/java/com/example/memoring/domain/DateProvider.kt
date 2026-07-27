package com.example.memoring.domain

import java.time.Clock
import java.time.LocalDate

fun interface DateProvider {
    fun today(): LocalDate
}

class ClockDateProvider(private val clock: Clock = Clock.systemDefaultZone()) : DateProvider {
    override fun today(): LocalDate = LocalDate.now(clock)
}
