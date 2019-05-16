package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.UUID;

/**
 * von SAM zu CAM zu C
 *
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.TicketGrantMessageSerializer.class)
@JsonPropertyOrder({ "F", "V" })
public class TicketGrantMessage {
	private static final String ID = "id";
	private String ticketId;
	private final Face face; // F
	private final Verifier verifier; // V
	private String camIdentifier = null;
	private String serverHost = null;

	TicketGrantMessage(Face face, Verifier verifier) {
		this.face = face;
		this.verifier = verifier;
		this.ticketId = UUID.randomUUID().toString();
	}

	TicketGrantMessage(@JsonProperty(ID) String ticketId, @JsonProperty(DcafEncodingType.F) Face face,
					   @JsonProperty(DcafEncodingType.V) byte[] verifier, @JsonProperty("cam") final String camIdentifier,
					   @JsonProperty("server") final String serverHost) {
		this.face = face;
		this.verifier = new Verifier(verifier);
		this.ticketId = ticketId;
		this.camIdentifier = camIdentifier;
		this.serverHost = serverHost;
	}

	String getId() {
		return ticketId;
	}

	Face getFace() {
		return face;
	}

	Verifier getVerifier() {
		return verifier;
	}

	String getCamIdentifier() {
		return camIdentifier;
	}

	String getServerHost() {
		return serverHost;
	}

	void setCamIdentifier(final String cam) {
		this.camIdentifier = cam;
	}

	void setServerHost(final String server) {
		this.serverHost = server;
	}
}
