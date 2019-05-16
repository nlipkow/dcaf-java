package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 *
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.ServerInfoSerializer.class)
public class ServerInfo {

    private String host;
    private String preSharedKey;
    private int seqNumber;
    private List<Resource> resources;

    ServerInfo(@JsonProperty("host") String host, @JsonProperty("preSharedKey") String preSharedKey,
               @JsonProperty("seqNumber") int seqNumber, @JsonProperty("resources") List<Resource> resources) {
        this.host = host;
        this.preSharedKey = preSharedKey;
        this.seqNumber = seqNumber;
        this.resources = resources;
    }

    ServerInfo(String host, String preSharedKey, List<Resource> resources) {
        this.host = host;
        this.preSharedKey = preSharedKey;
        this.seqNumber = 0;
        this.resources = resources;
    }

    String getHost() {
        return host;
    }

    String getPreSharedKey() {
        return preSharedKey;
    }

    int getSeqNumber() {
        return seqNumber;
    }

    List<Resource> getResources() {
        return resources;
    }
}
