package com.tuorg.fleetcare

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FleetcareBackendApplication

fun main(args: Array<String>) {
	runApplication<FleetcareBackendApplication>(*args)
}
