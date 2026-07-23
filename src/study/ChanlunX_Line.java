package study;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import study.chanlunx.ChanlunXNative;
import study.chanlunx.ChanlunXNative.Endpoint;

import java.util.List;

@StudyHeader(
        namespace = "com.unfinished",
        id = "ChanlunXLine",
        name = "ChanlunX Line",
        desc = "Line segments formed by connecting Chanlun Bi highs and lows (Func2)",
        menu = "ChanlunX",
        menu2 = "Custom Studies",
        overlay = true,
        supportsBarUpdates = true,
        requiresBarUpdates = true
)
public class ChanlunX_Line extends Study
{
    private static final String LINE_PATH = "linePath";
    private static final int MIN_BARS = 5;

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

    @Override
    public void onLoad(Defaults defaults)
    {
        ChanlunXNative.logAvailability(this::info);
    }

    /**
     * Native Chanlun needs the full high/low series, so we recalculate all figures
     * in one pass instead of drawing per-bar.
     */
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
            error("ChanlunX Line: native library unavailable: " + ChanlunXNative.loadError());
            return;
        }

        try {
            float[] high = copyHighs(series, size);
            float[] low = copyLows(series, size);

            float[] bi = ChanlunXNative.computeBi2(high, low);
            List<Endpoint> endpoints = ChanlunXNative.extractEndpoints(bi);

            info("ChanlunX Line: lib=" + ChanlunXNative.loadedPath()
                    + " bars=" + size
                    + " biEndpoints=" + endpoints.size());
            info(ChanlunXNative.summarizeMarks("Bi2", bi, 40));

            var path = getSettings().getPath(LINE_PATH);
            int linesDrawn = 0;

            for (int i = 1; i < endpoints.size(); i++) {
                Endpoint prev = endpoints.get(i - 1);
                Endpoint cur = endpoints.get(i);

                double prevPrice = prev.isTop() ? series.getHigh(prev.index) : series.getLow(prev.index);
                double curPrice = cur.isTop() ? series.getHigh(cur.index) : series.getLow(cur.index);

                var start = new Coordinate(series.getStartTime(prev.index), prevPrice);
                var end = new Coordinate(series.getStartTime(cur.index), curPrice);
                addFigure(new Line(start, end, path));
                linesDrawn++;
            }

            info("ChanlunX Line: drew " + linesDrawn + " Bi segments");
        }
        catch (Throwable t) {
            error("ChanlunX Line failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
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
