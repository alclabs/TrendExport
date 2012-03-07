package com.controlj.addon.trendexport.helper;

import com.controlj.green.addonsupport.access.*;
import org.jetbrains.annotations.NotNull;

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

    public static String convertPersistentLookupToReferencePath(String lookup) throws SystemException, ActionExecutionException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess access) throws Exception
            {

            }
        });

        return lookup;
    }
}
