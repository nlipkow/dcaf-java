package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * von C zu CAM
 *
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.AccessRequestSerializer.class)
class AccessRequest {
    private String sam;
    private List<Authorization> sai = new ArrayList<>();
    private long timestamp;
    private final String updateAttributes;
    private final String signature;

    @JsonCreator
    AccessRequest(@JsonProperty(DcafEncodingType.SAM) String sam,
                         @JsonProperty(DcafEncodingType.SAI) List<Authorization> sai,
                         @JsonProperty(DcafEncodingType.TS) long timestamp,
                         @JsonProperty(DcafEncodingType.UA) String attributes,
                         @JsonProperty(DcafEncodingType.S) String signature) {
        this.sam = sam;
        this.sai.addAll(sai);
        this.timestamp = timestamp;
        this.updateAttributes = attributes;
        this.signature = signature;
    }

    String getSam() {
        return sam;
    }

    List<Authorization> getSai() {
        return sai;
    }

    long getTimestamp() {
        return timestamp;
    }

    public String getUpdateAttributes() {
        return updateAttributes;
    }

    public String getSignature() {
        return signature;
    }
}
