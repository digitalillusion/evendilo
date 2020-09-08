package xyz.deverse.evendilo.config;

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfigurationSource
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import xyz.deverse.evendilo.config.support.ResilientOAuth2AuthorizationRequestResolver
import xyz.deverse.evendilo.config.support.ResilientTokenResponseConverter
import xyz.deverse.evendilo.config.support.SessionMapper


@Configuration
@RestController
@EnableWebSecurity
class WebSecurityConfiguration(
        var appConfigProperties: AppConfigurationProperties,
        var corsConfigurationSource: CorsConfigurationSource,
        var sessionAuthenticationStrategy: SessionAuthenticationStrategy,
        var clientRegistrationRepository: ClientRegistrationRepository,
        var sessionMappers: Collection<SessionMapper>
) : WebSecurityConfigurerAdapter() {

  val defaultTargetUrl = appConfigProperties.corsAllowedOrigin + "/"

  @RequestMapping("/session")
  fun session(@AuthenticationPrincipal token: OAuth2AuthenticationToken?): Map<String, Any> {
    if (token == null) {
      return mapOf()
    }
    val (clientId, destination) = token.authorizedClientRegistrationId.split("-")
    val mapped = mutableMapOf(
        Pair("clientId", clientId),
        Pair("destination", destination)
    )
    for (sessionMapper in sessionMappers) {
      if (sessionMapper.canHandle(destination)) {
        sessionMapper.map(token.principal as DefaultOAuth2User).forEach { p -> mapped[p.key] = p.value }
      }
    }
    return mapped.toMap()
  }

  fun logoutSuccessHandler() : OidcClientInitiatedLogoutSuccessHandler {
    var handler = OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)
    handler.setPostLogoutRedirectUri(defaultTargetUrl)
    return handler
  }


  @Bean
  fun accessTokenResponseClient(): OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest?>? {
    val accessTokenResponseClient = DefaultAuthorizationCodeTokenResponseClient()
    accessTokenResponseClient.setRequestEntityConverter(OAuth2AuthorizationCodeGrantRequestEntityConverter())
    val tokenResponseHttpMessageConverter = OAuth2AccessTokenResponseHttpMessageConverter()
    tokenResponseHttpMessageConverter.setTokenResponseConverter(ResilientTokenResponseConverter())
    val restTemplate = RestTemplate(listOf(
            FormHttpMessageConverter(), tokenResponseHttpMessageConverter))
    restTemplate.errorHandler = OAuth2ErrorResponseErrorHandler()
    accessTokenResponseClient.setRestOperations(restTemplate)
    return accessTokenResponseClient
  }

  @Bean
  fun authorizationRequestResolver(): OAuth2AuthorizationRequestResolver? {
    return ResilientOAuth2AuthorizationRequestResolver(clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI)
  }

  override fun configure(http: HttpSecurity) {
    http
      .cors()
      .configurationSource(corsConfigurationSource)
      .and()
      .sessionManagement()
        .sessionAuthenticationStrategy(sessionAuthenticationStrategy)
      .and()
      .authorizeRequests { a ->
        a.antMatchers("/", "/error", "/webjars/**", "/login/**", "/session", "/logout").permitAll()
         .anyRequest().authenticated()
      }
      .exceptionHandling { e -> e.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
      //.csrf { c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) }
      .csrf().disable()
      .logout()
        .logoutSuccessHandler(logoutSuccessHandler())
        .clearAuthentication(true)
        .deleteCookies()
        .invalidateHttpSession(true)
      .and()
      .oauth2Login()
        .tokenEndpoint()
          .accessTokenResponseClient(accessTokenResponseClient())
        .and()
        .authorizationEndpoint()
          .authorizationRequestResolver(authorizationRequestResolver())
        .and()
        .successHandler(SimpleUrlAuthenticationSuccessHandler(defaultTargetUrl))
  }

}
