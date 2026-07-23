package study;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.MarkerDescriptor;
import com.motivewave.platform.sdk.draw.Marker;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.*;

@StudyHeader(
        namespace = "com.unfinishedtrade",
        name = "Swings",
        id = "Swings",
        desc = "Plots Marker at Swing Points",
        overlay = true,
        menu = "Youtube",
menu2 = "Custom Studies"
)

public class Swings extends Study {
    @Override
    public void initialize(Defaults defaults){
        var sd = createSD();
        var tab = sd.addTab("General");
        var group = tab.addGroup("Swing Settings");
        group.addRow(new MarkerDescriptor("Swing Low", "Swing Low", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, new Color(0,0,255), new Color(255,255,255), true, true));
        group.addRow(new MarkerDescriptor("Swing High", "Swing High", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, new Color(255,0,0), new Color(255,255,255), true, true));

        var swingStrength = tab.addGroup("Swing Strength");
        swingStrength.addRow(new IntegerDescriptor("Swing Strength", "Strength", 1, 1, 10, 1));

        sd.addQuickSettings("Swing Low");
        sd.addQuickSettings("Swing High");
        sd.addQuickSettings("Swing Strength");
    }

    @Override
    protected void calculateValues(DataContext ctx){
        var swingLow = getSettings().getMarker("Swing Low");
        var swingHigh = getSettings().getMarker("Swing High");
        var strength = getSettings().getInteger("Swing Strength");

        var series = ctx.getDataSeries();
        var swingPoints = series.calcSwingPoints(strength);

        for(var sp : swingPoints){
            if(sp.isTop()){
                addFigure(new Marker(sp.getCoordinate(), Enums.Position.TOP, swingHigh));
            }
            else{
                addFigure(new Marker(sp.getCoordinate(), Enums.Position.BOTTOM, swingLow));
            }
        }
    }
}
