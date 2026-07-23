package study;

import java.awt.Color;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.draw.Box;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

@StudyHeader(
        namespace = "com.unfinished",
        id = "ChanlunXBox",
        name = "ChanlunX Box",
        desc = "Draws rectangular boxes for the current level",
        menu = "ChanlunX",
        menu2 = "Custom Studies",
        overlay = true,
        supportsBarUpdates = true,
        requiresBarUpdates = true
)
public class ChanlunX_Box extends Study
{
    private static final String FILL_BOX = "fillBox";
    private static final String BOX_BORDER = "boxBorder";
    private static final String BOX_FILL_COLOR = "boxFillColor";

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var generalTab = sd.addTab("General");

        var displayGroup = generalTab.addGroup("Display");

        displayGroup.addRow(
                new BooleanDescriptor(
                        FILL_BOX,
                        "Fill Box",
                        true
                )
        );

        var colorsGroup = generalTab.addGroup("Colors");

        colorsGroup.addRow(
                new PathDescriptor(
                        BOX_BORDER,
                        "Box Border",
                        Color.YELLOW,
                        1.5f,
                        null,
                        true,
                        false,
                        true
                )
        );

        colorsGroup.addRow(
                new ColorDescriptor(
                        BOX_FILL_COLOR,
                        "Box Fill Color",
                        new Color(255, 255, 0, 35)
                )
        );

        sd.addQuickSettings(
                FILL_BOX,
                BOX_BORDER,
                BOX_FILL_COLOR
        );

        setSettingsDescriptor(sd);
    }

    @Override
    protected void calculateValues(DataContext ctx)
    {
        // Remove previously drawn boxes before recalculating.
        clearFigures();

        var series = ctx.getDataSeries();
        int size = series.size();

        // We need enough bars to make the test box visible.
        if (size < 40) {
            return;
        }

        /*
         * TEST BOX
         *
         * This currently draws one yellow box over a recent group of bars.
         * Replace these indexes later with the start and end indexes from
         * your actual Chanlun box detection algorithm.
         */
        int startIndex = size - 40;
        int endIndex = size - 10;

        double top = Double.NEGATIVE_INFINITY;
        double bottom = Double.POSITIVE_INFINITY;

        /*
         * Find the highest high and lowest low between the selected bars.
         * This defines the top and bottom of the test rectangle.
         */
        for (int i = startIndex; i <= endIndex; i++) {
            top = Math.max(top, series.getHigh(i));
            bottom = Math.min(bottom, series.getLow(i));
        }

        long startTime = series.getStartTime(startIndex);
        long endTime = series.getStartTime(endIndex);

        Coordinate topLeft = new Coordinate(startTime, top);
        Coordinate bottomRight = new Coordinate(endTime, bottom);

        var borderPath = getSettings().getPath(BOX_BORDER);
        Color fillColor = getSettings().getColor(BOX_FILL_COLOR);
        boolean fillEnabled = getSettings().getBoolean(FILL_BOX);

        /*
         * MotiveWave's Box class accepts two chart Coordinates:
         *
         * first coordinate: start time and top price
         * second coordinate: end time and bottom price
         */
        Box box = new Box(
                topLeft,
                bottomRight,
                borderPath
        );

        /*
         * A null fill color means no fill.
         */
        if (fillEnabled) {
            box.setFillColor(fillColor);
        }
        else {
            box.setFillColor(null);
        }

        addFigure(box);
    }
}