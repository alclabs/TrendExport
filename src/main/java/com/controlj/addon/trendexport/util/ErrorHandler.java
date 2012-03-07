/*
* Copyright (c) 2011 Automated Logic Corporation
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/

package com.controlj.addon.trendexport.util;

public class ErrorHandler
{
    private static AlarmHandler alarmHandler;

    public ErrorHandler() { }

    public ErrorHandler(String path)
    {
        alarmHandler = new AlarmHandler(path);
    }

    public static void setAlarmHandlerPath(String path)
    {
        alarmHandler = new AlarmHandler(path);
    }

    public static boolean isAlarmHandlerConfigured()
    {
        return alarmHandler.isConfigured();
    }

    public static void handleError(String message, Throwable throwable)
    {
        Logger.println(message, throwable);
    }

    public static void handleError( String message, Throwable throwable, AlarmHandler.TrendExportAlarm alarmType)
    {
        handleError(message, throwable);

        if (alarmHandler == null && !isAlarmHandlerConfigured())
            return;

        alarmHandler.triggerAlarm(alarmType);
    }

    public static String getAlarmPath()
    {
        return alarmHandler.getAlarmLocation();
    }
}
