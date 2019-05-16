package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.util.ServerNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class JsonPskStore implements PskStore {

    private static Logger logger = LoggerFactory.getLogger(JsonPskStore.class);
    private static final String KEY_PATH =  getCurrentDirectoryPath() + "keys.json";

    private final ObjectMapper jsonMapper = new ObjectMapper(new JsonFactory());

    JsonPskStore() {
        createKeyFile();
    }

    @Override
    public byte[] getKey(final String identity) {

        if (identity == null) {
            throw new NullPointerException("identity must not be null");
        } else {
            return getKeyFromJson(readJsonKeyFile(), identity);
        }
    }

    @Override
    public byte[] getKey(final ServerNames serverNames, final String identity) {
        if (serverNames != null) {
            logger.warn("SNI is not supported");
            return null;
        } else if (identity == null) {
            throw new NullPointerException("identity must not be null");
        } else {
            return getKey(identity);
        }
    }

    @Override
    public String getIdentity(final InetSocketAddress peerAddress) {
        return getIdentityFromJsonByPeerAddress(readJsonKeyFile(), peerAddress);
    }

    @Override
    public String getIdentity(final InetSocketAddress peerAddress, final ServerNames virtualHost) {
        return getIdentity(peerAddress);
    }

    /**
     * Adds a shared key for a peer.
     * <p>
     * If the key already exists, it will be replaced.
     *
     * @param peerAddress the IP address and port to use the key for
     * @param identity the PSK identity
     * @param key the shared key
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public void addKnownPeer(final InetSocketAddress peerAddress, final String identity, final String key) {

        if (peerAddress == null) {
            throw new NullPointerException("peer address must not be null");
        } else {
            setKey(identity, key);
            setPeerAddressForIdentity(readJsonKeyFile(), peerAddress, identity);
        }
    }

    /**
     * Sets a key value for a given identity.
     * <p>
     * If the key already exists, it will be replaced.
     *
     * @param identity
     *            the identity associated with the key
     * @param key
     *            the key used to authenticate the identity
     */
    void setKey(final String identity, final String key) {

        if (identity == null) {
            throw new NullPointerException("identity must not be null");
        } else if (key == null) {
            throw new NullPointerException("key must not be null");
        } else {
            JsonNode jsonKeyNodes = readJsonKeyFile();
            addIdentityAndKeyToJson(jsonKeyNodes, identity, key);
        }
    }

    /**
     * Deletes a key value for a given identity.
     *
     * @param identity
     *            the identity associated with the key
     */
    void deleteKey(final String identity) {

        if (identity == null) {
            throw new NullPointerException("identity must not be null");
        } else {
            JsonNode jsonKeyNodes = readJsonKeyFile();
            deletePSKFromNode(jsonKeyNodes, identity);
        }
    }

    private void setPeerAddressForIdentity(JsonNode jsonKeyNodes, InetSocketAddress peerAddress, String identity) {
        if (jsonKeyNodes != null) {
            JsonNode keys = jsonKeyNodes.get("keys");
            if (keys instanceof ArrayNode) {

                for (int i = 0; i < keys.size(); i++) {
                    JsonNode identityValue = keys.get(i).get("identity");
                    if (identity.equals(identityValue.textValue())) {
                        ObjectNode peerAddressNode = new JsonNodeFactory(false).objectNode();
                        peerAddressNode.put("host", peerAddress.getHostString());
                        peerAddressNode.put("port", peerAddress.getPort());

                        ((ObjectNode) keys.get(i)).set("peerAddress", peerAddressNode);
                    }
                }
                writeNodeToJsonKeyFile(jsonKeyNodes);
            }
        }
    }

    private void addIdentityAndKeyToJson(JsonNode jsonKeyNodes, final String identity, final String key) {
        if (jsonKeyNodes != null) {
            if (jsonKeyNodes instanceof NullNode) {
                ObjectNode parentKeysNode = new JsonNodeFactory(false).objectNode();
                ArrayNode arrayNode = new JsonNodeFactory(false).arrayNode();
                parentKeysNode.set("keys", arrayNode);

                arrayNode.add(createNewIdentityKeyObjectNode(identity, key));
                jsonKeyNodes = parentKeysNode;
            } else if (jsonKeyNodes instanceof ObjectNode && !jsonKeyNodes.has("keys")) {
                ArrayNode arrayNode = new JsonNodeFactory(false).arrayNode();
                ((ObjectNode)jsonKeyNodes).set("keys", arrayNode);

                arrayNode.add(createNewIdentityKeyObjectNode(identity, key));
            } else {
                addIdentityKeyToExistingJsonFile(jsonKeyNodes, identity, key);
            }
            writeNodeToJsonKeyFile(jsonKeyNodes);
        }
    }

    private void addIdentityKeyToExistingJsonFile(JsonNode jsonKeyNodes, String identity, String key) {
        JsonNode keys = jsonKeyNodes.get("keys");
        if (keys instanceof ArrayNode) {
            boolean added = false;

            for (JsonNode identityKeyEntry : keys) {
                JsonNode identityValue = identityKeyEntry.get("identity");
                if (identity.equals(identityValue.textValue())) {
                    ((ObjectNode) identityKeyEntry).put("psk", key);
                    added = true;
                    break;
                }
            }
            if (!added) {
                ((ArrayNode) keys).add(createNewIdentityKeyObjectNode(identity, key));
            }
        }
    }

    private void deletePSKFromNode(JsonNode jsonKeyNodes, String identity) {
        if (jsonKeyNodes != null) {
            JsonNode keys = jsonKeyNodes.get("keys");
            if (keys instanceof ArrayNode) {
                boolean removed = false;

                for (int i = 0; i < keys.size(); i++) {
                    JsonNode identityValue = keys.get(i).get("identity");
                    if (identity.equals(identityValue.textValue())) {
                        ((ArrayNode) keys).remove(i);
                        removed = true;
                        break;
                    }
                }

                if (removed) {
                    writeNodeToJsonKeyFile(jsonKeyNodes);
                }
            }
        }
    }

    private void writeNodeToJsonKeyFile(JsonNode jsonKeyNodes) {
        ObjectWriter writer = jsonMapper.writer(new DefaultPrettyPrinter());

        File file = new File(KEY_PATH);
        file.getParentFile().mkdirs();

        try (Writer fileWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            writer.writeValue(fileWriter, jsonKeyNodes);
        } catch (IOException e) {
            logger.error("Error while trying to write to file " + KEY_PATH, e);
        }
    }

    private ObjectNode createNewIdentityKeyObjectNode(final String identity, final String key) {
        ObjectNode newNode = new JsonNodeFactory(false).objectNode();
        newNode.put("identity", identity);
        newNode.put("psk", key);

        return newNode;
    }


    private static byte[] getKeyFromJson(JsonNode node, final String identity) {

        if (node != null) {
            JsonNode keys = node.get("keys");
            if (keys instanceof ArrayNode) {
                for (Iterator<JsonNode> it = keys.elements(); it.hasNext();) {
                    JsonNode next = it.next();
                    if (identity.equals(next.get("identity").textValue())) {
                        return next.get("psk").textValue().getBytes();
                    }
                }
            }
        }
        return null;
    }

    private static String getIdentityFromJsonByPeerAddress(JsonNode node, final InetSocketAddress address) {

        if (node != null) {
            JsonNode keys = node.get("keys");
            if (keys instanceof ArrayNode) {
                for (Iterator<JsonNode> it = keys.elements(); it.hasNext();) {
                    JsonNode next = it.next();
                    InetSocketAddress current = new InetSocketAddress(next.get("peerAddress").get("host").textValue(),
                            Integer.parseInt(next.get("peerAddress").get("port").textValue()));
                    if (address.equals(current)) {
                        return next.get("identity").textValue();
                    }
                }
            }
        }
        return null;
    }

    private JsonNode readJsonKeyFile() {
        try {
            JsonNode jsonNode = jsonMapper.readTree(new InputStreamReader(new FileInputStream(KEY_PATH), StandardCharsets.UTF_8));
            if (jsonNode == null) {
                return new JsonNodeFactory(false).nullNode();
            }
            return jsonNode;
        }catch (FileNotFoundException e) {
            logger.error("Failed to read json file", e);
        } catch (JsonProcessingException e) {
            logger.error("Error while trying to read json tree from key file", e);
        } catch (IOException e) {
            logger.error("Could not initialize an input stream for key file", e);
        }

        return null;
    }

    private void createKeyFile() {
        Path file = Paths.get(KEY_PATH);

        try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        } catch (FileAlreadyExistsException e) {
            logger.info("File " + file.toAbsolutePath() + " already exists.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCurrentDirectoryPath() {
        return System.getProperty("user.dir") + System.getProperty("file.separator") + "dao/";
    }
}
