package com.yegoD;

/**
 * Exception that is called when an existing class is trying to be added to the class checker.
 */
public class ExistingClassException extends Exception {
    /**
     * No argument constructor.
     */
    public ExistingClassException() {}

    /**
     * Single argument constructor which takes an error message.
     * @param errorMessage Error message for this exception.
     */
    public ExistingClassException(String errorMessage)
    {
        super(errorMessage);
    }
}
