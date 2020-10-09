package xyz.deverse.evendilo

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry

inline fun <reified T> logger(): Logger {
	return LoggerFactory.getLogger(T::class.java)
}


@SpringBootApplication
@EnableConfigurationProperties
@EnableRetry
@EnableJpaRepositories
class EvendiloCoreApplication

fun main(args: Array<String>) {
	runApplication<EvendiloCoreApplication>(*args)
}