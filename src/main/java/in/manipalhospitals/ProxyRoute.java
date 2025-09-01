package in.manipalhospitals;

import java.util.Locale;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
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

        from(fromUri)
            // Log incoming request
            .log(">>> Incoming ${headers.CamelHttpMethod} request")
            .log("Forwarding to: ${headers.CamelHttpScheme}://${headers.CamelHttpHost}:${headers.CamelHttpPort}${headers.CamelHttpPath}")
            .log("Content-Type: ${headers.Content-Type}")
            .log("Request Body: ${body}")

            // Ensure original HTTP method is forwarded
            .setHeader(Exchange.HTTP_METHOD, simple("${headers.CamelHttpMethod}"))

            // Forward request to target
            .toD("netty-http:"
                + "${headers." + Exchange.HTTP_SCHEME + "}://"
                + "${headers." + Exchange.HTTP_HOST + "}:"
                + "${headers." + Exchange.HTTP_PORT + "}"
                + "${headers." + Exchange.HTTP_PATH + "}"
                + "?bridgeEndpoint=true&throwExceptionOnFailure=false")

            // Log response details
            .log("<<< Response code: ${header.CamelHttpResponseCode}")
            .log("Response Body before processing: ${body}")

            // Process response (uppercase only after response is received)
            .process(ProxyRoute::uppercase)

            // Final log of transformed response
            .log("Response Body after uppercase: ${body}");
    }

    public static void uppercase(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        if (body != null) {
            message.setBody(body.toUpperCase(Locale.US));
        }
    }
}
