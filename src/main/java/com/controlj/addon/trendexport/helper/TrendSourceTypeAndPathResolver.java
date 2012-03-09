package com.controlj.addon.trendexport.helper;

import com.controlj.green.addonsupport.access.Location;
import com.controlj.green.addonsupport.access.UnresolvableException;
import com.controlj.green.addonsupport.access.aspect.TrendSource;

public class TrendSourceTypeAndPathResolver
{
    public static StringBuilder getReferencePath(Location location, StringBuilder builder) throws UnresolvableException
    {
        if (location.hasParent())
        {
            getReferencePath(location.getParent(), builder);
            builder.append(location.getReferenceName());
            if (!location.getChildren().isEmpty())
                builder.append('/');
        }

        return builder;
    }

    public static StringBuilder getFullDisplayPath(Location location, StringBuilder builder) throws UnresolvableException
    {
        if (location.hasParent())
            getFullDisplayPath(location.getParent(), builder);

        builder.append(" \\ ").append(location.getDisplayName());
        return builder;
    }

    public static TrendSource.Type getTrendSourceType(short type)
    {
        switch (type)
        {
            case 1:
                return TrendSource.Type.Analog;
            case 2:
                return TrendSource.Type.Digital;
            case 3:
                return TrendSource.Type.EquipmentColor;
        }

        return TrendSource.Type.Complex;
    }

    public static short getTrendSourceType(TrendSource.Type type)
    {
        if (type == TrendSource.Type.Analog)
            return 1;
        else if (type == TrendSource.Type.Digital)
            return 2;
//        else if (type == TrendSource.Type.EquipmentColor)
//            return 3;
        else
            return 4;
    }
}
