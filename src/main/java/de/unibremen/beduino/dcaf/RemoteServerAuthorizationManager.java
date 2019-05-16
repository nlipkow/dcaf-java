package de.unibremen.beduino.dcaf;

import de.unibremen.beduino.dcaf.exceptions.MacFailedException;
import de.unibremen.beduino.dcaf.exceptions.ResourceNotFoundException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Norman Lipkow
 */
public class RemoteServerAuthorizationManager extends AuthorizationManager {

    private static Logger logger = LoggerFactory.getLogger(RemoteServerAuthorizationManager.class);
    private static final int SERVER_TIMEOUT = 10000;
    private static final String URI_PROTOCOL = "coap://";
    private static final String SAM_IDENTITY = "sam";
    private static final int LIFETIME = 60;
    private static final int TICKET_CHECK_INTERVAL = 30; // in seconds

    private MacMethod macMethod = MacMethod.HMAC_SHA_256;
    private DAO dao = FileDAO.getInstance();

    public RemoteServerAuthorizationManager() {
        initializeResource();
        addDTLSEndpoint(5684, SAM_IDENTITY);
        startTicketValidityCheckTask();
    }

    /**
     * Instantiates a new Remote server authorization manager.
     *
     * @param port the port
     */
    public RemoteServerAuthorizationManager(int port) {
        initializeResource();
        addDTLSEndpoint(port, SAM_IDENTITY);
    }

    @Override
    public synchronized void start() {
        super.start();

        logger.info("SAM server running on " + getInterfaces() + ".");
    }

    /**
     * Returns all rules defined on the authorization manager.
     *
     * @return All access rules on the authorization manager.
     */
    public List<AccessRule> getAccessRules() {
        return dao.getAccessRules();
    }

    /**
     * Creates a new access rule. If the the rule id for the given rule already exists, no rule will be created.
     *
     * @param rule The new rule to be created.
     * @return true if rule was created and false if the rule id already existed.
     */
    public boolean addAccessRule(AccessRule rule) {
        ServerInfo info = dao.getServerInformation(rule.getServerAccessRules().get(0).getServerHost());

        if (info == null) {
            throw new IllegalArgumentException();
        }
        rule.getServerAccessRules().get(0).setServerInfo(info);

        if (dao.getAccessRule(rule.getId()) == null && dao.saveAccessRule(rule)) {
            logger.info("Added new access rule with id " + rule.getId());
            return true;
        }

        return false;
    }

    /**
     * Deletes an access rule for the gives ID.
     *
     * @param id The ID of the access rule to be deleted.
     */
    public void deleteAccessRule(String id) {
        if (dao.deleteAccessRule(id)) {
            logger.info("Deleted access rule with id " + id);
        }
    }

    /**
     * Updates an already existing access rule and changes the data according to the passed access rule.
     * If no access rule was found with the same ID, a new rule will be created.
     *
     * @param rule The rule to be updated
     * @param revoke If true, all tickets associated with the changes in the rule will be revoked.
     */
    public void updateAccessRule(AccessRule rule, boolean revoke) {
        if (dao.getAccessRule(rule.getId()) == null) {
            dao.saveAccessRule(rule);
        } else if (dao.updateAccessRule(rule)) {
            logger.info("Updated access rule with id " + rule.getId());
        }

        if (revoke) {
            revokeAffectedTickets(rule);
        }
    }

    /**
     * Creates a new known server for SAM. If a server with the same host address already exists, no new server
     * will be created.
     *
     * @param cam The new CAM to create.
     * @return true, if a new CAM was created and false if a CAM with the same id already existed.
     */
    public boolean addCamInfo(CamInfo cam) {
        if (!dao.getClientAuthorizationManagers().contains(cam) && dao.saveClientAuthorizationManager(cam)) {
            logger.info("Added new CAM with id " + cam.getId());
            return true;
        }
        return false;
    }

    public List<CamInfo> getClientAuthorizationManagers() {
        return dao.getClientAuthorizationManagers();
    }

    /**
     * Deletes a known Client Authorization Manager from the Server Authorization Manager.
     *
     * @param id The client identifier (host address) from the CAM to be deleted.
     * @param revoke If true, all tickets which were commissioned to CAMs with the same host address will be deleted.
     *      *               be deleted.
     */
    public void deleteCam(String id, boolean revoke) {
        if (dao.deleteClientAuthorizationManager(id)) {
            deleteAllAffectedAccessRulesByCamId(id);
            logger.info("Deleted CAM with id " + id);

            if (revoke) {
                for (TicketGrantMessage ticket : dao.getTickets()) {
                    if (id.equals(ticket.getCamIdentifier())) {
                        revokeTicket(ticket.getId());
                    }
                }
            }
        }
    }

