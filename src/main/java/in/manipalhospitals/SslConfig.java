package in.manipalhospitals;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.inject.Named;
import org.apache.camel.support.jsse.*;

public class SslConfig {

    @Produces
    @Named("sslContextParameters")
    @Singleton
    public SSLContextParameters sslContextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("keystore.jks");
        ksp.setPassword(System.getenv().getOrDefault("KEYSTORE_PASSWORD", "123456"));
        ksp.setType("JKS");

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword(System.getenv().getOrDefault("KEYSTORE_PASSWORD", "123456"));

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);
        return scp;
    }
}
