package de.unibremen.beduino.dcaf;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author Norman Lipkow
 */
public class DcafEncodingType {
    public static final String SAM = "0";
    public static final String SAI = "1";
    public static final String CAI = "2";
    public static final String E = "3";
    public static final String K = "4";
    public static final String TS = "5";
    public static final String L = "6";
    public static final String G = "7";
    public static final String F = "8";
    public static final String V = "9";
    public static final String A = "10";
    public static final String D = "11";
    public static final String N = "12";
    public static final String UA = "13";
    public static final String S = "14";
    public static final String UH = "15";

    public static final HashBiMap<String, String> registry;
    static {
        BiMap<String, String> temp = HashBiMap.create();
        temp.put(SAM, "SAM");
        temp.put(SAI, "SAI");
        temp.put(CAI, "CAI");
        temp.put(E, "E");
        temp.put(K, "K");
        temp.put(TS, "TS");
        temp.put(L, "L");
        temp.put(G, "G");
        temp.put(F, "F");
        temp.put(V, "V");
        temp.put(A, "A");
        temp.put(D, "D");
        temp.put(N, "N");
        temp.put(UA, "UA");
        temp.put(S, "S");
        temp.put(UH, "UH");
        registry = HashBiMap.create(temp);
    }

    public static String fromStringToEncoding(String value) {
        String encodedValue = registry.inverse().get(value);

        if (encodedValue != null) {
            return encodedValue;
        } else {
            return null;
        }
    }

    public static String toString(String encodingType) {
        String value = registry.get(encodingType);

        if (value != null) {
            return value;
        } else {
            return null;
        }
    }
}
