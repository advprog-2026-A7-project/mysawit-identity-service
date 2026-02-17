package com.mysawit.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MysawitIdentityServiceApplication

fun main(args: Array<String>) {
    val context = runApplication<MysawitIdentityServiceApplication>(*args)
    if (context.environment.getProperty("app.test.close-context", Boolean::class.java, false)) {
        context.close()
    }
}
