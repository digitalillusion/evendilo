package xyz.deverse.evendilo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
class RestConfiguration {
    @Bean
    fun localValidatorFactoryBean(): javax.validation.Validator {
        return LocalValidatorFactoryBean()
    }
}