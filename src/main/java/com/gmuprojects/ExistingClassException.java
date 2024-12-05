package com.gmuprojects;

/**
 * Exception that is called when an existing class is trying to be added.
 */
public class ExistingClassException extends Exception {
    public ExistingClassException() {}

    public ExistingClassException(String errorMessage)
    {
        super(errorMessage);
    }
}
