package de.unibremen.beduino.dcaf;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.stream.Collectors;

/**
 * @author Norman Lipkow
 */
abstract class AuthorizationManager extends CoapServer {

    private static final String TRUST_STORE_PASSWORD = "rootPass";
    private static final String KEY_STORE_PASSWORD = "endPass";
    private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
    private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";
    private static final String KEYSTORE_TYPE = "JKS";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int port;
    protected DTLSConnector dtlsConnector;
    private DtlsConnectorConfig config;

    AuthorizationManager() {
        this.port = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT);
    }

    AuthorizationManager(int port) {
        this.port = port;
    }

    void addDTLSEndpoint(String identity) {
        addDTLSEndpoint(this.port, identity);
    }

    void addDTLSEndpoint(int port, String identity) {
        for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            if (addr.getHostAddress().equals("127.0.0.1")) {
                InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
                dtlsConnector = getDtlsConnector(identity, bindToAddress);

                CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
                builder.setConnector(dtlsConnector)
                        .setNetworkConfig(NetworkConfig.getStandard());
                addEndpoint(builder.build());
            }
        }
    }

    /**
     * Add individual endpoints listening on default CoAP port on all ddresses of all network interfaces.
     */
    public void addEndpoints(int port) {
        for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
            CoapEndpoint.CoapEndpointBuilder coapEndpointBuilder = new CoapEndpoint.CoapEndpointBuilder();

            addEndpoint(coapEndpointBuilder.setInetSocketAddress(bindToAddress).build());
        }
    }

    int getDTLSPort() {
        return port;
    }

    private DTLSConnector getDtlsConnector(String identity, InetSocketAddress bindToAddress) {
        if (dtlsConnector == null) {
            DTLSConnector connector;

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
                 InputStream inTrust = getClass().getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION)) {
                KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());

                KeyStore trustStore = KeyStore.getInstance(KEYSTORE_TYPE);
                trustStore.load(inTrust, TRUST_STORE_PASSWORD.toCharArray());

                Certificate[] trustedCertificates = new Certificate[1];
                trustedCertificates[0] = trustStore.getCertificate("root");

                DtlsConnectorConfig.Builder dTLSConfig = new DtlsConnectorConfig.Builder();
                JsonPskStore store = new JsonPskStore();
                config = dTLSConfig.setAddress(bindToAddress)
                        .setIdentity((PrivateKey)keyStore.getKey(identity, KEY_STORE_PASSWORD.toCharArray()),
                        keyStore.getCertificateChain(identity), true)
                        .setClientAuthenticationRequired(true)
                        .setPskStore(store)
                        .setTrustStore(trustedCertificates)
                        .build();

                connector = new DTLSConnector(config);
            } catch (Exception e) {
                throw new RuntimeException("Something went wrong while initializing the key and trust store", e.getCause());
            }
            return connector;
        }

        return dtlsConnector;
    }

    public void addPsk(final String identity, final String key) {
        PskStore store = this.config.getPskStore();

        if (store instanceof JsonPskStore) {
            ((JsonPskStore) store).setKey(identity, key);
            return;
        }

        logger.error("PSKStore does not support adding new keys after initialization.");
    }

    public void addKnownPeer(final InetSocketAddress peer, final String identity, final String key) {
        PskStore store = this.config.getPskStore();

        if (store instanceof JsonPskStore) {
            ((JsonPskStore) store).addKnownPeer(peer, identity, key);
            return;
        }

        logger.error("PSKStore does not support adding new keys after initialization.");
    }

    public byte[] getPsk(final String identity) {
        return this.config.getPskStore().getKey(identity);
    }

    void deletePsk(final String identity) {
        PskStore store = this.config.getPskStore();

        if (store instanceof JsonPskStore) {
            ((JsonPskStore) store).deleteKey(identity);
            return;
        }

        logger.error("PSKStore does not support deleting keys after initialization.");
    }

    String getInterfaces() {
        return getEndpoints().stream()
                .map(Endpoint::getAddress)
                .map(a -> "[" + a.getHostString() + ":" + a.getPort() + "]")
                .collect(Collectors.joining(", "));
    }
}
