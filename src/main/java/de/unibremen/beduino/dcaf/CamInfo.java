package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.CamInfoSerializer.class)
public class CamInfo {
    private String identifier;
    private String name;

    CamInfo(@JsonProperty("id") String identifier, @JsonProperty("name") String name) {
        this.identifier = identifier;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        CamInfo oCam = (CamInfo) o;
        return this.identifier.equals(oCam.identifier) && this.name.equals(oCam.name);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (identifier != null && name != null) {
            result = 31 * result + identifier.hashCode();
            result = 31 * result + name.hashCode();
        }
        return result;
    }

    String getId() {
        return identifier;
    }

    String getName() {
        return name;
    }
}
