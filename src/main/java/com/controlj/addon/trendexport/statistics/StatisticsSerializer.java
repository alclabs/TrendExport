package com.controlj.addon.trendexport.statistics;

import java.util.Date;
import java.util.List;

public class StatisticsSerializer
{
    public StatisticsSerializer() {}

    public SourceStatsHolder deserialize(String line)
    {
        return parseFlatStatistics(line.split(";"));
    }

    private SourceStatsHolder parseFlatStatistics(String[] strings)
    {
        SourceStatsHolder holder = new SourceStatsHolder();
        int lengthForEach = (int) Long.parseLong(strings[1]);

        for (int i = 2; i < lengthForEach; i++)
        {
            Date date = new Date(Long.parseLong(strings[i]));
            long duration = Long.parseLong(strings[1 + i + lengthForEach]);
            long samples = Long.parseLong(strings[2 + i + lengthForEach]);

            holder.addStatistics(new Statistics(date, duration, samples));
        }

        return holder;
    }


    public String serialize(Statistics holder)
    {
        return holder.getDate().getTime() + ";" + holder.getElapsedTime() + ";" + holder.getSamples();
    }

    public String serialize(List<Statistics> holderList)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(holderList.size()).append(";");

        for (Statistics s : holderList)
            builder.append(serialize(s)).append(";");

        return builder.toString();
    }
}