    /**
     * Updates an already existing known Client Authorization Manager (CAM) and changes the data according to the passed
     * CAM.
     * If no CAM was found with the same ID, a new rule will be created.
     *
     * @param cam The CAM to be updated
     */
    public void updateCam(CamInfo cam) {
        if (dao.getClientAuthorizationManager(cam.getId()) == null) {
            dao.saveClientAuthorizationManager(cam);
        } else if (dao.updateClientAuthorizationManager(cam)) {
            logger.info("Updated CAM with id " + cam.getId());
        }
    }

    /**
     * Creates a new known server for SAM. If a server with the same host address already exists, no new server
     * will be created.
     *
     * @param serverInfo The new server to create.
     * @return true, if a new server was created and false if a server with the same id already existed.
     */
    public boolean addServer(ServerInfo serverInfo) {
        if (dao.getServerInformation(serverInfo.getHost()) == null && dao.saveServerInformation(serverInfo)) {
            addPsk(serverInfo.getHost(), serverInfo.getPreSharedKey());
            logger.info("Added new Server with host address " + serverInfo.getHost());
            return true;
        }
        return false;
    }

    /**
     * Returns all known server on SAM.
     *
     * @return A list of all known server on SAM.
     */
    public List<ServerInfo> getServer() {
        return dao.getServerInformations();
    }

    /**
     * Updates an already existing known server and changes the data according to the passed server.
     * If no server was found with the same ID, a new server will be created.
     *
     * @param serverInfo The server to be updated.
     */
    public void updateServer(ServerInfo serverInfo) {
        if (dao.getServerInformation(serverInfo.getHost()) == null) {
            dao.saveServerInformation(serverInfo);
        } else if (dao.updateServerInformation(serverInfo)) {
            addPsk(serverInfo.getHost(), serverInfo.getPreSharedKey());
            logger.info("Updated Server with host address " + serverInfo.getHost());
        }
    }

    /**
     * Deletes a known server from the SAM.
     *
     * @param host server's host address to be deleted.
     * @param revoke If true, all tickets which were commissioned with the given server as the destination will
     *               be deleted.
     */
    public void deleteServer(String host, boolean revoke) {
        if (dao.deleteServerInformation(host)) {
            deleteAllAffectedAccessRulesByServerHost(host);
            deletePsk(host);
            logger.info("Deleted Server with host address " + host);

            if (revoke) {
                for (TicketGrantMessage ticket : dao.getTickets()) {
                    if (host.equals(ticket.getServerHost())) {
                        revokeTicket(ticket.getId());
                    }
                }
            }
        }
    }

    /**
     * Returns all known tickets.
     *
     * @return A list of all currently commissioned tickets on SAM.
     */
    public List<TicketGrantMessage> getTickets() {
        return dao.getTickets();
    }

    private void revokeAffectedTickets(AccessRule rule) {
        List<TicketGrantMessage> currentTickets = dao.getTickets();
        String cam = rule.getCamIdentifier();

        for (TicketGrantMessage ticket : currentTickets) {
            if (cam.equals(ticket.getCamIdentifier())) {
                revokeAllOfCamsAffectedTickets(rule, ticket);
            }
        }
    }

    private void revokeAllOfCamsAffectedTickets(AccessRule rule, TicketGrantMessage ticket) {
        for (ServerAccessRule serverRule : rule.getServerAccessRules()) {
            for (Authorization authorization : ticket.getFace().getSai()) {
                if (!StringUtils.equals(authorization.getResourcePath(), serverRule.getResource()) ||
                        (authorization.getMethods() & serverRule.getMethods()) != authorization.getMethods()) {
                    revokeTicket(ticket.getId());
                }
            }
        }
    }

    private void initializeResource() {
        CoapResource serverAuthorizeResource = initializeAuthorizeResource();
        serverAuthorizeResource.getAttributes().setTitle("DCAF SAM Authorization Endpoint");
        add(serverAuthorizeResource);
    }

