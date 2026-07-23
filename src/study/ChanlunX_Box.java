package study;

import java.awt.Color;
import java.util.List;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.draw.Box;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import study.chanlunx.ChanlunXNative;
import study.chanlunx.ChanlunXNative.PivotBox;

@StudyHeader(
        namespace = "com.unfinished",
        id = "ChanlunXBox",
        name = "ChanlunX Box",
        desc = "Draws ZhongShu (pivot) boxes from ChanlunX Func2 + Func5/6/7",
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
    private static final int MIN_BARS = 10;

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
    public void onLoad(Defaults defaults)
    {
        ChanlunXNative.logAvailability(this::info);
    }

    @Override
    protected void calculateValues(DataContext ctx)
    {
        clearFigures();

        var series = ctx.getDataSeries();
        int size = series.size();
        if (size < MIN_BARS) {
            return;
        }

        if (!ChanlunXNative.isAvailable()) {
            error("ChanlunX Box: native library unavailable: " + ChanlunXNative.loadError());
            return;
        }

        try {
            float[] high = copyHighs(series, size);
            float[] low = copyLows(series, size);

            // Standard Bi marks, then ZhongShu high / low / start-end on those marks.
            float[] bi = ChanlunXNative.computeBi2(high, low);
            float[] zg = ChanlunXNative.computeZhongShuHigh(bi, high, low);
            float[] zd = ChanlunXNative.computeZhongShuLow(bi, high, low);
            float[] se = ChanlunXNative.computeZhongShuSignals(bi, high, low);

            List<PivotBox> boxes = ChanlunXNative.extractPivotBoxes(zg, zd, se);

            info("ChanlunX Box: lib=" + ChanlunXNative.loadedPath()
                    + " bars=" + size
                    + " biNonZero=" + ChanlunXNative.extractEndpoints(bi).size()
                    + " boxes=" + boxes.size());
            info(ChanlunXNative.summarizeMarks("Bi2", bi, 40));
            info(ChanlunXNative.summarizeMarks("ZS_SE", se, 40));
            info(ChanlunXNative.summarizeBoxes(boxes, 20));

            var borderPath = getSettings().getPath(BOX_BORDER);
            Color fillColor = getSettings().getColor(BOX_FILL_COLOR);
            boolean fillEnabled = getSettings().getBoolean(FILL_BOX);

            int drawn = 0;
            for (PivotBox pivot : boxes) {
                if (pivot.startIndex < 0 || pivot.endIndex >= size || pivot.startIndex > pivot.endIndex) {
                    continue;
                }
                if (pivot.high == 0f || pivot.low == 0f) {
                    continue;
                }

                long startTime = series.getStartTime(pivot.startIndex);
                long endTime = series.getStartTime(pivot.endIndex);

                Coordinate topLeft = new Coordinate(startTime, pivot.high);
                Coordinate bottomRight = new Coordinate(endTime, pivot.low);

                Box box = new Box(topLeft, bottomRight, borderPath);
                box.setFillColor(fillEnabled ? fillColor : null);
                addFigure(box);
                drawn++;
            }

            info("ChanlunX Box: drew " + drawn + " ZhongShu rectangles");
        }
        catch (Throwable t) {
            error("ChanlunX Box failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static float[] copyHighs(DataSeries series, int size)
    {
        float[] high = new float[size];
        for (int i = 0; i < size; i++) {
            high[i] = series.getHigh(i);
        }
        return high;
    }

    private static float[] copyLows(DataSeries series, int size)
    {
        float[] low = new float[size];
        for (int i = 0; i < size; i++) {
            low[i] = series.getLow(i);
        }
        return low;
    }
}
