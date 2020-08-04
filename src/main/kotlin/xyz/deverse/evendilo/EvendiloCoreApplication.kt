package xyz.deverse.evendilo

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import xyz.deverse.evendilo.config.CorsSecurityConfiguration

inline fun <reified T> logger(): Logger {
	return LoggerFactory.getLogger(T::class.java)
}


@SpringBootApplication
@EnableConfigurationProperties
class EvendiloCoreApplication

fun main(args: Array<String>) {
	val config = arrayOf(EvendiloCoreApplication::class.java, CorsSecurityConfiguration::class.java)
	SpringApplication.run(config, args)
}
