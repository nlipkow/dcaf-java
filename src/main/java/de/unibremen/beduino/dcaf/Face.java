package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.FaceSerializer.class)
class Face {
	private final List<Authorization> sai;
	private final long ts;
	private final long lifetime; // in seconds
    private final MacMethod macMethod;
    private final String updateHashEncrypted;

	Face(@JsonProperty(DcafEncodingType.SAI) List<Authorization> sai, @JsonProperty(DcafEncodingType.TS) long ts,
		 @JsonProperty(DcafEncodingType.L) long lifetime, @JsonProperty(DcafEncodingType.G)  String encoding,
		 @JsonProperty(DcafEncodingType.UH) String updateHashEncrypted) {
		this.sai = sai;
        if (ts <= 0) {
            this.ts = System.currentTimeMillis() / 1000;
        } else {
            this.ts = ts;
        }
        this.lifetime = lifetime;
		this.macMethod = MacMethod.valueOf(encoding);
		this.updateHashEncrypted = updateHashEncrypted;
	}

	long getLifetime() {
		return lifetime;
	}

	MacMethod getMacMethod() {
		return macMethod;
	}

	List<Authorization> getSai() {
		return sai;
	}

	long getTimestamp() {
		return ts;
	}

	String getUpdateHashEncrypted() {
		return updateHashEncrypted;
	};
}
