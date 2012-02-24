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

import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.PresentValue;
import com.controlj.green.addonsupport.access.value.BoolValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AlarmHandler
{
    private String alarmLocation;

    public enum TrendExportAlarm {TrendSourceUnreachable, HistorianDisabled, CollectionFailure, Test, CollectionDatabaseCommError}
    private Map<TrendExportAlarm, String> alarmMap;

    protected AlarmHandler(String location)
    {
        this.alarmLocation = location;

        alarmMap = new HashMap<TrendExportAlarm, String>();
        alarmMap.put(TrendExportAlarm.TrendSourceUnreachable, "m001");
        alarmMap.put(TrendExportAlarm.HistorianDisabled, "m003");
        alarmMap.put(TrendExportAlarm.CollectionFailure, "m007");
        alarmMap.put(TrendExportAlarm.CollectionDatabaseCommError, "m007");
        alarmMap.put(TrendExportAlarm.Test, "m007");
    }

    protected void triggerAlarm(final TrendExportAlarm alarmLabel)
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        try
        {
            connection.runWriteAction(FieldAccessFactory.newFieldAccess(), "Triggering alarm!", new WriteAction()
            {
                @Override
                public void execute(@NotNull WritableSystemAccess access) throws Exception
                {
                    String refName = alarmMap.get(alarmLabel);
                    Location location = access.resolveGQLPath(alarmLocation);
                    PresentValue alarmTrigger = location.getChild(refName).getAspect(PresentValue.class);
                    BoolValue value = (BoolValue)alarmTrigger.getValue();

                    if (value != null && value.isWritable())
                        value.set(true);
                }
            });
        }
        catch (WriteAbortedException e)
        {
            ErrorHandler.handleError("AlarmHandler write aborted!", e);
        }
        catch (SystemException e)
        {
            ErrorHandler.handleError("AlarmHandler SystemException!", e);
        }
        catch (ActionExecutionException e)
        {
            ErrorHandler.handleError("AlarmHandler ActionExecution Exception!", e);
        }
    }
}
