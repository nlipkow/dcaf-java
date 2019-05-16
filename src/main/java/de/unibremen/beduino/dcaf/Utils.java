package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import de.unibremen.beduino.dcaf.exceptions.MacFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
public class Utils {

    private static Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory());

    /**
     * DCAF MediaType to answer with
     *
     * @see <a href="https://www.iana.org/assignments/core-parameters/core-parameters.xhtml#table-content-formats">Constrained RESTful Environments (CoRE) Parameters: CoAP Content-Formats</a>
     */

    private Utils() {

    }

    /**
     * Compute MAC for a given byte array
     *
     * @param macMethod key generation method to use
     * @param key       key to use when applying the KDF
     * @param input     input we want to compute a MAC for
     * @return computed MAC
     */
    static byte[] computeMac(final MacMethod macMethod,
                             final byte[] key,
                             final byte[] input)
            throws MacFailedException, InvalidKeyException {

        try {
            Mac mac = Mac.getInstance(macMethod.getAlgorithmName());
            SecretKeySpec secretKey = new SecretKeySpec(key, macMethod.getAlgorithmName());
            mac.init(secretKey);

            return mac.doFinal(input);
        } catch (InvalidKeyException e) {
            throw e;
        } catch (Exception e) {
            throw new MacFailedException(e);
        }
    }

    /**
     * Compute MAC for a given {@see Face}
     *
     * @param macMethod {@see MacMethod} to use
     * @param key       Key to use
     * @param face      Face to serialze and compute the MAC for
     * @return MAC
     * @throws MacFailedException  in case there was an error
     * @throws InvalidKeyException if the given key was inappropriate to initialize the MAC
     */
    public static byte[] computeMac(final MacMethod macMethod,
                                    final byte[] key,
                                    final Face face)
            throws MacFailedException, InvalidKeyException {

        Optional<byte[]> input = serializeCbor(face);

        if (input.isPresent()) {
            return computeMac(macMethod, key, input.get());
        }
        throw new MacFailedException();
    }


    /**
     * Serialize given object as CBOR
     *
     * @param object to serialize
     * @return Optional of serialized input or Optional.empty()
     */
    static <T> Optional<byte[]> serializeCbor(T object) {
        try {
            return Optional.of(CBOR_MAPPER.writeValueAsBytes(object));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Deserialize given object from CBOR
     *
     * @param requestPayload to deserialize
     * @param valueType      Type to deserialize as
     * @return Optional of deserialized input or Optional.empty()
     */
    static <T> Optional<T> deserializeCbor(byte[] requestPayload, Class<T> valueType) {
        try {
            return Optional.of(CBOR_MAPPER.readValue(requestPayload, valueType));
        } catch (IOException e) {
            logger.error("Error 500", e);
            return Optional.empty();
        }
    }

    static String cborPayloadToPrettyString(byte[] requestPayload) {
        try {
            JsonNode opt = CBOR_MAPPER.readTree(requestPayload);
            transformDcafEncodingsToStringValues(opt);
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(opt);
        } catch (IOException e) {
            logger.error("Failed to parse payload", e);
            return null;
        }
    }

    public static void initializeTestData() {
        DAO dao = FileDAO.getInstance();

        // Create Client Authorization Manager Info
        CamInfo camInfo = new CamInfo("/127.0.0.1:8002", "cam");

        // Create Server and his resourceUPDATE
        Resource resource = new Resource("/update", 11);
        Resource resource2 = new Resource("/temperature", 1);
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(resource);
        resourceList.add(resource2);
        ServerInfo serverInfo = new ServerInfo("[2001:DB8::dcaf:1234]", "key",
                0, resourceList);

        //create update attributes
        UpdateAttribute updateAttribute = new UpdateAttribute("gpio_in", 12, 3, null, null);
        UpdateAttribute updateAttribute2 = new UpdateAttribute("coap in", null, null, "GET", "/foo");
        List<UpdateAttribute> updateAttributes = new ArrayList<>();
        updateAttributes.add(updateAttribute);
        updateAttributes.add(updateAttribute2);

        // Create access rule for Client Authorization Manager
        AccessRule accessRule = new AccessRule("Update Access", camInfo.getId());
        accessRule.addRule(serverInfo, "/update", 3, updateAttributes);

        // Save Data to DAO
        dao.saveClientAuthorizationManager(camInfo);
        dao.saveServerInformation(serverInfo);
        dao.saveAccessRule(accessRule);
    }

    public static void transformDcafEncodingsToStringValues(JsonNode node) {
        for (String key : DcafEncodingType.registry.keySet()) {
            changeKeyIfPresent(node, key);
        }
    }

    public static void transformStringValuesToDcafEncodings(JsonNode node) {
        for (String key : DcafEncodingType.registry.values()) {
            changeKeyIfPresent(node, key);
        }
    }

    private static void changeKeyIfPresent(JsonNode node, String key) {
        JsonNode value = node.findValue(key);
        while (value != null) {
            JsonNode parent = node.findParent(key);

            if (parent instanceof ObjectNode) {
                String newFieldname;
                if (DcafEncodingType.registry.values().contains(key)) {
                    newFieldname = DcafEncodingType.fromStringToEncoding(key);
                } else {
                    newFieldname = DcafEncodingType.toString(key);
                }

                ((ObjectNode) parent).set(newFieldname, value);
                ((ObjectNode) parent).remove(key);
            }

            value = node.findValue(key);
        }
    }
}
