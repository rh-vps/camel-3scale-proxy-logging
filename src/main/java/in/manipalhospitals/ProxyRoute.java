package in.manipalhospitals;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

public class ProxyRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Use environment variables for flexibility
        String httpsEnabled = System.getenv().getOrDefault("ENABLE_HTTPS", "false");
        String port = httpsEnabled.equalsIgnoreCase("true") ? "8443" : "8080";

        String fromUri = "netty-http:proxy://0.0.0.0:" + port
                + (httpsEnabled.equalsIgnoreCase("true")
                ? "?ssl=true&keyStoreFile=/tls/keystore.jks&passphrase=changeit&trustStoreFile=/tls/keystore.jks"
                : "");

        // Global error handling
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Error occurred: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant("{\"error\": \"Internal Server Error\"}"));

        from(fromUri)
                // Log incoming request
                .log(">>> Incoming ${headers.CamelHttpMethod} request")

                .log("Forwarding to: ${headers.CamelHttpScheme}://${headers.CamelHttpHost}:${headers.CamelHttpPort}${headers.CamelHttpPath}?${headers.CamelHttpQuery}")

                .log("Content-Type: ${headers.Content-Type}")

                // Ensure original HTTP method is forwarded
                .setHeader(Exchange.HTTP_METHOD, simple("${headers.CamelHttpMethod}"))

                // Pretty print request safely (JSON/XML/plain)
                .log("<<< Response code: ${header.CamelHttpResponseCode}")
                .process(new PrettyLogger())

                // Forward request to target
                .toD("netty-http:"
                        + "${headers." + Exchange.HTTP_SCHEME + "}://"
                        + "${headers." + Exchange.HTTP_HOST + "}:"
                        + "${headers." + Exchange.HTTP_PORT + "}"
                        + "${headers." + Exchange.HTTP_PATH + "}"
                        + "?bridgeEndpoint=true&throwExceptionOnFailure=true")

                .process(new PrettyLogger())

                // Final log of transformed response
                .log("Response Body: ${body}");

    }
}
