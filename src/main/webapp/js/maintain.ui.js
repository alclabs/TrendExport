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
    var tableOfAllSources;

    setInterval("getCollectorStatus()", 5000);


    var collectDataNowBtn = $('#collectDataNow').button().bind("click", function()
    {
        var selected = getSelectedRows(tableOfAllSources);
        collectData(combineKeys(selected));
    });
    collectDataNowBtn.button('disable');


    var removeSourceButton = $('#maintain_RemoveSource').button().bind("click", function()
    {
        var selected = getSelectedRows(tableOfAllSources);
        askToKeepData(combineKeys(selected));

    });
    removeSourceButton.button('disable');


    var enableOrDisable_btn = $('#enableOrDisable_btn').button().bind("click", function()
    {
        var selectedRows = getSelectedRows(tableOfAllSources);

        if (determineIfRowsEnabled(selectedRows) === true) // all selected rows are enabled
            createDisableCollectionRequest(combineKeys(selectedRows));
        else
            createEnableCollectionRequest(combineKeys(selectedRows));
    });
    enableOrDisable_btn.button('disable');


    $('#collectorStatusLabel').text("Current Status: ");

    tableOfAllSources = $('#maintenanceTable').dataTable({
                bPaginate: true,
                bAutoWidth: false,
                "bDeferRender": true,
                "sAjaxSource" : 'servlets/currentTrends',
                "sPaginationType": "full_numbers",
                "sRowSelect": "multiple",
                "fnServerData": function(sSource, aoData, fnCallback)
                {
                    $.getJSON(sSource, aoData, function (json)
                    {
                        removeSelectedClassFromRows(tableOfAllSources);
                        updateButtonsBasedOnRowsSelected(getSelectedRows(tableOfAllSources));
                        fnCallback(json)
                    });

                },
                "aoColumns" : [
                    {"sTitle": "Collection", "sWidth": "2%", "mDataProp": "isEnabled"},
                    {"sTitle": "Source Path",   "sWidth": "50%", "mDataProp": "displayPath"},
                    {"sTitle": "Source Name",   "sWidth": "25%", "mDataProp": "sourceDisplayName"},
                    {"sTitle": "Table Name",    "sWidth": "25%", "mDataProp": "tableName"},
                    {"sTitle": "Lookup String", "sWidth": "5%",  "mDataProp": "sourceReferencePath", "bVisible": false}
//                    {"sTitle": "Lookup String", "sWidth": "5%",  "mDataProp": "sourceLookupString", "bVisible": false}

                ],
                "fnCreatedRow": function(nRow)
                {
                    $(nRow).live('click', rowClickEvent());
                    return nRow;
                },
                "fnInitComplete": function()
                {
                    $('#maintenanceTable tbody tr').live('click', function()
                    {
                        // fix for detecting dummy table row in the event that the table is empty
                        var htmlString = $(this).html();
                        if (htmlString.indexOf("Add a source from") !== -1)
                        {
                            $("#tabs").tabs('select', 1);
                            return;
                        }

                        if ($(this).hasClass('row_selected'))
                            $(this).removeClass('row_selected');
                        else
                            $(this).addClass('row_selected');

                        updateButtonsBasedOnRowsSelected(getSelectedRows(tableOfAllSources));
                    });
                }
            });


    function rowClickEvent()
    {
        if ($(this).hasClass('row_selected'))
            $(this).removeClass('row_selected');
        else
            $(this).addClass('row_selected');

        updateButtonsBasedOnRowsSelected(getSelectedRows(tableOfAllSources));
    }
});

function updateButtonsBasedOnRowsSelected(rowsOfData)
{
    var enableOrDisable_btn = $('#enableOrDisable_btn');

    if (rowsOfData.length === 0)
    {
        $('#collectDataNow').button('disable');
        $('#maintain_RemoveSource').button('disable');
        enableOrDisable_btn.button('disable');
    }
    else
    {
        $('#collectDataNow').button('enable');
        $('#maintain_RemoveSource').button('enable');
        enableOrDisable_btn.button('enable');
    }

    if (determineIfRowsEnabled(rowsOfData) === true) // all selected rows are enabled
        enableOrDisable_btn.button("option", "label", "Disable Collection");
    else
        enableOrDisable_btn.button("option", "label", "Enable Collection");
}

function combineKeys(keys)
{
    var keyString = "";
    for (var index = 0; index < keys.length; index++)
        keyString += keys[index]["sourceReferencePath"] + ";;";

    return keyString;
}

function reloadTable()
{
    $('#maintenanceTable').dataTable().fnReloadAjax();
}

function getNumberOfSelectedRows(oTableLocal)
{
//    "use strict";
    var aTrs = oTableLocal.fnGetNodes();
    var count = 0;
    for (var i = 0; i < aTrs.length; i++)
    {
        if ($(aTrs[i]).hasClass('row_selected'))
            count++;
    }

    return count;
}

function removeSelectedClassFromRows(oTableLocal)
{
    var aTrs = oTableLocal.fnGetNodes();

    for (var i = 0; i < aTrs.length; i++)
    {
        if ($(aTrs[i]).hasClass('row_selected'))
            $(aTrs[i]).removeClass('row_selected');
    }

    return aTrs;
}

function getSelectedRows(oTableLocal)
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

function determineIfRowsEnabled(keys)
{
    var enabledRows = 0;
    var disabledRows = 0;

    for (var i = 0; i < keys.length; i++)
    {
        var singleRow = keys[i];
        if (singleRow["isEnabled"] === "Enabled")
            enabledRows++;
        else
            disabledRows++;
    }

    // if all are enabled, return true
    // else (some are disabled, some may be enabled) -> return false
    return enabledRows !== 0 && disabledRows === 0;
}