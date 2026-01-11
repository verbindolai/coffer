package org.coffer.coffer2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
class Coffer2Application

fun main(args: Array<String>) {
    runApplication<Coffer2Application>(*args)
}
