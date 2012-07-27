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
//        int lengthForEach = (int) Long.parseLong(strings[0]);

        for (int i = 0; i <= strings.length-1; i+=3)
        {
            Date date = new Date(Long.parseLong(strings[i]));
            long duration = Long.parseLong(strings[i+1]);
            long samples = Long.parseLong(strings[i+2]);

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
        for (Statistics s : holderList)
            builder.append(serialize(s)).append(";");

        return builder.toString();
    }
}
