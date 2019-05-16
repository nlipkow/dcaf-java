package de.unibremen.beduino.dcaf.exceptions;

/**
 * @author Connor Lanigan
 * @author Sven Höper
 */
public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(Exception e) {
        super(e);
    }

    public ResourceNotFoundException() {}
}
