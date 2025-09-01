package in.manipalhospitals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

public class ProxyRoute extends RouteBuilder {


    private static final ObjectWriter jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Override
    public void configure() {
        // Use environment variables for flexibility
        String httpsEnabled = System.getenv().getOrDefault("ENABLE_HTTPS", "false");
        String port = httpsEnabled.equalsIgnoreCase("true") ? "8443" : "8080";

        String fromUri = "netty-http:proxy://0.0.0.0:" + port
                + (httpsEnabled.equalsIgnoreCase("true")
                    ? "?ssl=true&keyStoreFile=/tls/keystore.jks&passphrase=changeit&trustStoreFile=/tls/keystore.jks"
                    : "");

        // 🔹 Global error handling
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
            .log("Request Body: ${body}")

            // Ensure original HTTP method is forwarded
            .setHeader(Exchange.HTTP_METHOD, simple("${headers.CamelHttpMethod}"))

            // Forward request to target
            .toD("netty-http:"
                + "${headers." + Exchange.HTTP_SCHEME + "}://"
                + "${headers." + Exchange.HTTP_HOST + "}:"
                + "${headers." + Exchange.HTTP_PORT + "}"
                + "${headers." + Exchange.HTTP_PATH + "}"
                + "?bridgeEndpoint=true&throwExceptionOnFailure=true")

            // Process response
            .process(ProxyRoute::prettyPrintBody)

            // Final log of transformed response
            .log("Response Body: ${body}");

    }


    public static void prettyPrintBody(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        if (body != null && !body.isBlank()) {
            try {
                // Try JSON pretty-print first
                String formatted = jsonWriter.writeValueAsString(
                    new ObjectMapper().readValue(body, Object.class)
                );
                message.setBody(formatted);
                exchange.getContext().createProducerTemplate().sendBody("log:pretty?level=INFO", formatted);
            } catch (Exception e) {
                // Fallback: plain text
                exchange.getContext().createProducerTemplate().sendBody("log:pretty?level=INFO", body);
            }
        }
    }



}
