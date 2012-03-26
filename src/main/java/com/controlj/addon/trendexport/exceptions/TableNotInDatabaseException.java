package com.controlj.addon.trendexport.exceptions;

public class TableNotInDatabaseException extends Exception
{
    private String error;
    private Throwable throwable;

    public TableNotInDatabaseException()
    {
        super();
        error = "Source Not Found";
    }

    public TableNotInDatabaseException(String message)
    {
        super(message);
        error = message;
    }

    public TableNotInDatabaseException(String message, Throwable t)
    {
        super(message);
        error = message;
        throwable = t;
    }

    public String getError()
    {
        return error;
    }

    public Throwable getCause()
    {
        return throwable;
    }
}
