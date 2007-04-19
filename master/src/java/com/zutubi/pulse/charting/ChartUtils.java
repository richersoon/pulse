package com.zutubi.pulse.charting;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.servlet.ServletUtilities;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import com.zutubi.util.RandomUtils;

/**
 * <class comment/>
 */
public class ChartUtils
{
    public static Map renderForWeb(JFreeChart chart, int width, int height) throws IOException
    {
        Map<String, Object> params = new HashMap<String, Object>();
        ChartRenderingInfo chartRenderingInfo = new ChartRenderingInfo();

        String location = ServletUtilities.saveChartAsPNG(chart, width, height, chartRenderingInfo, null);
        params.put("location", location);
        params.put("width", width);
        params.put("height", height);

        String mapName = "imageMap-" + RandomUtils.randomString(3);
        params.put("imageMap", ChartUtilities.getImageMap(mapName, chartRenderingInfo));
        params.put("imageMapName", mapName);
        return params;
    }
}
