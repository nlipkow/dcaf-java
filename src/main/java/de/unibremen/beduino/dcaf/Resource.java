package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.ResourceSerializer.class)
class Resource {
	private final String url;
	private final int methods;

	Resource(@JsonProperty("url") String url, @JsonProperty("methods") int methods){
		this.url = url;
		this.methods = methods;
	}

	static Resource of(String url, int methods) {
		return new Resource(url, methods);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Resource resource = (Resource) o;

		return url.equals(resource.url) && methods == resource.methods;
	}

	@Override
	public int hashCode() {
		int result = url.hashCode();
		result = 31 * result + methods;
		return result;
	}

	String getResourcePath() {
		return url;
	}

	int getMethods() {
		return methods;
	}
}