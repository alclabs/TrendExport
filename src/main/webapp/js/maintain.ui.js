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
    var maintenanceTable;

    var collectDataNowBtn = $('#collectDataNow').button().bind("click", function()
    {
        var selected = getRowData(maintenanceTable);
        collectData(combineKeys(selected));
    });
    collectDataNowBtn.button('disable');

    var removeSourceButton = $('#maintain_RemoveSource').button().bind("click", function()
    {
        var selected = getRowData(maintenanceTable);
        askToKeepData(combineKeys(selected));

    });
    removeSourceButton.button('disable');


    $('#collectorStatusLabel').text("Current Status: ");

    maintenanceTable = $('#maintenanceTable').dataTable({
                bPaginate: true,
                bAutoWidth: false,
                "bDeferRender": true,
                "sAjaxSource" : 'servlets/currentTrends',
                "sPaginationType": "full_numbers",
                "sRowSelect": "multiple",
                "aoColumns" : [
                    {"sTitle": "Source Path",   "sWidth": "50%", "mDataProp": "path"},
                    {"sTitle": "Source Name",   "sWidth": "25%", "mDataProp": "sourceDisplayName"},
                    {"sTitle": "Table Name",    "sWidth": "25%", "mDataProp": "tableName"},
//                        {"sTitle": "Table Entries", "sWidth": "5%", "mDataProp": "tableEntries"},
                    {"sTitle": "Enabled", "sWidth": "5%", "mDataProp": "isEnabled"},
                    {"sTitle": "Lookup String", "sWidth": "5%",  "mDataProp": "sourceLookupString", "bVisible": false}
                ],
                "fnCreatedRow": function(nRow, aData, iDisplayIndex)
                {
                    $(nRow).live('click', rowClickEvent());
                    return nRow;
                },
                "fnInitComplete": function(oSettings)
                {
                    $('#maintenanceTable tbody tr').live('click', function()
                    {
                        // fix for detecting dummy table row in the event that the table is empty
                        var htmlString = $(this).html();
                        if (htmlString.indexOf("Add a source from") !== -1)
                        {
                            $("#tabs").tabs('select', 1);
//                            alert("Please add at least one trend source from the Add or Remove Tab.");
                            return;
                        }

                        if ($(this).hasClass('row_selected'))
                        {
                            $(this).removeClass('row_selected');
                            checkTable(maintenanceTable);
                        }
                        else
                        {
                            $(this).addClass('row_selected');
                            collectDataNowBtn.button('enable');
                            removeSourceButton.button('enable');
                        }
                    })
                }
            });

    function rowClickEvent()
    {
        if ($(this).hasClass('row_selected'))
        {
            $(this).removeClass('row_selected');
            checkTable(maintenanceTable);
        }
        else
        {
            $(this).addClass('row_selected');
            collectDataNowBtn.button('enable');
            removeSourceButton.button('enable');
        }
    }
});

function checkTable(oTableLocal)
{
    if (getNumberOfSelectedRows(oTableLocal) === 0)
    {
        $('#collectDataNow').button('disable');
        $('#maintain_RemoveSource').button('disable');
    }
}

function combineKeys(keys)
{
    var keyString = "";
    for (var index = 0; index < keys.length; index++)
        keyString += keys[index]["sourceLookupString"] + ";;";

    return keyString;
}

function reloadTable()
{
    $('#maintenanceTable').dataTable().fnReloadAjax();
}

function getNumberOfSelectedRows(oTableLocal)
{
    var aTrs = oTableLocal.fnGetNodes();
    var count = 0;
    for (var i = 0; i < aTrs.length; i++)
    {
        if ($(aTrs[i]).hasClass('row_selected'))
            count++;
    }

    return count;
}

function getRowData(oTableLocal)
{
    var aReturn = new Array();
    var aTrs = oTableLocal.fnGetNodes();

    for (var i = 0; i < aTrs.length; i++)
    {
        if ($(aTrs[i]).hasClass('row_selected'))
            aReturn.push(oTableLocal.fnGetData(i));
    }
    return aReturn;
}