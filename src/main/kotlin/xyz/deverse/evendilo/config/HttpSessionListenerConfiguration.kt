package xyz.deverse.evendilo.config

import org.springframework.context.annotation.Configuration
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener

@Configuration
class HttpSessionListenerConfiguration : HttpSessionListener {
    override fun sessionCreated(event: HttpSessionEvent) {
        event.session.maxInactiveInterval = -1
    }
}