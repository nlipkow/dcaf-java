package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Norman Lipkow
 */
class FileDAO implements DAO {
    private static Logger logger = LoggerFactory.getLogger(FileDAO.class);
    private static final String ACCESS_RULES = "accessRules.json";
    private static final String CAMS = "cams.json";
    private static final String REVOCATIONS = "revocations.json";
    private static final String SERVER_INFO = "serverInfo.json";
    private static final String TICKETS = "tickets.json";
    private static FileDAO dao;

    private ObjectMapper jsonMapper;

    private FileDAO() {
        jsonMapper = new ObjectMapper(new JsonFactory());
        createDAOFiles();
    }

    static FileDAO getInstance() {
        if (dao == null) {
            dao = new FileDAO();
            return dao;
        }
        return dao;
    }

    private void createDAOFiles() {
        String directoryPath = getCurrentDirectoryPath();
        String[] daoFiles = {ACCESS_RULES, CAMS, REVOCATIONS, SERVER_INFO, TICKETS};

        for (String daoFile : daoFiles) {
            Path file = Paths.get(directoryPath + daoFile);

            try {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
            } catch (FileAlreadyExistsException e) {
                logger.info("File " + file.toAbsolutePath() + " already exists.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<TicketGrantMessage> getTickets() {
        return getObjectsFromJson(getCurrentDirectoryPath() + TICKETS, TicketGrantMessage.class);
    }

    @Override
    public TicketGrantMessage getTicket(String ticketId) {
        return getObjectFromId(ticketId, getCurrentDirectoryPath() + TICKETS, TicketGrantMessage.class);
    }

    @Override
    public boolean deleteTicket(String ticketId) {
        return deleteObjectFromId(ticketId, getCurrentDirectoryPath() + TICKETS, TicketGrantMessage.class);
    }

    @Override
    public boolean saveTicket(TicketGrantMessage ticket) {
        return writeToFile(ticket, getCurrentDirectoryPath() + TICKETS);
    }

    @Override
    public boolean updateTicket(TicketGrantMessage ticket) {
        return updateObject(ticket, getCurrentDirectoryPath() + TICKETS, TicketGrantMessage.class);
    }

    @Override
    public List<AccessRule> getAccessRules() {
        return getObjectsFromJson(getCurrentDirectoryPath() + ACCESS_RULES, AccessRule.class);
    }

    @Override
    public AccessRule getAccessRule(String ruleId) {
        return getObjectFromId(ruleId, getCurrentDirectoryPath() + ACCESS_RULES, AccessRule.class);
    }

    @Override
    public boolean deleteAccessRule(String accessRuleId) {
        return deleteObjectFromId(accessRuleId, getCurrentDirectoryPath() + ACCESS_RULES, AccessRule.class);
    }

    @Override
    public boolean saveAccessRule(AccessRule accessRule) {
        if (getObjectFromId(accessRule.getId(), getCurrentDirectoryPath() + ACCESS_RULES, AccessRule.class) != null) {
            logger.error("Failed to persist object " + AccessRule.class + " with id " + accessRule.getId() +
                    ". An object with the same id already existed");
            return false;
        }
        return writeToFile(accessRule, getCurrentDirectoryPath() + ACCESS_RULES);
    }

    @Override
    public boolean updateAccessRule(AccessRule accessRule) {
        return updateObject(accessRule, getCurrentDirectoryPath() + ACCESS_RULES, AccessRule.class);
    }

    @Override
    public List<CamInfo> getClientAuthorizationManagers() {
        return getObjectsFromJson(getCurrentDirectoryPath() + CAMS, CamInfo.class);
    }

    @Override
    public CamInfo getClientAuthorizationManager(String camIdentifier) {
        return getObjectFromId(camIdentifier, getCurrentDirectoryPath() + CAMS, CamInfo.class);
    }

    @Override
    public boolean deleteClientAuthorizationManager(String camIdentifier) {
        return deleteObjectFromId(camIdentifier, getCurrentDirectoryPath() + CAMS, CamInfo.class);
    }

    @Override
    public boolean saveClientAuthorizationManager(CamInfo camInfo) {
        if (getObjectFromId(camInfo.getId(), getCurrentDirectoryPath() + CAMS, CamInfo.class) != null) {
            logger.error("Failed to persist object " + CamInfo.class + " with id " + camInfo.getId() +
                    ". An object with the same id already existed");
            return false;
        }
        return writeToFile(camInfo, getCurrentDirectoryPath() + CAMS);
    }

    @Override
    public boolean updateClientAuthorizationManager(CamInfo camInfo) {
        return updateObject(camInfo, getCurrentDirectoryPath() + CAMS, CamInfo.class);
    }

    @Override
    public List<RevocationTicket> getRevocationTickets() {
        return getObjectsFromJson(getCurrentDirectoryPath() + REVOCATIONS, RevocationTicket.class);
    }

    @Override
    public RevocationTicket getRevocationTicket(String revocationId) {
        return getObjectFromId(revocationId, getCurrentDirectoryPath() + REVOCATIONS, RevocationTicket.class);
    }

    @Override
    public boolean deleteRevocationTicket(String revocationId) {
        return deleteObjectFromId(revocationId, getCurrentDirectoryPath() + REVOCATIONS, RevocationTicket.class);
    }

    @Override
    public boolean saveRevocationTicket(RevocationTicket revocationTicket) {
        return writeToFile(revocationTicket, getCurrentDirectoryPath() + REVOCATIONS);
    }

    @Override
    public boolean updateRevocationTicket(RevocationTicket revocationTicket) {
        return updateObject(revocationTicket, getCurrentDirectoryPath() + REVOCATIONS, RevocationTicket.class);
    }

    @Override
    public List<ServerInfo> getServerInformations() {
        return getObjectsFromJson(getCurrentDirectoryPath() + SERVER_INFO, ServerInfo.class);
    }

    @Override
    public ServerInfo getServerInformation(String host) {
        return getObjectFromId(host, getCurrentDirectoryPath() + SERVER_INFO, ServerInfo.class);
    }

    @Override
    public boolean deleteServerInformation(String host) {
        return deleteObjectFromId(host, getCurrentDirectoryPath() + SERVER_INFO, ServerInfo.class);
    }

    @Override
    public boolean saveServerInformation(ServerInfo serverInfo) {
        if (getObjectFromId(serverInfo.getHost(), getCurrentDirectoryPath() + SERVER_INFO, ServerInfo.class) != null) {
            logger.error("Failed to persist object " + ServerInfo.class + " with host " + serverInfo.getHost() +
                    ". An object with the same host already existed");
            return false;
        }
        return writeToFile(serverInfo, getCurrentDirectoryPath() + SERVER_INFO);
    }

    @Override
    public boolean updateServerInformation(ServerInfo serverInfo) {
        return updateObject(serverInfo, getCurrentDirectoryPath() + SERVER_INFO, ServerInfo.class);
    }

    private <T> List<T> getObjectsFromJson(String path, Class<T> classType)  {
        JsonNode nodes = readJsonFromFile(path);
        List<T> objects = new ArrayList<>();

        try {
            if (nodes != null) {
                if (nodes instanceof ArrayNode) {
                    for (Iterator<JsonNode> iter = nodes.elements(); iter.hasNext(); ) {
                        JsonNode currentNode = iter.next();
                        objects.add(jsonMapper.readValue(currentNode.toString(), classType));
                    }
                } else {
                    objects.add(jsonMapper.readValue(nodes.toString(), classType));
                }
            }
        } catch (IOException e) {
            logger.error("Error while trying to deserialize a json string to " + classType.getName() + " object", e);
        }

        return objects;
    }

    private <T> boolean updateObject(Object dao, String path, Class<T> classType) {
        JsonNode nodes = readJsonFromFile(path);
        String fieldIdName = getIdFieldName(classType);
        JsonNode serializedNode = serializeObjectToJsonNode(dao);

        if (nodes == null || serializedNode == null) {
            return false;
        }

        String id = serializedNode.get(getIdFieldName(classType)).textValue();
        try {
            if (nodes instanceof ArrayNode) {
                for (int i = 0; i < nodes.size(); i++) {
                    JsonNode currentNode = nodes.get(i);
                    String textValue = currentNode.get(fieldIdName).textValue();
                    if (textValue != null && textValue.equals(id)) {
                        copy(serializedNode, currentNode);
                        writeJsonNodeToFile(path, nodes);
                        return true;
                    }
                }
            } else {
                String textValue = nodes.get(getIdFieldName(classType)).textValue();
                if (textValue != null && textValue.equals(id)) {
                    copy(serializedNode, nodes);
                    writeJsonNodeToFile(path, nodes);
                    return true;
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Unable to write json object as String value", e);
        }

        return false;
    }

    private void copy(JsonNode src, JsonNode dest) {

        for (Iterator<String> iter = src.fieldNames(); iter.hasNext();) {
            String currentFieldName = iter.next();
            JsonNode newValue = src.get(currentFieldName);
            ((ObjectNode)dest).replace(currentFieldName, newValue);
//            if (newValue);
        }
    }

    private <T> T getObjectFromId(String id, String path, Class<T> classType) {
        JsonNode nodes = readJsonFromFile(path);
        String fieldIdName = getIdFieldName(classType);

        if (nodes == null) {
            return null;
        }

        try {
            if (nodes instanceof ArrayNode) {
                for (Iterator<JsonNode> iter = nodes.elements(); iter.hasNext(); ) {
                    JsonNode currentNode = iter.next();
                    String textValue = getIdFieldValue(currentNode, fieldIdName);
                    if (textValue != null && textValue.equals(id)) {
                        return jsonMapper.readValue(currentNode.toString(), classType);
                    }
                }
            } else {
                String textValue = getIdFieldValue(nodes, fieldIdName);
                if (textValue != null && textValue.equals(id)) {
                    return jsonMapper.readValue(nodes.toString(), classType);
                }
            }
        } catch (IOException e) {
            logger.error("Error while trying to deserialize a json string to " + classType.getName() + " object", e);
        }

        return null;
    }

    private String getIdFieldValue(JsonNode node, String fieldIdName) {
        JsonNode nodeValue = node.findPath(fieldIdName);
        if (nodeValue instanceof MissingNode) {
            logger.error("No field found for 'id'. This should not have happened");
            return null;
        }

        return nodeValue.textValue();
    }

    private <T> boolean deleteObjectFromId(String id, String path, Class<T> classType) {
        JsonNode nodes = readJsonFromFile(path);
        String fieldIdName = getIdFieldName(classType);

        if (nodes == null) {
            return false;
        }

        try {
            if (nodes instanceof ArrayNode) {
                for (int i = 0; i < nodes.size(); i++) {
                    JsonNode currentNode = nodes.get(i);
                    String textValue = currentNode.get(fieldIdName).textValue();
                    if (textValue != null && textValue.equals(id)) {
                        ((ArrayNode) nodes).remove(i);
                        writeJsonNodeToFile(path, nodes);
                        return true;
                    }
                }
            } else {
                String textValue = nodes.get(getIdFieldName(classType)).textValue();
                if (textValue != null && textValue.equals(id)) {
                    writeJsonNodeToFile(path, null);
                    return true;
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Unable to write json object as String value", e);
        }

        return false;
    }

    private boolean writeToFile(Object dao, String path) {
        JsonNode node = readJsonFromFile(path);
        JsonNode serializedNode = serializeObjectToJsonNode(dao);

        if (serializedNode == null) {
            return false;
        }

        if (node instanceof ArrayNode) {
            for (Iterator<JsonNode> iterator = node.elements(); iterator.hasNext();) {
                JsonNode currentNode = iterator.next();
                if (currentNode.equals(serializedNode)) {
                    return false;
                }
            }
            ((ArrayNode)node).add(serializedNode);
        } else if (node == null){
            node = serializedNode;
        } else {
            if (node.equals(serializedNode)) {
                return false;
            }

            ArrayNode arrayNode = new JsonNodeFactory(false).arrayNode();
            arrayNode.add(node);
            arrayNode.add(serializedNode);
            node = arrayNode;
        }

        Utils.transformDcafEncodingsToStringValues(node);

        try {
            writeJsonNodeToFile(path, node);
            return true;
        } catch (JsonProcessingException e) {
            logger.error("Unable to write json object as String value", e);
        }

        return false;
    }

    private void writeJsonNodeToFile(String path, JsonNode jsonNode) throws JsonProcessingException {
        String json;
        if (jsonNode != null) {
            Utils.transformDcafEncodingsToStringValues(jsonNode);
            json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } else {
            json = "";
        }
        File file = new File(path);
        file.getParentFile().mkdirs();

        try (Writer fileWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            fileWriter.write(json);
        } catch (IOException e) {
            logger.error("Error while trying to write to file " + path, e);
        }
    }

    private JsonNode readJsonFromFile(String path) {
        try {
            if (fileIsEmpty(path)) {
                return null;
            }
            JsonNode jsonNode = jsonMapper.readTree(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            Utils.transformStringValuesToDcafEncodings(jsonNode);
            return jsonNode;
        } catch (JsonProcessingException e) {
            logger.error("Error while trying to read json tree from file " + path, e);
        } catch (IOException e) {
            logger.error("Could not initialize an input stream for file " + path, e);
        }

        return null;
    }

    private boolean fileIsEmpty(String path) {
        try(BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine() == null;
        } catch (IOException e) {
            logger.error("Files to read file " + path, e);
        }
        return false;
    }

    private JsonNode serializeObjectToJsonNode(Object object) {
        try {
            return jsonMapper.readTree(jsonMapper.writeValueAsBytes(object));
        } catch (IOException e) {
            logger.error("Could not serialize " + object.getClass().getName() + " to json", e);
        }

        return null;
    }

    private String getCurrentDirectoryPath() {
        return System.getProperty("user.dir") + System.getProperty("file.separator") + "dao/";
    }

    private <T> String getIdFieldName(Class<T> classType) {
        if (ServerInfo.class.equals(classType)) {
            return "host";
        }

        return "id";
    }
}
