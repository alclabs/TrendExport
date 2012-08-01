package com.controlj.addon.trendexport.exceptions;


public class NoStatisticsException extends Exception
{
    private String error;

    public NoStatisticsException()
    {
        super();
        error = "Source Not Found";
    }

    public NoStatisticsException(String message)
    {
        super(message);
        error = message;
    }

    public String getError()
    {
        return error;
    }
}
