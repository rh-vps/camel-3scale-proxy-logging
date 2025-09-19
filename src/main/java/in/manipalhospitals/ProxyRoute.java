package in.manipalhospitals;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

public class ProxyRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Use environment variables for flexibility
        boolean httpsEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("ENABLE_HTTPS", "true"));
        int port = httpsEnabled ? 8443 : 8080;

        // Use http:// or https:// instead of proxy://
        String fromUri = httpsEnabled
                ? "netty-http:https://0.0.0.0:" + port + "?sslContextParameters=#sslContextParameters"
                : "netty-http:http://0.0.0.0:" + port;

        // Global error handling
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Error occurred: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant("{\"error\": \"Internal Server Error\"}"));

        from(fromUri)
                // Preserve original HTTP method
                .setHeader(Exchange.HTTP_METHOD, simple("${headers.CamelHttpMethod}"))

                .process(new PrettyLogger("Request"))

                // Forward request to target dynamically
                .toD("netty-http:${headers.CamelHttpScheme}://${headers.CamelHttpHost}:${headers.CamelHttpPort}${headers.CamelHttpPath}"
                        + "?bridgeEndpoint=true&throwExceptionOnFailure=true")

                .process(new PrettyLogger("Response"));
    }
}
