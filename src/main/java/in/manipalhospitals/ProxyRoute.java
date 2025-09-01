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
            .process(ProxyRoute::uppercase)
            .log("Request : ${body}")
            .toD("netty-http:"
                + "${headers." + Exchange.HTTP_SCHEME + "}://"
                + "${headers." + Exchange.HTTP_HOST + "}:"
                + "${headers." + Exchange.HTTP_PORT + "}"
                + "${headers." + Exchange.HTTP_PATH + "}")
            .log("Response : ${body}")
            .process(ProxyRoute::uppercase);
    }

    public static void uppercase(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        if (body != null) {
            message.setBody(body.toUpperCase(Locale.US));
        }
    }
}
