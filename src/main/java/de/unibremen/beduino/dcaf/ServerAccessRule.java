package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.ServerAccessRuleSerializer.class)
class ServerAccessRule {
    private ServerInfo serverInfo;
    private String resource;
    private int methods;
    private List<UpdateAttribute> updateAttributes = new ArrayList<>();

    ServerAccessRule(@JsonProperty("serverInfo") ServerInfo serverInfo,
                     @JsonProperty("resource") String resource,
                     @JsonProperty("methods") int methods,
                     @JsonProperty("updateAttributes") List<UpdateAttribute> updateAttributes) {
        this.serverInfo = serverInfo;
        this.resource = resource;
        this.methods = methods;
        this.updateAttributes = updateAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ServerAccessRule oResource = (ServerAccessRule) o;
        return this.serverInfo.equals(oResource.serverInfo) && this.resource.equals(oResource.resource) &&
                this.methods == oResource.methods;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (serverInfo != null && resource != null) {
            result = 31 * result + serverInfo.hashCode();
            result = 31 * result + resource.hashCode();
            result = 31 * result + methods;
            result = 31 * result + updateAttributes.hashCode();
        }
        return result;
    }

    String getServerHost() {
        return serverInfo.getHost();
    }

    String getResource() {
        return resource;
    }

    ServerInfo getServerInfo() {
        return serverInfo;
    }

    void setServerInfo(ServerInfo info) {
        this.serverInfo = info;
    }

    int getMethods() {
        return methods;
    }

    void setMethods(int methods) {
        this.methods = methods;
    }

    void addUpdateAttribute(String type, Integer pin, Integer port, String method, String url) {
        UpdateAttribute updateAttribute = new UpdateAttribute(type, pin, port, method, url);
        if (!updateAttributes.contains(updateAttribute)) {
            updateAttributes.add(updateAttribute);
        }
    }

    void addUpdateAttribute(UpdateAttribute updateAttribute) {
        if (!updateAttributes.contains(updateAttribute)) {
            updateAttributes.add(updateAttribute);
        }
    }

    List<UpdateAttribute> getUpdateAttributes() { return updateAttributes; }
}
