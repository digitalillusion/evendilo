package xyz.deverse.evendilo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer
import org.springframework.session.MapSession
import org.springframework.session.MapSessionRepository
import org.springframework.session.web.socket.server.SessionRepositoryMessageInterceptor
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import java.util.*

@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
@Import(HttpSessionConfiguration::class)
class WebSocketSecurityConfig(
        @param:Value("\${messaging.sockjs.url:https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.4.0/sockjs.js}") private val clientLibraryUrl: String,
        @param:Value("\${messaging.broker.endpoint:/messages}") private val messagingBrokerEndpoint: String,
        @param:Value("\${messaging.broker.prefix:/app}") private val messagingBrokerPrefix: String,
        @param:Value("\${messaging.broker.topic:/topic}") private val messagingBrokerTopic: String,
        private val sessionRepository: MapSessionRepository,
        private val corsConfigurationSource: CorsConfigurationSource
) : AbstractSecurityWebSocketMessageBrokerConfigurer() {
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker(messagingBrokerTopic)
        config.setApplicationDestinationPrefixes(messagingBrokerPrefix)
    }

    override fun customizeClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(sessionRepositoryMessageInterceptor())
    }

    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages.anyMessage().authenticated()
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val corsAllowedOrigins: MutableList<String> = ArrayList()
        if (UrlBasedCorsConfigurationSource::class.java.isAssignableFrom(corsConfigurationSource.javaClass)) {
            val corsConfigurations = (corsConfigurationSource as UrlBasedCorsConfigurationSource).corsConfigurations
            corsConfigurations.forEach { (_: String?, conf: CorsConfiguration) -> conf.allowedOrigins!!.stream().forEach { e: String -> corsAllowedOrigins.add(e) } }
        }
        registry.addEndpoint(messagingBrokerEndpoint)
                .setAllowedOrigins(*corsAllowedOrigins.toTypedArray())
                .withSockJS()
                .setClientLibraryUrl(clientLibraryUrl)
                .setInterceptors(sessionRepositoryMessageInterceptor())
    }

    override fun sameOriginDisabled(): Boolean {
        return true
    }

    @Bean
    fun sessionRepositoryMessageInterceptor(): SessionRepositoryMessageInterceptor<MapSession> {
        return SessionRepositoryMessageInterceptor(sessionRepository)
    }

}