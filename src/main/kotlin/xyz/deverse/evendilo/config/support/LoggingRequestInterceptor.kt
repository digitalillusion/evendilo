package xyz.deverse.evendilo.config.support

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset

class LoggingRequestInterceptor : ClientHttpRequestInterceptor {
    @Throws(IOException::class)
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        traceRequest(request, body)
        val response = execution.execute(request, body)
        traceResponse(response)
        return response
    }

    @Throws(IOException::class)
    private fun traceRequest(request: HttpRequest, body: ByteArray) {
        log.debug("REQUEST")
        log.debug("URI         : {}", request.uri)
        log.debug("Method      : {}", request.method)
        log.debug("Headers     : {}", request.headers)
        log.debug("Body        : {}", String(body, Charset.defaultCharset()))
    }

    @Throws(IOException::class)
    private fun traceResponse(response: ClientHttpResponse) {
        val inputStringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(response.body, "UTF-8"))
        var line = bufferedReader.readLine()
        while (line != null) {
            inputStringBuilder.append(line)
            inputStringBuilder.append('\n')
            line = bufferedReader.readLine()
        }
        log.debug("RESPONSE")
        log.debug("Status code  : {}", response.statusCode)
        log.debug("Status text  : {}", response.statusText)
        log.debug("Headers      : {}", response.headers)
        log.debug("Response body: {}", inputStringBuilder.toString())
    }

    companion object {
        val log = LoggerFactory.getLogger(LoggingRequestInterceptor::class.java)
    }
}