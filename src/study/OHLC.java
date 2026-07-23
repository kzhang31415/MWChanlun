package study;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.Color;

@StudyHeader(
        namespace = "com.unfinished",
        id = "OHLC Study",
        name = "OHLC Study",
        desc = "Gets OHLC data",
        menu = "Youtube",
        menu2 = "Custom Studies",
        overlay = true
)

public class OHLC extends Study {
    @Override
    public void initialize(Defaults defaults){
        var sd = createSD();
        var tab = sd.addTab("General");

        var group = tab.addGroup("Colors");
        group.addRow(new ColorDescriptor("Color", "Color", new Color(255, 255, 255)));

        sd.addQuickSettings("Color");
    }

    @Override
    public void calculate(int index, DataContext ctx){
        var color = getSettings().getColor("Color");
        var series = ctx.getDataSeries();

        //get OHLC
        var open = series.getOpen(index);
        var high = series.getHigh(index);
        var low = series.getLow(index);
        var close = series.getClose(index);

        //Log message
        //info("Index: " + String.valueOf(index) + " Close: " + String.valueOf(close));

        if(close > open){
            series.setPriceBarColor(index, color);
        }
    }
}