package xyz.deverse.evendilo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

@Configuration
class CorsSecurityConfiguration : WebMvcConfigurer {

	@Value("\${variables.cors-allowed-origin}")
	lateinit var corsAllowedOrigin : String;

    override fun configureContentNegotiation(configurer : ContentNegotiationConfigurer) {
        configurer.
                favorParameter(false).
                ignoreAcceptHeader(true).
                defaultContentType(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON);
    }
    
	
	@Bean
    @Primary
    fun corsFilter() : CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.allowedOrigins = Collections.singletonList(corsAllowedOrigin)
        config.allowedHeaders = listOf("Origin", "Content-Type", "Accept")
        config.allowedMethods = listOf("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH")
        source.registerCorsConfiguration("/**", config)
        return source
    }
	

}