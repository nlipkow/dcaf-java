package de.unibremen.beduino.dcaf;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 */
enum MacMethod {
    HMAC_SHA_256("HMAC_SHA_256", "HmacSHA256"),
    HMAC_SHA_384("HMAC_SHA_384", "HmacSHA384"),
    HMAC_SHA_512("HMAC_SHA_512", "HmacSHA512");

    private final String encoding;
    private final String algorithmName;

    MacMethod(String encoding, String algorithmName) {
        this.encoding = encoding;
        this.algorithmName = algorithmName;
    }

    String getEncoding() {
        return encoding;
    }

    String getAlgorithmName() {
        return algorithmName;
    }}
