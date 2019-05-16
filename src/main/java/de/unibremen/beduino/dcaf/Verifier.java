package de.unibremen.beduino.dcaf;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 */
class Verifier {
	private final byte[] verifier;

	Verifier(byte[] verifier) {
		this.verifier = verifier;
	}

	byte[] getVerifier() {
		return verifier;
	}
}
