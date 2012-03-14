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

$(function()
{
    $("#dbtypeCombo").hyjack_select();
    $("#alarmPath").removeProp("disabled");
    $('#testAlarm').button().bind("click", function()
    {
        var obj = getSettingsObj("testAlarm");
        $.getJSON("servlets/settings", obj,
                function(data)
                {
                    alert(data["result"]);
                });
    });
    $('select').change(function()
    {
        updateComboSettings($(this).val());
    });

    $.getJSON("servlets/settings",
            function(data)
            {
                $('#dbTypeCombo').val(data['dbType']);
                $('#host').val(data['host']);
                $('#port').val(data['port']);
                $('#instance').val(data['instance']);
                $('#user').val(data['user']);
                $('#pass').val(data['pass']);

                updateComboSettings(data['dbType']);

                // set collection settings
                if (data['collectionType'] === 'Interval')
                {
                    $('#collInt').prop('checked', true);
                    enableTimeSettings(false);

                    $('#intervalValue').val(data['collectionValue']);
                }
                else
                {
                    $('#collTime').prop('checked', true);
                    enableTimeSettings(true);

                    var rawTime = data['collectionValue'] / 60000;
                    var hours = Math.round(rawTime / 60);
                    var minutes = rawTime % 60;

                    if (hours > 12)
                    {
                        hours /= 12;
                        $('#pm').prop('checked', true);
                    }

                    var minute_str = minutes < 10 ? "0" + minutes : minutes;

                    $('#collTime_Hours').val(hours);
                    $('#collTime_Minutes').val(minute_str);
                    $('#intervalValue').val("");
                }

                // Alarm control program path
                $('#alarmPath').val(data['alarmPath']);
            });

    $('#Save').button().bind("click", function()
    {
        var newInfo = getSettingsObj("save");
        $.getJSON("servlets/settings", newInfo,
                function(data)
                {
                    alert(data["result"]);
                });
    });

    $('#testConnection').button().bind("click", function()
    {
        var obj = getSettingsObj("connect");
        $.getJSON("servlets/settings", obj,
                function(data)
                {
                    alert(data["result"]);
                });
    });

    $("input[type='radio']").change(function()
    {
        enableTimeSettings($("input[name=collectionSettings]:checked").val() === "time");
    });

    function updateComboSettings(thing)
    {
//        alert(thing);
        if (thing === "derby")
        {
            // disable all other connection controls
            $('#host').prop('disabled', true);
            $('#port').prop('disabled', true);
            $('#instance').prop('disabled', true);
            $('#user').prop('disabled', true);
            $('#pass').prop('disabled', true);
        }
        else if (thing === "mysql" || thing === "postgresql" || thing === "oracle" || thing === "sqlserver")
        {
            // enable controls
            $('#host').prop('disabled', false);
            $('#port').prop('disabled', false);
            $('#instance').prop('disabled', false);
            $('#user').prop('disabled', false);
            $('#pass').prop('disabled', false);
        }
    }


});

function getSettingsObj(action)
{
    if ($("input[@name='collectionSettings']:checked").val() === 'interval')
    {
        return {
            "action"     : action,
            "dbType"     : $('#dbTypeCombo').val(),
            "host"       : $('#host').val(),
            "port"       : $('#port').val(),
            "instance"   : $('#instance').val(),
            "user"       : $('#user').val(),
            "pass"       : $('#pass').val(),
            "collMethod" : $("input[@name='collectionSettings']:checked").val(),
            "collValue"  : $('#intervalValue').val(),
            "alarmPath"  : $('#alarmPath').val()
        };
    }
    else
    {
        var amOrPm = $('#am').is(':checked') ? "AM" : "PM";
        var timeString = $('#collTime_Hours').val() + ':' + $('#collTime_Minutes').val() + ':' + amOrPm;

        return {
            "action"     : action,
            "dbType"     : $('#dbTypeCombo').val(),
            "host"       : $('#host').val(),
            "port"       : $('#port').val(),
            "instance"   : $('#instance').val(),
            "user"       : $('#user').val(),
            "pass"       : $('#pass').val(),
            "collMethod" : $("input[@name='collectionSettings']:checked").val(),
            "collValue"  : timeString,
            "alarmPath"  : $('#alarmPath').val()
        };
    }

}

function enableTimeSettings(enableTime)
{
    if (enableTime === true)
    {
        $('#intervalValue').prop("disabled", true);
        $('#collTime_Hours').removeProp("disabled");
        $('#collTime_Minutes').removeProp("disabled");
        $('#am').removeProp("disabled");
        $('#pm').removeProp("disabled");
    }
    else
    {
        $('#intervalValue').removeProp("disabled");
        $('#collTime_Hours').prop("disabled", true);
        $('#collTime_Minutes').prop("disabled", true);
        $('#am').prop("disabled", true);
        $('#pm').prop("disabled", true);
    }
}




