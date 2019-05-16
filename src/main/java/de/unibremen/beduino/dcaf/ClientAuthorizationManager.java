package de.unibremen.beduino.dcaf;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
public class ClientAuthorizationManager extends AuthorizationManager {

    private static Logger logger = LoggerFactory.getLogger(ClientAuthorizationManager.class);
    private static final String CAM_IDENTITY = "cam";
    private static final int SAM_RESPONSE_TIMEOUT = 10000;

    /**
     * Initializes a Client Authorization Manager which listens on the requested port.
     *
     * @param port The port on which the CAM should listen to for incoming Access Request Messages
     */
    public ClientAuthorizationManager(int port) {
        super(port);
        initializeResource();
        addDTLSEndpoint(CAM_IDENTITY);
    }

    /**
     * Initializes a Client Authorization Manager which listens on default CoAPs port 5684
     */
    public ClientAuthorizationManager() {
        super(NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT));
        initializeResource();
        addDTLSEndpoint(CAM_IDENTITY);
    }


    @Override
    public synchronized void start() {
        super.start();

        String interfaces = getEndpoints().stream().
                map(Endpoint::getAddress).
                map(a -> "[" + a.getHostString() + ":" + a.getPort() + "]")
                .collect(Collectors.joining(", "));


        logger.info("CAM server running on " + interfaces + ".");
    }

    private void initializeResource() {
        /* Add CoAP Resource to process incoming requests */
        add(new CoapResource("client-authorize") {

            {
                /* Set title/description for the CoAP Resource */
                getAttributes().setTitle("DCAF Client Authorization Manager");
            }

            @Override
            public void handlePOST(CoapExchange exchange) {
                byte[] requestPayload = exchange.getRequestPayload();

                Optional<AccessRequest> request = Utils.deserializeCbor(requestPayload, AccessRequest.class);
                if (!request.isPresent()) {
                    logger.error("Deserialization of CBOR failed. The access request probably is malformed");
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                    return;
                }

                Response response = processAccessRequest(request.get());
                byte[] payload = null;

                if(response.getCode() == CoAP.ResponseCode.CONTENT && response.getPayload() == null) {
                    exchange.respond(CoAP.ResponseCode.CONTENT);
                } else if (response.getCode() == CoAP.ResponseCode.CONTENT && response.getPayload() != null) {
                    Optional<TicketGrantMessage> answer = Utils.deserializeCbor(response.getPayload(), TicketGrantMessage.class);
                    boolean success = false;
                    if (answer.isPresent()) {
                        Optional<byte[]> possibleTicketGrantMessageCbor = Utils.serializeCbor(answer.get());
                        if (possibleTicketGrantMessageCbor.isPresent()) {
                            payload = possibleTicketGrantMessageCbor.get();
                            success = true;
                        }
                    }

                    if (success) {
                        logger.debug("respond: h'" + Hex.encodeHexString(payload) + "'");
                        exchange.respond(CoAP.ResponseCode.CONTENT, payload);
                    } else {
                        logger.error("Something went wrong. The response from SAM was malformed. Probably an internal" +
                                "server error.");
                        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    exchange.respond(response.getCode());
                }
            }
        });
    }

    private Response processAccessRequest(AccessRequest message) {
        TicketRequestMessage tRM = new TicketRequestMessage(message.getSam(), message.getSai(),
                message.getTimestamp(), message.getUpdateAttributes(), message.getSignature());
        Optional<byte[]> optionalSerializedTRM = Utils.serializeCbor(tRM);

        if (optionalSerializedTRM.isPresent()) {
            byte [] serializedTicketRequestMessage = optionalSerializedTRM.get();
            return dTLSPSKRequest(message.getSam(), serializedTicketRequestMessage);
        } else {
            logger.error("Error while serializing the Ticket Request Message, Access Request might be malformed");
            return new Response(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    private Response dTLSPSKRequest(String uri, byte[] payload) {
        Request request = Request.newPost();
        request.setURI(uri);
        request.setPayload(payload);
        request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_CBOR);

        request.send(getEndpoint(getDTLSPort()));
        Response response = null;
        try {
            response = request.waitForResponse(SAM_RESPONSE_TIMEOUT);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        if (response == null) {
            logger.error("Connection to SAM server timed out, no response");
            return new Response(CoAP.ResponseCode.BAD_GATEWAY);
        }

        return response;
    }
}
