package com.controlj.addon.trendexport.helper;

import com.controlj.green.addonsupport.access.Location;
import com.controlj.green.addonsupport.access.UnresolvableException;

public class TrendSourcePathResolvers
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
}
