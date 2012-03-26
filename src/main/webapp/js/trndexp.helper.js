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

function collectData(lookups)
{
    var objToSend = {
        "action": "collectData",
        "nodeLookupString": lookups
    };

    makeRequestToCollector(objToSend);
}

function createAddSourceRequest(nodeKey)
{
    var tableName = $("#source_tableName_input").val();
    makeRequestToCollector({"action": "addSource", "nodeLookupString": nodeKey , "tableName": tableName});
}

function createEnableCollectionRequest(nodeKey)
{
    var tableName = $("#source_tableName_input").val();
    makeRequestToCollector({"action": "enableSource", "nodeLookupString": nodeKey , "tableName": tableName, "keepData" : false});
}

function askToKeepData(keys)
{
    $("#removeDialog").show();
    $("#removeDialog").dialog({
                draggable: true,
                position: "center",
                height: 250,
                width: 400,
                modal: true,
                open: function(event, ui)
                {
                    $('body').css('overflow', 'hidden');
                    $('.ui-widget-overlay').css('width', '100%');
                },
                close: function(event, ui)
                {
                    $('body').css('overflow', 'auto');
                },
                buttons: {
                    "Yes": function()
                    {
                        createRemoveSourceRequest(keys);
                        $(this).dialog("close");
                    },
                    "No": function()
                    {
                        $(this).dialog("close");
                    }
                }
            });
}

// key      - key of tree (persistent lookup from dynatree for node)
// keepData - boolean asking whether to keep the current data or clear it
function createDisableCollectionRequest(nodeKey)
{
    var tableName = $("#source_tableName_input").val();
    makeRequestToCollector({"action": "disableSource", "nodeLookupString": nodeKey , "tableName": tableName, "keepData" : false});
}

function createRemoveSourceRequest(nodeKey)
{
    makeRequestToCollector({"action": "removeSource", "nodeLookupString": nodeKey, "keepData" : false});
}

function getCollectorStatus()
{
    // make request for status every 15s here...
    $.getJSON("servlets/addOrRemoveSource", {"action": "getCollectorStatus", "nodeLookupString": ""},
            function(data)
            {
                var statusOfResults = data["result"];
                $('#maintainStatusText').text(statusOfResults);
            });
}

function makeRequestToCollector(objectToSend)
{
    $('#workingText').show();
    getCollectorStatus();

    $.getJSON("servlets/addOrRemoveSource", objectToSend,
            function(data)
            {
                // if an error exists, handle here
                if (data["result"] === "Table Name is not valid")
                    alert("Table Name is not valid. Please refer to the help section about valid table names.");
                else
                {
                    reloadTable(); // method defined in maintain.ui.js

                    // split the nodes back and get each lookup for the tree to update...need to improve the servlets
                    // lesson learned: servlets need to be designed better...
                    var tree = $("#treeOfPotentialSources").dynatree("getTree");
                    var keys = data["lookups"].split(";;");

                    for (var i = 0; i < keys.length; i++)
                    {
                        var node = tree.getNodeByKey(keys[i]);
                        if (node === null)
                            continue;

                        if (objectToSend['tableName'] != null)
                            node.data.url = objectToSend['tableName']; // need to update the new name

                        updateUI(node, objectToSend["action"]);
                    }
                }

                $('#workingText').hide();
            }).error(function(e, jqxhr, settings, exception)
            {
                alert('Error: ' + settings);
            });
}