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
    $('#source_tableName_input').prop('disabled', true);
    $('#removeDialog').hide();
    $('#workingText').hide();
    $('#addSource').button("disable");
    $('#addSource').button().bind("click", function()
    {
        createAddSourceRequest(getActiveNodeKey());
    });

    $('#addRemove_removeSource').button("disable");
    $('#addRemove_removeSource').button().bind("click", function()
    {
        askToKeepData(getActiveNodeKey());
    });

    var sourceEnabledCheckbox = $('#isTrendEnabled').removeProp('checked');
    sourceEnabledCheckbox.prop('disabled', true);
    sourceEnabledCheckbox.click(function()
    {
        if (sourceEnabledCheckbox.is(':checked'))
            createEnableCollectionRequest(getActiveNodeKey());
        else
            createDisableCollectionRequest(getActiveNodeKey());
    });

    initializeTree();

});

function initializeTree()
{
    $("#treeOfPotentialSources").dynatree({
                title: "System",
                selectMode: 1,
                autoCollapse: true,
                clickFolderMode: 3,
                cache: false,

                initAjax: { url: "servlets/treedata" },
                onPostInit: function(isReloading, isError , XMLHttpRequest, textStatus, errorThrown)
                {
                    if (isError)
                        alert(errorThrown);
                },
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

                                    if (dtnode.data.title != null)
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
                                    if (dtnode.data.title != null)
                                        updateUI(dtnode, 'activate');
                                }
                            });
                }
            });
}

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
    $('#source_tableName_input').prop('disabled', dtnode.data.isSource === false);

    if (dtnode.data.isSource === false)
    {
        $('#source_tableName_input').val("N/A");

        $('#addSource').button("disable");
        $('#addRemove_removeSource').button("disable");

        $('input:checkbox').prop('disabled', true);
    }
    else
    {
        if (typeOfRequest === 'removeSource' || typeOfRequest === 'addSource')
        {
            dtnode.data.addClass = typeOfRequest !== 'removeSource' ? "selectedNode" : "unselectedNode";
            dtnode.render();
        }
        else if (typeOfRequest === 'enableSource' || typeOfRequest === 'disableSource')
        {
            dtnode.data.addClass = typeOfRequest === 'enableSource' ? "selectedNode" : "disabledNode";
            dtnode.render();
        }

        var isSelectedNodeOrDisabled = dtnode.data.addClass === "selectedNode" || dtnode.data.addClass === 'disabledNode';

        $('#source_tableName_input').val(dtnode.data.url);
        $('#source_tableName_input').prop('disabled', isSelectedNodeOrDisabled);
        $('input:checkbox').prop('disabled', isSelectedNodeOrDisabled === false);

        if (isSelectedNodeOrDisabled)
        {
            $('#addSource').button("disable");
            $('#addRemove_removeSource').button("enable");
            $('input:checkbox').prop('checked', dtnode.data.addClass !== 'disabledNode');
        }
        else
        {
            $('#addSource').button("enable");
            $('#addRemove_removeSource').button("disable");
            $('input:checkbox').removeProp('checked');
        }
    }
}


