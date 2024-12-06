package com.yegoD;

/**
 * Exception that is called when an existing class is trying to be added to the class checker.
 */
public class ExistingClassException extends Exception {
    public ExistingClassException() {}

    public ExistingClassException(String errorMessage)
    {
        super(errorMessage);
    }
}
