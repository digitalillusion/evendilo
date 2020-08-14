package xyz.deverse.evendilo.config;

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import java.util.*


@Configuration
class RestConfiguration(var appConfigProperties: AppConfigurationProperties) : WebMvcConfigurer {

    @Bean
    fun localValidatorFactoryBean(): javax.validation.Validator {
        return LocalValidatorFactoryBean()
    }


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
        config.allowedOrigins = Collections.singletonList(appConfigProperties.corsAllowedOrigin)
        config.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "X-XSRF-TOKEN")
        config.allowedMethods = listOf("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH")
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun apiRetryTemplate(): RetryTemplate {
        val simpleRetryPolicy = SimpleRetryPolicy()
        simpleRetryPolicy.maxAttempts = 2
        val backOffPolicy = FixedBackOffPolicy()
        backOffPolicy.backOffPeriod = 5000 // 5ms
        val template = RetryTemplate()

        val policyMap: MutableMap<Class<out Throwable?>, RetryPolicy> = HashMap()
        policyMap[ResourceAccessException::class.java] = simpleRetryPolicy
        val retryPolicy = ExceptionClassifierRetryPolicy()
        retryPolicy.setPolicyMap(policyMap)

        template.setRetryPolicy(retryPolicy)
        template.setBackOffPolicy(backOffPolicy)
        return template
    }

}