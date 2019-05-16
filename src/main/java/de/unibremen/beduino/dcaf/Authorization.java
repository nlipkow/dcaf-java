package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.AuthorizationSerializer.class)
class Authorization {
	private String uri;
	private String resourcePath;
	private String host;
	private int methods;

	@JsonCreator
	Authorization(Object[] array) throws URISyntaxException {
		if (array.length != 2)
			throw new IllegalArgumentException("Wrong array length.");

		if (array[0] instanceof String && array[1] instanceof Integer) {
			initializeURIAttributes(array);
		} else {
			throw new IllegalArgumentException("Wrong types of array elements");
		}
	}

	Authorization(String uri, int methods) {
		URI url = null;
		try {
			url = new URI(uri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (url != null) {
			this.resourcePath = url.getPath();
			this.host = url.getHost();
			this.uri = url.toString();
		} else {
			this.uri = uri;
			this.host = "";
			this.resourcePath = "";
		}
		this.methods = methods;
	}

	String getResourcePath() {
		return resourcePath;
	}

	String getUri() {
		return uri;
	}

	String getHostURL() {
		return host;
	}

	int getMethods() {
		return methods;
	}

	void setMethods(int methods) {
		this.methods = methods;
	}

	@Override
	public String toString() {
		return "[" + uri + ": " + Method.getMethodsFromIntValue(methods) + "]";
	}

	private void initializeURIAttributes(Object[] array) throws URISyntaxException {
		URI url =  new URI((String) array[0]);
		this.resourcePath = url.getPath();
		this.host = url.getHost();
		this.uri = url.toString();
		this.methods = ((Integer) array[1]).byteValue();
	}
}
