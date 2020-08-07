package xyz.deverse.evendilo.config;

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.cors.CorsConfigurationSource
import xyz.deverse.evendilo.config.properties.AppConfigurationProperties
import java.security.Principal

@Configuration
@RestController
@EnableWebSecurity
class WebSecurityConfiguration(
    var appConfigProperties: AppConfigurationProperties,
    var corsConfigurationSource: CorsConfigurationSource,
    var sessionAuthenticationStrategy: SessionAuthenticationStrategy,
    var clientRegistrationRepository: ClientRegistrationRepository
) : WebSecurityConfigurerAdapter() {

  val defaultTargetUrl = appConfigProperties.corsAllowedOrigin + "/"

  @RequestMapping("/session")
  fun session (@AuthenticationPrincipal principal: Principal?): Map<String, Any> {
    if (principal == null) {
      return mapOf()
    }
    return mapOf(Pair("name", principal.name))
  }

  fun logoutSuccessHandler() : OidcClientInitiatedLogoutSuccessHandler {
    var handler = OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)
    handler.setPostLogoutRedirectUri(defaultTargetUrl)
    return handler
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
    .csrf { c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) }
    .logout()
      .logoutSuccessHandler(logoutSuccessHandler())
      .clearAuthentication(true)
      .deleteCookies()
      .invalidateHttpSession(true)
    .and()
    .oauth2Login()
      .successHandler (SimpleUrlAuthenticationSuccessHandler(defaultTargetUrl))
  }
}
