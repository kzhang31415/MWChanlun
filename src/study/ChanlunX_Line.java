package study;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

@StudyHeader(
        namespace = "com.unfinished",
        id = "ChanlunXLine",
        name = "ChanlunX Line",
        desc = "Line segments formed by connecting highs and lows",
        menu = "ChanlunX",
        menu2 = "Custom Studies",
        overlay = true,
        supportsBarUpdates = true,
        requiresBarUpdates = true
)
public class ChanlunX_Line extends Study
{
    private static final String LINE_PATH = "linePath";

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tab = sd.addTab("General");

        var lineGroup = tab.addGroup("Line Settings");

        lineGroup.addRow(
                new PathDescriptor(
                        LINE_PATH,
                        "Chanlun Line",
                        defaults.getLineColor(),
                        2.0f,
                        null,
                        true,
                        false,
                        true
                )
        );

        sd.addQuickSettings(LINE_PATH);

        setSettingsDescriptor(sd);
    }

    //Currently trivial logic, just draws a line between each consecutive bar. Replace later with something using the Chanlun functions.
    @Override
    protected void calculate(int index, DataContext ctx)
    {
        if (index < 1) {
            return;
        }

        var series = ctx.getDataSeries();
        var path = getSettings().getPath(LINE_PATH);

        long previousTime = series.getStartTime(index - 1);
        long currentTime = series.getStartTime(index);

        double previousPrice = series.getClose(index - 1);
        double currentPrice = series.getClose(index);

        var start = new Coordinate(previousTime, previousPrice);
        var end = new Coordinate(currentTime, currentPrice);

        var line = new Line(start, end, path);

        addFigure(line);
    }
}