    private void deleteAllAffectedAccessRulesByServerHost(String host) {
        for (AccessRule rule : dao.getAccessRules()) {
            for (ServerAccessRule serverRule : rule.getServerAccessRules()) {
                if (host.equals(serverRule.getServerHost())) {
                    deleteAccessRule(rule.getId());
                }
            }
        }
    }
    private void deleteAllAffectedAccessRulesByCamId(String camId) {
        for (AccessRule rule : dao.getAccessRules()) {
            if (camId.equals(rule.getCamIdentifier())) {
                deleteAccessRule(rule.getId());
            }
        }
    }

    private CoapResource initializeAuthorizeResource() {
        return new CoapResource("authorize") {
            @Override
            public void handlePOST(CoapExchange exchange) {
                if (exchange.getRequestOptions().getContentFormat() != MediaTypeRegistry.APPLICATION_CBOR) {
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                    return;
                }

                byte[] requestPayload = exchange.getRequestPayload();
                /* Deserialize incoming requests from CBOR to Java Objects */
                Optional<TicketRequestMessage> request = Utils.deserializeCbor(requestPayload, TicketRequestMessage.class);
                if (!request.isPresent()) {
                    logger.error("Deserialization of CBOR failed. The access request probably is malformed");
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                    return;
                }

                TicketRequestMessage ticketRequestMessage = request.get();
                CamInfo camInfo = getCamInfoFromIdentifier(getClientIdentifier(exchange));


                if (camInfo == null) {
                    logger.error("Unknown Client information");
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Unknown Client information");
                    return;
                }

                //if true, it's an update request and needs further checks
                String updateHashEncrypted = null;
                if (ticketRequestMessage.getSignature() != null && ticketRequestMessage.getUpdateAttributes() != null) {
                    UpdateVerifier updateVerifier = new UpdateVerifier(ticketRequestMessage);

                    try {
                        if (!updateVerifier.verify()) {
                            logger.error("Signature is invalid");
                            exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Signature is invalid");
                            return;
                        } else {
                            byte[] psk = getPskFromServer(ticketRequestMessage.getSai().get(0).getHostURL());

                            try {
                                updateHashEncrypted = AES.encrypt(new String(psk), updateVerifier.getHash());
                            } catch (GeneralSecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (CertificateException | InvalidKeyException | SignatureException | KeyStoreException |
                            IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }

                ticketRequestMessage = filterPermissions(camInfo, ticketRequestMessage);

                if (ticketRequestMessage.getSai().size() == 0) {
                    exchange.respond(CoAP.ResponseCode.CONTENT);
                } else {
                    Face face = new Face(ticketRequestMessage.getSai(), ticketRequestMessage.getTimestamp(), LIFETIME,
                            macMethod.getEncoding(), updateHashEncrypted);
                    Verifier verifier;
                    try {
                        verifier = generateVerifier(face);
                    } catch (Exception e) {
                        logger.error("An unexpected error occurred while generating the verifier", e);
                        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                        return;
                    }

                    TicketGrantMessage ticketGrantMessage = new TicketGrantMessage(face, verifier);
                    ticketGrantMessage.setCamIdentifier(camInfo.getId());
                    ticketGrantMessage.setServerHost(ticketRequestMessage.getSai().get(0).getHostURL());

                    if (!dao.saveTicket(ticketGrantMessage)) {
                        logger.error("Could not create ticket, possible duplicate?");
                        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                        return;
                    }
                    ticketGrantMessage.setCamIdentifier(null);
                    ticketGrantMessage.setServerHost(null);

                    long validUntil = System.currentTimeMillis() + (ticketGrantMessage.getFace().getLifetime() * 1000);
                    logger.info("A new ticket was created for " + camInfo.getName() + " with the following " +
                            "authorizations: " + ticketGrantMessage.getFace().getSai() + ". The ticket is valid " +
                            "until " + new Date(validUntil) + ".");

                    exchange.setMaxAge(ticketGrantMessage.getFace().getLifetime());
                    Optional<byte[]> serializedTRM = Utils.serializeCbor(ticketGrantMessage);
                    if (serializedTRM.isPresent()) {
                        exchange.respond(CoAP.ResponseCode.CONTENT, serializedTRM.get());
                    } else {
                        logger.error("Could not serialize object " + ticketGrantMessage.getClass().getName());
                        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        };
    }

    private byte[] getPskFromServer(String host) {
        ServerInfo serverInfo = dao.getServerInformation(host);

        if (serverInfo == null || serverInfo.getPreSharedKey() == null) {
            throw new IllegalArgumentException("No key was found for host " + host);
        }

        return serverInfo.getPreSharedKey().getBytes();
    }

    private void startTicketValidityCheckTask() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new TicketValidityCheckTask(), 30, TICKET_CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    public void revokeTicket(String id) {
        TicketGrantMessage ticketGrantMessage = dao.getTicket(id);
        RevocationTicket revocationTicket = new RevocationTicket(ticketGrantMessage);

        if (ticketGrantMessage != null) {
            dao.deleteTicket(ticketGrantMessage.getId());
            logger.info("Ticket with id " + ticketGrantMessage.getId() + " is not valid anymore and was therefore revoked.");
            //TODO: Still needs consensus on how sam should behave here. For now, tickets are only deleted on SAM
            // when revoked.
//            Response response = tryToInformServerAboutRevokedTicket(revocationTicket);
//            if (response != null && response.getCode() == CoAP.ResponseCode.CONTENT) {
//                long deliveryTime = System.currentTimeMillis() / 1000;
//                revocationTicket.setDeliveryTime(deliveryTime);
//                dao.saveRevocationTicket(revocationTicket);
//            }
        }
    }

    private Response tryToInformServerAboutRevokedTicket(RevocationTicket revocationTicket) {
        Authorization authorization = revocationTicket.getTicket().getFace().getSai().get(0);
        if (authorization == null) {
            return null;
        }

        String serverHost = authorization.getHostURL();

        Optional<byte[]> optionalRT = Utils.serializeCbor(revocationTicket);

        if (optionalRT.isPresent()) {
            Request request = new Request(CoAP.Code.POST);
            request.setURI(URI_PROTOCOL + serverHost + "/revoke");
            request.setPayload(optionalRT.get());
            Response response;

            try {
                response = request.waitForResponse(SERVER_TIMEOUT);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                return null;
            }

            if (response == null) {
                logger.error("Connection to Server [" + serverHost + "] timed out, no response");
                return new Response(CoAP.ResponseCode.BAD_GATEWAY);
            }

            return response;
        }
        return null;
    }

    private String getClientIdentifier(CoapExchange exchange) {
        try {
            String digestHex = getClientCertificateFingerprint(exchange);
            System.err.println(digestHex);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // TODO: We need some sort of unique identifier for CAM/C which can be retrieved from SAM. Peer address sufficient?
        return exchange.advanced().getRequest().getSourceContext().getPeerAddress().getHostString();
    }

    private String getClientCertificateFingerprint(CoapExchange exchange) throws NoSuchAlgorithmException {
        InetSocketAddress peerAddress = exchange.advanced().getRequest().getSourceContext().getPeerAddress();
        RawPublicKeyIdentity peerIdentity = (RawPublicKeyIdentity) dtlsConnector.getSessionByAddress(peerAddress).getPeerIdentity();

        MessageDigest instance = MessageDigest.getInstance("SHA-1");
        instance.update(peerIdentity.getKey().getEncoded());

        return DatatypeConverter.printHexBinary(instance.digest());
    }

    private boolean addAccessRule(String id, String camIdentifier, ServerAccessRule serverAccessRule) throws ResourceNotFoundException {
        if (ruleIdAlreadyDefined(id) && getCamInfoFromIdentifier(camIdentifier) != null) {
            return false;
        }

        AccessRule rule = getAccessRuleFromCamIdentifier(camIdentifier);
        if (rule == null) {
            rule = new AccessRule(id, camIdentifier);
        }

        Resource serverResource = getServerResourceFromPath(serverAccessRule.getResource(),
                serverAccessRule.getServerInfo().getResources());
        if (serverResource == null) {
            throw new ResourceNotFoundException();
        }
        int supportedMethods = serverAccessRule.getMethods() & serverResource.getMethods();
        int notSupportedMethods = supportedMethods ^ serverAccessRule.getMethods();

        if (notSupportedMethods != 0) {
            String logInfo;
            logInfo = "Server host " + serverAccessRule.getServerInfo().getHost() + " does not support methods " +
                    Method.getMethodsFromIntValue(notSupportedMethods) + " on resource " +
                    serverAccessRule.getResource() + ". ";
            if (supportedMethods != 0) {
                logInfo += "Adding permissions for methods " + Method.getMethodsFromIntValue(supportedMethods) + " only.";
                logger.warn(logInfo);
            } else {
                logInfo += "No requested access method is supported. No rule was created.";
                logger.warn(logInfo);
                return false;
            }
        }

        serverAccessRule.setMethods(supportedMethods);

        rule.addRule(serverAccessRule);
        dao.saveAccessRule(rule);

        return true;
    }

    private Resource getServerResourceFromPath(String resource, List<Resource> serverResources) {
        for (Resource serverResource : serverResources) {
            if (serverResource.getResourcePath().equals(resource)) {
                return serverResource;
            }
        }

        return null;
    }

    private boolean ruleIdAlreadyDefined(String id) {
        for (AccessRule rule : dao.getAccessRules()) {
            if (rule.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    private AccessRule getAccessRuleFromCamIdentifier(String identifier) {
        for (AccessRule rule : dao.getAccessRules()) {
            if (rule.getCamIdentifier().equals(identifier)) {
                return rule;
            }
        }

        return null;
    }

    private CamInfo getCamInfoFromIdentifier(String identifier) {
        for (CamInfo info : dao.getClientAuthorizationManagers()) {
            if (info.getId().equals(identifier)) {
                return info;
            }
        }
        return null;
    }

    /**
     * @param face the the verifier should be generated for
     * @return Verifier
     */
    private Verifier generateVerifier(Face face) throws MacFailedException, InvalidKeyException {
        Optional<byte[]> cborData = Utils.serializeCbor(face);

        if (cborData.isPresent()) {
            byte[] psk = getPskFromServer(face.getSai().get(0).getHostURL());

            logger.debug("computeMac with payload: " + Hex.encodeHexString(cborData.get())
                    + " and algorithm: " + face.getMacMethod().getAlgorithmName());
            byte[] mac = Utils.computeMac(face.getMacMethod(), psk, cborData.get());

            return new Verifier(mac);
        }

        return new Verifier(new byte[]{});
    }

    private TicketRequestMessage filterPermissions(CamInfo cam, TicketRequestMessage ticketRequestMessage) {
        UpdateVerifier updateVerifier = new UpdateVerifier(ticketRequestMessage);
        List<Authorization> authorizations = new ArrayList<>();
        for (AccessRule accessRule : dao.getAccessRules()) {
            if (!accessRule.getCamIdentifier().equals(cam.getId())) {
                continue;
            }

            if (0 < accessRule.getExpirationTime() - System.currentTimeMillis()) {
                continue;
            }

            for (ServerAccessRule serverAccessRule : accessRule.getServerAccessRules()) {
                for (Authorization authorization : ticketRequestMessage.getSai()) {
                    if (!serverAccessRule.getServerHost().equals(authorization.getHostURL())) {
                        continue;
                    }

                    if (!serverAccessRule.getResource().equals(authorization.getResourcePath())) {
                        continue;
                    }
                    int methods = serverAccessRule.getMethods() & authorization.getMethods();
                    if (methods == 0) {
                        continue;
                    }

                    if (serverAccessRule.getUpdateAttributes() != null && !serverAccessRule.getUpdateAttributes().isEmpty()) {
                        if (ticketRequestMessage.getUpdateAttributes() == null) {
                            continue;
                        }

                        if (!updateVerifier.attributesAllowed(serverAccessRule.getUpdateAttributes())) {
                            continue;
                        }
                    }

                    Authorization auth = new Authorization(authorization.getUri(), methods);
                    mergeOrAddMethodsIfAlreadyExistent(auth, authorizations);
                }
            }
        }


        return new TicketRequestMessage(ticketRequestMessage.getSamUrl(), authorizations,
                ticketRequestMessage.getTimestamp(), ticketRequestMessage.getUpdateAttributes(), ticketRequestMessage.getSignature());
    }

    private void mergeOrAddMethodsIfAlreadyExistent(Authorization authorization, List<Authorization> authorizations) {
        for (Authorization auth : authorizations) {
            if (auth.getUri().equals(authorization.getUri())) {
                auth.setMethods(auth.getMethods() | authorization.getMethods());
                return;
            }
        }

        authorizations.add(authorization);
    }

    private class TicketValidityCheckTask implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("Ticket Validity Check");
            List<TicketGrantMessage> tickets = dao.getTickets();
            for (TicketGrantMessage ticket : tickets) {
                Face face = ticket.getFace();
                long currentTimeInSeconds = System.currentTimeMillis() / 1000;
                if (currentTimeInSeconds - face.getTimestamp() > face.getLifetime()) {
                    revokeTicket(ticket.getId());
                }
            }
        }
    }
}
