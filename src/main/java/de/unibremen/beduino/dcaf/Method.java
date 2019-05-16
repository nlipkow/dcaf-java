package de.unibremen.beduino.dcaf;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 */
enum Method {
	GET(1),
	POST(2),
	PUT(4),
	DELETE(8),
	PATCH(16);

	private final int bit;

	Method(int bit) {
		this.bit = bit;
	}

	int getBit() {
		return bit;
	}

	static Set<Method> getMethodsFromIntValue(int methods) {
		HashSet<Method> result = new HashSet<>();

		for (Method m : Method.values()) {

			int andResult = m.getBit() & methods;

			if (andResult != 0) {
				result.add(m);
			}
		}

		return result;
	}

}