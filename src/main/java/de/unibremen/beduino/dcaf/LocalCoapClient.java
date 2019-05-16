package de.unibremen.beduino.dcaf;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Norman Lipkow
 */
public class LocalCoapClient {

    private static final int TIMEOUT = 10000;
    private static final String CLIENT_IDENTITY = "client";
    private static final String SECRET_PSK = "secretPSK";
    private static final String COAPS = "coaps://";
    private static final String LOCALHOST = "127.0.0.1";
    private static Logger logger = LoggerFactory.getLogger(LocalCoapClient.class);
    private static Endpoint dtlsEndpoint;

    public LocalCoapClient(int port) {
        initialize(port);
    }

    public LocalCoapClient() {
        initialize(8000);

    }

    public void testAccessRequestToCam(int camPort) {
        // EXAMPLE SAI DATA
        List<Authorization> authorizations = new ArrayList<>();
        authorizations.add(new Authorization("coaps://[2001:DB8::dcaf:1234]/update", 10));
        authorizations.add(new Authorization("coaps://resource-server.com/secret/resource2", 5));

        Response response = sendAccessRequest(authorizations, camPort);

        if (response != null && response.getPayload() != null) {
            String jsonResponseOutputPrettyPrint = Utils.cborPayloadToPrettyString(response.getPayload());
            logger.info(jsonResponseOutputPrettyPrint);
        }
    }

    private Response sendAccessRequest(List<Authorization> authorizations, int camPort) {

        AccessRequest accessRequest = new AccessRequest(COAPS + LOCALHOST + "/authorize", authorizations, 0, null, null);

        Request request = new Request(CoAP.Code.POST);
        request.setURI(COAPS + LOCALHOST + ":" + camPort + "/client-authorize");
        Optional<byte[]> optionalBytes = Utils.serializeCbor(accessRequest);

        if (optionalBytes.isPresent()) {
            request.setPayload(optionalBytes.get());
            request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_CBOR);

            request.send(dtlsEndpoint);

            Response response = null;
            try {
                response = request.waitForResponse(TIMEOUT);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            if (response == null) {
                logger.error("Connection to CAM timed out, no response");
                return new Response(CoAP.ResponseCode.BAD_GATEWAY);
            }

            return response;
        }

        return null;
    }

    private void initialize(int port) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();

        InMemoryPskStore pskStore = new InMemoryPskStore();
        pskStore.addKnownPeer(new InetSocketAddress(LOCALHOST, 8002), "TEST_CLIENT", SECRET_PSK.getBytes());
        builder.setPskStore(pskStore);
        builder.setAddress(inetSocketAddress);
        builder.setSniEnabled(false);
        builder.setSupportedCipherSuites(new CipherSuite[] {CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});

        DTLSConnector dtlsconnector = new DTLSConnector(builder.build(), null);

        dtlsEndpoint = new CoapEndpoint.CoapEndpointBuilder()
                .setConnector(dtlsconnector)
                .setNetworkConfig(NetworkConfig.getStandard()).build();
        try {
            dtlsEndpoint.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EndpointManager.getEndpointManager().setDefaultEndpoint(dtlsEndpoint);
    }
}
