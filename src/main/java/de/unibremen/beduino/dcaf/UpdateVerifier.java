package de.unibremen.beduino.dcaf;

import de.unibremen.beduino.dcaf.exceptions.AttributesInvalidException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author Christopher Reusche
 */

class UpdateVerifier {
    private static Logger logger = LoggerFactory.getLogger(UpdateVerifier.class);
    private static final String CERT_PATH = "src/main/resources/update_certs/mykey.jks";
    private static final String CERT_PW = "tester";
    private static final String CERT_ALIAS = "wadi";

    private String attributes;
    private String signature;

    UpdateVerifier(TicketRequestMessage ticketRequestMessage) {
        this.attributes = ticketRequestMessage.getUpdateAttributes();
        this.signature = ticketRequestMessage.getSignature();
    }

    boolean verify() throws CertificateException, NoSuchAlgorithmException, IOException,
            KeyStoreException, SignatureException, InvalidKeyException {

        return checkAttributes(this.signature);
    }

    private PublicKey getPublicKey() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream readStream = new FileInputStream(CERT_PATH);
        ks.load(readStream, CERT_PW.toCharArray());
        PublicKey publicKey = ks.getCertificate(CERT_ALIAS).getPublicKey();
        readStream.close();
        return publicKey;
    }

    private static boolean verifySignature(byte[] data, PublicKey key, byte[] signature) throws NoSuchAlgorithmException,
            SignatureException, InvalidKeyException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initVerify(key);
        signer.update(data);
        boolean result = signer.verify(signature);
        logger.info("verifySignature: {}", result);

        return result;
    }

    private String getAttributesDecoded() {
        String attributesDecoded = null;

        if (this.attributes != null) {
            try {
                attributesDecoded = new String(Base64.getDecoder().decode(this.attributes));
            } catch (IllegalArgumentException i) {
                logger.warn("could not decode attributes with base64 decoder");
                return null;
            }
        }

        return attributesDecoded;
    }

    private JSONObject getAttributesAsJson() {
        if (getAttributesDecoded() == null)
            return null;

        return new JSONObject(getAttributesDecoded());
    }

    public List<UpdateAttribute> getUpdateAttributes() {
        List<UpdateAttribute> updateAttributes = new ArrayList<>();
        if (getAttributesAsJson() == null || getAttributesAsJson().isEmpty()) {
            return updateAttributes;
        }

        JSONArray attributesArray = getAttributesAsJson().getJSONArray("attributes");

        for (int i = 0; i < attributesArray.length(); i++) {
            JSONObject array = getAttributesAsJson().getJSONArray("attributes").getJSONObject(1);

            if (attributesArray.getJSONObject(i).has("type")) {
                if (attributesArray.getJSONObject(i).getString("type").equals("gpio_in") ||
                        attributesArray.getJSONObject(i).getString("type").equals("gpio_out")) {
                    updateAttributes.add(new UpdateAttribute(attributesArray.getJSONObject(i).getString("type"),
                            Integer.parseInt(attributesArray.getJSONObject(i).getString("pin")),
                            Integer.parseInt(attributesArray.getJSONObject(i).getString("port")),
                            null,null));
                } else if (attributesArray.getJSONObject(i).getString("type").equals("coap in") ||
                        attributesArray.getJSONObject(i).getString("type").equals("coap out")) {
                    updateAttributes.add(new UpdateAttribute(attributesArray.getJSONObject(i).getString("type"),
                            null,null,
                            attributesArray.getJSONObject(i).getString("method"),
                            attributesArray.getJSONObject(i).getString("url")));
                }
            }
        }

        return updateAttributes;
    }

    public boolean attributesAllowed(List<UpdateAttribute> updateAttributes) {
        if (getUpdateAttributes().isEmpty()) {
            return false;
        }

        for (UpdateAttribute updateAttribute : getUpdateAttributes()) {
            if (!updateAttributes.contains(updateAttribute)) {
                return false;
            }
        }

        return true;
    }

    public String getHash() {
        JSONObject attributesAsJson = getAttributesAsJson();
        return attributesAsJson != null ? attributesAsJson.getString("hash") : null;
    }

    private boolean checkAttributes(String signature) throws IOException, KeyStoreException,
            CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {

        if (getAttributesDecoded() == null) {
            return false;
        }

        JSONObject attributesAsJson = getAttributesAsJson();

        if (attributesAsJson == null) {
            return false;
        }

        if (!attributesAsJson.has("attributes") || !attributesAsJson.has("hash")) {
            throw new AttributesInvalidException();
        }

        byte[] decodedSignature;
        try {
            decodedSignature = Base64.getDecoder().decode(signature.getBytes("UTF-8"));
        } catch (IllegalArgumentException i) {
            logger.warn("could not decode attributes with base64 decoder");
            return false;
        }

        return verifySignature(getAttributesDecoded().getBytes(), getPublicKey(), decodedSignature);
    }
}
