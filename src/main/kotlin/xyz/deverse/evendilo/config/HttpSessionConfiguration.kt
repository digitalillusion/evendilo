package xyz.deverse.evendilo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.session.MapSessionRepository
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer
import java.util.concurrent.ConcurrentHashMap


@Configuration
@EnableSpringHttpSession
class HttpSessionConfiguration {
    @Bean
    fun sessionRegistry(): SessionRegistry {
        return SessionRegistryImpl()
    }

    @Bean
    fun sessionRepository(): MapSessionRepository {
        return MapSessionRepository(ConcurrentHashMap())
    }

    @Bean
    fun cookieSerializer(): CookieSerializer {
        val serializer = DefaultCookieSerializer()
        // serializer.setCookieName("JSESSIONID")
        // serializer.setCookiePath("/")
        // serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$")
        
        serializer.setSameSite("None")
        return serializer
    }

    @Bean
    protected fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
        return RegisterSessionAuthenticationStrategy(sessionRegistry())
    }
}