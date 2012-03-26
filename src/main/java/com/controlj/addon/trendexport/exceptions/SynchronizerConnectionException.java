package com.controlj.addon.trendexport.exceptions;

public class SynchronizerConnectionException extends Exception
{
    private String error;

    public SynchronizerConnectionException()
    {
        super();
        error = "Unable to connect to Synchronizer";
    }

     public SynchronizerConnectionException(String message, Throwable throwable)
    {
        super(message, throwable);
        error = message;
    }

    public String getError()
    {
        return error;
    }
}
