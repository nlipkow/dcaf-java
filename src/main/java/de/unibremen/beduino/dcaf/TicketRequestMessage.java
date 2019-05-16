package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * von CAM zu SAM
 *
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.TicketRequestMessageSerializer.class)
class TicketRequestMessage {
	private final String sam;
	private final List<Authorization> sai = new ArrayList<>();
	private final long timestamp;
	private final String updateAttributes;
	private final String signature;

	@JsonCreator
	TicketRequestMessage(@JsonProperty(DcafEncodingType.SAM) String clientDescription,
	                            @JsonProperty(DcafEncodingType.SAI) List<Authorization> sai,
								@JsonProperty(DcafEncodingType.TS) long timestamp,
						 		@JsonProperty(DcafEncodingType.UA) String attributes,
								@JsonProperty(DcafEncodingType.S) String signature) {
		this.sam = clientDescription;
		this.sai.addAll(sai);
		this.timestamp = timestamp;
		this.updateAttributes = attributes;
		this.signature = signature;
	}

	String getSamUrl() {
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
