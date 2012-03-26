package com.controlj.addon.trendexport.exceptions;

public class TableNotInDatabaseException extends Exception
{
    private String error;

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

    public String getError()
    {
        return error;
    }
}
