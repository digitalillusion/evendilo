package xyz.deverse.evendilo.config.support

import org.springframework.core.convert.converter.Converter
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.util.StringUtils
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap

/**
 * Copy of MapOAuth2AccessTokenResponseConverter that
 * sets the token type as BEARER by default instead of null
 */
class ResilientTokenResponseConverter : Converter<MutableMap<String, String>, OAuth2AccessTokenResponse> {
    private val TOKEN_RESPONSE_PARAMETER_NAMES: Set<String> = HashSet(listOf(
            OAuth2ParameterNames.ACCESS_TOKEN,
            OAuth2ParameterNames.EXPIRES_IN,
            OAuth2ParameterNames.REFRESH_TOKEN,
            OAuth2ParameterNames.SCOPE,
            OAuth2ParameterNames.TOKEN_TYPE
    ))

    override fun convert(tokenResponseParameters: MutableMap<String, String>): OAuth2AccessTokenResponse? {
        val accessToken = tokenResponseParameters[OAuth2ParameterNames.ACCESS_TOKEN]
        var accessTokenType: OAuth2AccessToken.TokenType = OAuth2AccessToken.TokenType.BEARER
        if (OAuth2AccessToken.TokenType.BEARER.value.equals(
                        tokenResponseParameters[OAuth2ParameterNames.TOKEN_TYPE], ignoreCase = true)) {
            accessTokenType = OAuth2AccessToken.TokenType.BEARER
        }
        var expiresIn: Long = 0
        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.EXPIRES_IN)) {
            try {
                expiresIn = tokenResponseParameters[OAuth2ParameterNames.EXPIRES_IN]!!.toLong()
            } catch (ex: NumberFormatException) {
            }
        }
        var scopes = emptySet<String>()
        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.SCOPE)) {
            val scope = tokenResponseParameters[OAuth2ParameterNames.SCOPE]
            scopes = HashSet(listOf(*StringUtils.delimitedListToStringArray(scope, " ")))
        }
        val refreshToken = tokenResponseParameters[OAuth2ParameterNames.REFRESH_TOKEN]
        val additionalParameters: MutableMap<String, Any?> = LinkedHashMap()
        for ((key, value) in tokenResponseParameters) {
            if (!TOKEN_RESPONSE_PARAMETER_NAMES.contains(key)) {
                additionalParameters[key] = value
            }
        }
        return OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(accessTokenType)
                .expiresIn(expiresIn)
                .scopes(scopes)
                .refreshToken(refreshToken)
                .additionalParameters(additionalParameters)
                .build()
    }
}
