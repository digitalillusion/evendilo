package xyz.deverse.evendilo.config.support

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.web.util.UrlUtils
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.util.Assert
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest

/**
 * Copy of DefaultOAuth2AuthorizationRequestResolver that
 * does not use non alphanumeric characters for the state param. This way it is not subjected to urlencoding
 */
class ResilientOAuth2AuthorizationRequestResolver(clientRegistrationRepository: ClientRegistrationRepository,
                                                  authorizationRequestBaseUri: String) : OAuth2AuthorizationRequestResolver {
    private val clientRegistrationRepository: ClientRegistrationRepository
    private val authorizationRequestMatcher: AntPathRequestMatcher
    private val stateGenerator: StringKeyGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder())
    private val secureKeyGenerator: StringKeyGenerator = Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96)
    private var authorizationRequestCustomizer = Consumer<OAuth2AuthorizationRequest.Builder> { }
    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val registrationId = resolveRegistrationId(request)
        val redirectUriAction = getAction(request, "login")
        return resolve(request, registrationId, redirectUriAction)
    }

    override fun resolve(request: HttpServletRequest, registrationId: String?): OAuth2AuthorizationRequest? {
        if (registrationId == null) {
            return null
        }
        val redirectUriAction = getAction(request, "authorize")
        return resolve(request, registrationId, redirectUriAction)!!
    }

    /**
     * Sets the `Consumer` to be provided the [OAuth2AuthorizationRequest.Builder]
     * allowing for further customizations.
     *
     * @since 5.3
     * @param authorizationRequestCustomizer the `Consumer` to be provided the [OAuth2AuthorizationRequest.Builder]
     */
    fun setAuthorizationRequestCustomizer(authorizationRequestCustomizer: Consumer<OAuth2AuthorizationRequest.Builder>) {
        Assert.notNull(authorizationRequestCustomizer, "authorizationRequestCustomizer cannot be null")
        this.authorizationRequestCustomizer = authorizationRequestCustomizer
    }

    private fun getAction(request: HttpServletRequest, defaultAction: String): String {
        return request.getParameter("action") ?: return defaultAction
    }

    private fun resolve(request: HttpServletRequest, registrationId: String?, redirectUriAction: String): OAuth2AuthorizationRequest? {
        if (registrationId == null) {
            return null
        }
        val clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId)
                ?: throw IllegalArgumentException("Invalid Client Registration with Id: $registrationId")
        val attributes: MutableMap<String, Any> = HashMap()
        attributes[OAuth2ParameterNames.REGISTRATION_ID] = clientRegistration.registrationId
        val builder: OAuth2AuthorizationRequest.Builder
        when (clientRegistration.authorizationGrantType) {
            AuthorizationGrantType.CLIENT_CREDENTIALS,
            AuthorizationGrantType.AUTHORIZATION_CODE -> {
                builder = OAuth2AuthorizationRequest.authorizationCode()
                val additionalParameters: MutableMap<String, Any> = HashMap()
                if (!CollectionUtils.isEmpty(clientRegistration.scopes) &&
                        clientRegistration.scopes.contains(OidcScopes.OPENID)) {
                    // Section 3.1.2.1 Authentication Request - https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
                    // scope
                    // 		REQUIRED. OpenID Connect requests MUST contain the "openid" scope value.
                    addNonceParameters(attributes, additionalParameters)
                }
                if (ClientAuthenticationMethod.NONE == clientRegistration.clientAuthenticationMethod) {
                    addPkceParameters(attributes, additionalParameters)
                }
                builder.additionalParameters(additionalParameters)
            }
            AuthorizationGrantType.IMPLICIT -> {
                builder = OAuth2AuthorizationRequest.implicit()
            }
            else -> {
                throw IllegalArgumentException("Invalid Authorization Grant Type (" +
                        clientRegistration.authorizationGrantType.value +
                        ") for Client Registration with Id: " + clientRegistration.registrationId)
            }
        }
        val redirectUriStr = expandRedirectUri(request, clientRegistration, redirectUriAction)
        builder
                .clientId(clientRegistration.clientId)
                .authorizationUri(clientRegistration.providerDetails.authorizationUri)
                .redirectUri(redirectUriStr)
                .scopes(clientRegistration.scopes)
                .state(stateGenerator.generateKey().replace("[^A-Za-z0-9]".toRegex(), ""))
                .attributes(attributes)
        authorizationRequestCustomizer.accept(builder)
        return builder.build()
    }

    private fun resolveRegistrationId(request: HttpServletRequest): String? {
        return if (authorizationRequestMatcher.matches(request)) {
            authorizationRequestMatcher
                    .matcher(request).variables[REGISTRATION_ID_URI_VARIABLE_NAME]
        } else null
    }

    /**
     * Creates nonce and its hash for use in OpenID Connect 1.0 Authentication Requests.
     *
     * @param attributes where the [OidcParameterNames.NONCE] is stored for the authentication request
     * @param additionalParameters where the [OidcParameterNames.NONCE] hash is added for the authentication request
     *
     * @since 5.2
     * @see [3.1.2.1.  Authentication Request](https://openid.net/specs/openid-connect-core-1_0.html.AuthRequest)
     */
    private fun addNonceParameters(attributes: MutableMap<String, Any>, additionalParameters: MutableMap<String, Any>) {
        try {
            val nonce = secureKeyGenerator.generateKey()
            val nonceHash = createHash(nonce)
            attributes[OidcParameterNames.NONCE] = nonce
            additionalParameters[OidcParameterNames.NONCE] = nonceHash
        } catch (e: NoSuchAlgorithmException) {
        }
    }

    /**
     * Creates and adds additional PKCE parameters for use in the OAuth 2.0 Authorization and Access Token Requests
     *
     * @param attributes where [PkceParameterNames.CODE_VERIFIER] is stored for the token request
     * @param additionalParameters where [PkceParameterNames.CODE_CHALLENGE] and, usually,
     * [PkceParameterNames.CODE_CHALLENGE_METHOD] are added to be used in the authorization request.
     *
     * @since 5.2
     * @see [1.1.  Protocol Flow](https://tools.ietf.org/html/rfc7636.section-1.1)
     *
     * @see [4.1.  Client Creates a Code Verifier](https://tools.ietf.org/html/rfc7636.section-4.1)
     *
     * @see [4.2.  Client Creates the Code Challenge](https://tools.ietf.org/html/rfc7636.section-4.2)
     */
    private fun addPkceParameters(attributes: MutableMap<String, Any>, additionalParameters: MutableMap<String, Any>) {
        val codeVerifier = secureKeyGenerator.generateKey()
        attributes[PkceParameterNames.CODE_VERIFIER] = codeVerifier
        try {
            val codeChallenge = createHash(codeVerifier)
            additionalParameters[PkceParameterNames.CODE_CHALLENGE] = codeChallenge
            additionalParameters[PkceParameterNames.CODE_CHALLENGE_METHOD] = "S256"
        } catch (e: NoSuchAlgorithmException) {
            additionalParameters[PkceParameterNames.CODE_CHALLENGE] = codeVerifier
        }
    }

    companion object {
        private const val REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId"
        private const val PATH_DELIMITER = '/'

        /**
         * Expands the [ClientRegistration.getRedirectUriTemplate] with following provided variables:<br></br>
         * - baseUrl (e.g. https://localhost/app) <br></br>
         * - baseScheme (e.g. https) <br></br>
         * - baseHost (e.g. localhost) <br></br>
         * - basePort (e.g. :8080) <br></br>
         * - basePath (e.g. /app) <br></br>
         * - registrationId (e.g. google) <br></br>
         * - action (e.g. login) <br></br>
         *
         *
         * Null variables are provided as empty strings.
         *
         *
         * Default redirectUriTemplate is: [org.springframework.security.config.oauth2.client].CommonOAuth2Provider#DEFAULT_REDIRECT_URL
         *
         * @return expanded URI
         */
        private fun expandRedirectUri(request: HttpServletRequest, clientRegistration: ClientRegistration, action: String?): String {
            val uriVariables: MutableMap<String, String> = HashMap()
            uriVariables["registrationId"] = clientRegistration.registrationId
            val uriComponents = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                    .replacePath(request.contextPath)
                    .replaceQuery(null)
                    .fragment(null)
                    .build()
            val scheme = uriComponents.scheme
            uriVariables["baseScheme"] = scheme ?: ""
            val host = uriComponents.host
            uriVariables["baseHost"] = host ?: ""
            // following logic is based on HierarchicalUriComponents#toUriString()
            val port = uriComponents.port
            uriVariables["basePort"] = if (port == -1) "" else ":$port"
            var path = uriComponents.path
            if (StringUtils.hasLength(path)) {
                if (path!![0] != PATH_DELIMITER) {
                    path = PATH_DELIMITER.toString() + path
                }
            }
            uriVariables["basePath"] = path ?: ""
            uriVariables["baseUrl"] = uriComponents.toUriString()
            uriVariables["action"] = action ?: ""
            return UriComponentsBuilder.fromUriString(clientRegistration.redirectUriTemplate)
                    .buildAndExpand(uriVariables)
                    .toUriString()
        }

        @Throws(NoSuchAlgorithmException::class)
        private fun createHash(value: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(value.toByteArray(StandardCharsets.US_ASCII))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        }
    }

    /**
     * Constructs a `DefaultOAuth2AuthorizationRequestResolver` using the provided parameters.
     *
     * @param clientRegistrationRepository the repository of client registrations
     * @param authorizationRequestBaseUri the base `URI` used for resolving authorization requests
     */
    init {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null")
        Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty")
        this.clientRegistrationRepository = clientRegistrationRepository
        authorizationRequestMatcher = AntPathRequestMatcher(
                "$authorizationRequestBaseUri/{$REGISTRATION_ID_URI_VARIABLE_NAME}")
    }
}