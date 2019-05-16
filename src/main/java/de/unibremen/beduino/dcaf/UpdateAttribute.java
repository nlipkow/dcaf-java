package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

@JsonSerialize(using = DcafJsonSerializer.UpdateAttributeSerializer.class)
class UpdateAttribute {
    private String type;
    private Integer pin;
    private Integer port;
    private String method;
    private String url;

    UpdateAttribute(@JsonProperty("type") String type,
                    @JsonProperty("pin") Integer pin,
                    @JsonProperty("port") Integer port,
                    @JsonProperty("method") String method,
                    @JsonProperty("url") String url) {
        this.type = type;
        this.pin = pin;
        this.port = port;
        this.method = method;
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UpdateAttribute)) {
            return false;
        }

        UpdateAttribute updateAttribute = (UpdateAttribute) o;

        return Objects.equals(updateAttribute.getMethod(), this.method) &&
                Objects.equals(updateAttribute.getPin(), this.pin) &&
                Objects.equals(updateAttribute.getPort(), this.port) &&
                Objects.equals(updateAttribute.getType(), this.type) &&
                Objects.equals(updateAttribute.getUrl(), this.url);
    }

    @Override
    public String toString() {
        return "Type: " + this.type;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        if (pin != null) {
            result = 31 * result + pin;
        }

        if (port != null) {
            result = 31 * result + port;
        }

        if (method != null) {
            result = 31 * result + method.hashCode();
        }

        if (url != null) {
            result = 31 * result + url.hashCode();
        }

        return result;
    }

    public boolean isGpio() {
        return this.pin != null && this.port != null;
    }

    public String getType() {
        return type;
    }

    public Integer getPin() {
        return pin;
    }

    public Integer getPort() {
        return port;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }


    public static void main(String[] args) {
        UpdateAttribute updateAttribute = new UpdateAttribute("gpio_in", 12, 3, null, null);
        UpdateAttribute updateAttribute2 = new UpdateAttribute("gpio_in", 12, 3, null, null);

        if (Objects.equals(updateAttribute, updateAttribute2))
            System.out.println("true!!!");

        System.exit(0);
    }
}