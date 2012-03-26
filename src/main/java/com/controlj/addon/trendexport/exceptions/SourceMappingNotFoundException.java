package com.controlj.addon.trendexport.exceptions;

public class SourceMappingNotFoundException extends Exception
{
    private String error;

    public SourceMappingNotFoundException()
    {
        super();
        error = "Source Not Found";
    }

     public SourceMappingNotFoundException(String message)
    {
        super(message);
        error = message;
    }

    public String getError()
    {
        return error;
    }
}
