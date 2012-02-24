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
    $("#tabs").tabs();
    $('#source_tableName_input').val("(Select a node)");
    $('#removeDialog').hide();
    $('#workingText').hide();

    $('#addSource').button().bind("click", function()
    {
        createEnableCollectionRequest(getActiveNodeKey());
    });
    $('#addSource').button("disable");


    $('#addRemove_removeSource').button().bind("click", function()
    {
        askToKeepData(getActiveNodeKey());
    });
    $('#addRemove_removeSource').button("disable");


    $("#treeOfPotentialSources").dynatree({
                title: "System",
                selectMode: 1,
                autoCollapse: true,
                clickFolderMode: 3,
                cache: false,

                initAjax: { url: "servlets/treedata" },
                onLazyRead: function(dtnode)
                {
                    dtnode.appendAjax({
                                url:"servlets/treedata",
                                data: {
                                    key: dtnode.data.key,
                                    "mode": "lazyTree"
                                },
                                success: function(dtnode)
                                {
                                    if (dtnode.hasChildren === true)
                                        dtnode.expand();

                                    updateUI(dtnode, 'lazy');
                                }
                            });
                },
                onActivate: function(dtnode)
                {
                    if (dtnode.hasChildren === true)
                        dtnode.expand();

                    dtnode.appendAjax(
                            {
                                url:"servlets/treedata",
                                data: {
                                    key: dtnode.data.key,
                                    "mode": "data"
                                },
                                success: function(dtnode)
                                {
                                    updateUI(dtnode, 'activate');
                                }
                            });
                }
            });
});

function getActiveNodeKey()
{
    var node = $('#treeOfPotentialSources').dynatree('getActiveNode');
    if (node)
        return node.data.key;
    else
        return null;
}

function updateUI(dtnode, typeOfRequest)
{
    if (typeOfRequest === 'removeSource' || typeOfRequest === 'addSource')
    {
        dtnode.data.addClass = typeOfRequest !== 'removeSource' ? "selectedNode" : "unselectedNode";
        dtnode.render();
    }

    // May be way to simplify logic here: too many enable/disables being repeated
    if (dtnode.data.isFolder === true)
    {
        $('#source_tableName_input').val("N/A");
        $('#source_tableName_input').prop('disabled', dtnode.data.isFolder === true);

        $('#addSource').button("disable");
        $('#addRemove_removeSource').button("disable");
    }
    else
    {
        $('#source_tableName_input').val(dtnode.data.url);
        $('#source_tableName_input').prop('disabled', dtnode.data.addClass === "selectedNode");

        if (dtnode.data.addClass === "selectedNode")
        {
            $('#addSource').button("disable");
            $('#addRemove_removeSource').button("enable");
        }
        else
        {
            $('#addSource').button("enable");
            $('#addRemove_removeSource').button("disable");
        }
    }
}


