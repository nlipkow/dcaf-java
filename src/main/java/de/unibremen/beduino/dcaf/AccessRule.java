package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.AccessRuleSerializer.class)
public class AccessRule {

    private String ruleId;
    private String camIdentifier;
    private List<ServerAccessRule> serverAccessRules = new ArrayList<>();
    private long expirationTime;

    AccessRule(String ruleId, String camIdentifier) {
        this.ruleId = ruleId;
        this.camIdentifier = camIdentifier;
    }

    AccessRule(@JsonProperty("id") String ruleId, @JsonProperty("camIdentifier") String camIdentifier,
               @JsonProperty("accessRules") List<ServerAccessRule> serverAccessRules,
               @JsonProperty("expiration") long expirationTime) {
        this.ruleId = ruleId;
        this.camIdentifier = camIdentifier;
        this.serverAccessRules = serverAccessRules;
        this.expirationTime = expirationTime;
    }

    void addRule(ServerInfo info, String resource, int methods, List<UpdateAttribute> updateAttributes) {
        ServerAccessRule ruleResource = new ServerAccessRule(info, resource, methods, updateAttributes);
        if (!serverAccessRules.contains(ruleResource)) {
            serverAccessRules.add(ruleResource);
        }
    }

    void addRule(ServerAccessRule serverAccessRule) {
        if (!serverAccessRules.contains(serverAccessRule)) {
            serverAccessRules.add(serverAccessRule);
        }
    }

    String getCamIdentifier() {
        return camIdentifier;
    }

    String getId() {
        return ruleId;
    }

    long getExpirationTime() {
        return expirationTime;
    }

    List<ServerAccessRule> getServerAccessRules() {
        return serverAccessRules;
    }
}
