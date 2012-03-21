<%@ page import="com.controlj.addon.trendexport.config.ConfigManagerLoader" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>
<!--
~ Copyright (c) 2011 Automated Logic Corporation
~
~ Permission is hereby granted, free of charge, to any person obtaining a copy
~ of this software and associated documentation files (the "Software"), to deal
~ in the Software without restriction, including without limitation the rights
~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
~ copies of the Software, and to permit persons to whom the Software is
~ furnished to do so, subject to the following conditions:
~
~ The above copyright notice and this permission notice shall be included in
~ all copies or substantial portions of the Software.
~
~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
~ THE SOFTWARE.
-->
<head>
    <title>Trend Export</title>

    <link rel="stylesheet" type="text/css" href="css/smoothness/jquery-ui-1.8.9.custom.css"/>
    <link rel='stylesheet' type='text/css' href='skin/ui.dynatree.css'/>
    <link rel='stylesheet' type='text/css' href='skin/jquery.svg.css'/>
    <link rel="stylesheet" type="text/css" href="skin/ui.theme.css"/>
    <link rel="stylesheet" type="text/css" href="skin/ui.maintainPage.css"/>
    <link rel="stylesheet" type='text/css' href='skin/ui.helpPage.css'/>

    <script type="text/javascript" src="js/jquery-1.6.4.min.js"></script>
    <script type="text/javascript" src="js/jquery-ui-1.8.18.custom.min.js"></script>

    <script type="text/javascript" src="js/jquery.dynatree.min.js"></script>
    <script type="text/javascript" src="js/jquery.hyjack.select.min.js"></script>
    <script type="text/javascript" src="js/jquery.dataTables.js"></script>
    <script type="text/javascript" src="js/jquery.dataTables.fnReloadAjax.js"></script>

    <script type="text/javascript" src="js/addRemove.ui.js"></script>
    <script type="text/javascript" src="js/settings.ui.js"></script>
    <script type="text/javascript" src="js/maintain.ui.js"></script>
    <script type="text/javascript" src="js/trndexp.helper.js"></script>
</head>

<body>

<div id="tabs" class="customTabs">
    <!--define tabs-->
    <ul>
        <li><a href="#maintain">Maintain</a></li>
        <li><a href="#addOrRemove">Add Or Remove</a></li>
        <li><a href="#settings">Settings</a></li>
        <li><a href="help.html">Help</a></li>
    </ul>

    <%--Define dialog content--%>
    <div id="removeDialog" title="Remove Source?">
        <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>
            This will remove all the data associated this source from the TrendExport database.
            If you want to keep your data but prevent additional data from being collected about this source,
            please disable this source instead.<br/><br/>
            Are you sure you want to remove the source?
        </p>
    </div>


    <!--Add/Remove Tab-->
    <div id="addOrRemove">
        <div class="potentialtrends">
            <div id="treeOfPotentialSources" style="display:table;"></div>
        </div>

        <div class="addremovebuttons">
            <div class="content_veryPaleBlue">
                <table>
                    <tbody>
                    <tr>
                        <td>Table Name:&nbsp;&nbsp;</td>
                        <td><input class="mainPageStretchedTDs" type="text" id="source_tableName_input" value="..."/>
                        </td>
                        <td><input type="checkbox" id="isTrendEnabled" value="yes"/>Collection enabled?</td>
                    </tr>
                    <tr>
                        <td><span id="workingText">Working....</span></td>
                    </tr>
                    </tbody>
                </table>
                <br/> <br/>
                <button id="addSource">Add Source</button>
                <button id="addRemove_removeSource">Remove Source...</button>
            </div>
        </div>
    </div>

    <!--Maintain Tab (place to display everything)-->
    <div id="maintain">
        <div class="groupTitle">
            <button id="collectDataNow">Collect Data Now</button>
            <button id="enableOrDisable_btn">Enable Collection</button>
            <button id="maintain_RemoveSource">Remove Source...</button>

            <span id="maintainStatusText" style="float: right;padding-right: 5%;padding-top: 5px;">Status: Idle</span>
        </div>


        <br/><br/>
        <table id="maintenanceTable" class="pretty"></table>
    </div>

    <!--Configure Settings-->
    <div id="settings">
        <div class="groupTitle">
            <button id="Save" class="smallButton">Save</button>
        </div>
        <br/>

        <div class="apiVersionWarningBanner" id="warningText">
            Warning: Add-on API version 1.1.3 is required for indicies to be correctly created in the target database.
            <br/>
        </div>

        <!--Connection group-->
        <div class="groupTitle">Database Connection Settings:</div>
        <div class="groupContent">
            <table>
                <tr>
                    <td> Database Brand:</td>
                    <td>
                        <select id="dbTypeCombo" class="combo">
                            <option value="derby">Apache Derby (default)</option>
                            <option value="mysql">MySQL (5.0 or later)</option>
                            <option value="postgresql">PostgreSQL (7.4 or later)</option>
                            <option value="oracle">Oracle (10g or later)</option>
                            <option value="sqlserver">SQL Server (2000 or later)</option>
                        </select>
                    </td>
                    <td>Instance:</td>
                    <td><input type="text" id="instance" value="default"/></td>
                </tr>
                <tr>
                    <td>Host:</td>
                    <td><input type="text" id="host" value="localhost"/></td>
                    <td>Port:</td>
                    <td><input type="text" id="port" value="0"/></td>
                </tr>
                <tr>
                    <td>Username:</td>
                    <td><input type="text" id="user" value=""/></td>
                    <td>Password:</td>
                    <td><input type="password" id="pass" value=""/></td>
                </tr>

            </table>
            <br/>
            <button id="testConnection" class="largeButton">Test Connection</button>
        </div>
        <br/> <br/>

        <!--Collection Settings Group-->
        <div class="groupTitle">
            Automatic Collection Settings:
        </div>

        <div class="groupContent">
            <table>
                <tr>
                    <td>
                        <input type="radio" name="collectionSettings" id="collInt" value="interval"/>
                        Collection Interval (hours):
                    </td>
                    <td><input type="text" id="intervalValue" value=""/></td>
                </tr>
                <tr>
                    <td>
                        <input type="radio" name="collectionSettings" id="collTime" value="time"/>
                        Collection Time:
                    </td>

                    <td><input class="smallInput" type="text" id="collTime_Hours" value="12"/> :
                        <input class="smallInput" type="text" id="collTime_Minutes" value="00"/>
                        <input type="radio" name="amorpm" id="am" value="am" checked="checked">AM
                        <input type="radio" name="amorpm" id="pm" value="pm"/>PM
                    </td>
                </tr>
            </table>
        </div>
        <br/><br/>

        <div class="groupTitle">
            Alarm Settings:
        </div>

        <div class="groupContent">
            Path to TrendExport Alarm Program:<br/>
            <input type="text" id="alarmPath" value=""/>
            <button id="testAlarm" class="largeButton">Test Alarm</button>
        </div>
    </div>
</div>

<script type="text/javascript">
    var isConfigured = <%=ConfigManagerLoader.isConfigured()%>;
    var $tabs = $("#tabs").tabs();
    $tabs.tabs('select', isConfigured === true ? 0 : 2);
</script>

</body>
</html>