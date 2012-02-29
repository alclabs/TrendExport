package com.controlj.addon.trendexport.helper;

import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import org.jetbrains.annotations.NotNull;

public class TrendPathAndDBTableName
{
    private String trendSourceDisplayName;
    private String trendSourceLookupString;
    private String dbTableName;
    private TrendSource.Type trendType;
    private boolean isEnabled;

    public TrendPathAndDBTableName(String sourcePath, String displayName, String tableName, TrendSource.Type type, boolean enabled)
    {
        this.trendSourceLookupString = sourcePath;
        this.trendSourceDisplayName = displayName;
        this.dbTableName = tableName;
        this.trendType = type;
        this.isEnabled = enabled;
    }

    public String getTrendSourceLookupString()
    {
        return trendSourceLookupString;
    }

    public String getDbTableName()
    {
        return dbTableName;
    }

    public short getTrendType()
    {
        if (trendType == TrendSource.Type.Analog)
            return 0;
        else if (trendType == TrendSource.Type.Digital)
            return 1;
        else if (trendType == TrendSource.Type.EquipmentColor)
            return 2;
        else
            return 3; // 'Complex' should never show up
    }

    public String getDisplayPath() throws SystemException, ActionExecutionException
    {
        final String lookup = this.trendSourceLookupString;
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        return connection.runReadAction(new ReadActionResult<String>()
        {
            @Override
            public String execute(@NotNull SystemAccess access) throws Exception
            {
                return access.getTree(SystemTree.Geographic).resolve(lookup).getDisplayPath();
            }
        });
    }

    public String getDisplayName()
    {
        return trendSourceDisplayName;
    }

    public boolean getIsEnabled()
    {
        return isEnabled;
    }

    public void setIsEnabled(boolean enabled)
    {
        this.isEnabled = enabled;
    }
}
