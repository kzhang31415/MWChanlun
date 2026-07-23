package study.chanlunx;

import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads {@code libChanlunX} and exposes high-level Chanlun helpers for MotiveWave studies.
 *
 * <p>Library search order:
 * <ol>
 *   <li>{@code -Dchanlunx.library.path=/absolute/path/to/libChanlunX.dylib}</li>
 *   <li>{@code CHANLUNX_LIBRARY} environment variable</li>
 *   <li>{@code ~/MotiveWave Extensions/native/libChanlunX.*}</li>
 *   <li>{@code ~/MotiveWave Extensions/lib/libChanlunX.*}</li>
 *   <li>project-relative {@code lib/native/libChanlunX.*} (dev / ant deploy)</li>
 *   <li>plain name {@code ChanlunX} via {@code java.library.path} / JNA defaults</li>
 * </ol>
 */
public final class ChanlunXNative {
    private static final Logger JUL = Logger.getLogger(ChanlunXNative.class.getName());

    private static final Object LOCK = new Object();
    private static volatile ChanlunXLib LIB;
    private static volatile String LOADED_PATH = "(unresolved)";
    private static volatile String LOAD_ERROR;

    private ChanlunXNative() {
    }

    /** Bi / segment endpoint: non-zero marks only. */
    public static final class Endpoint {
        public final int index;
        /** {@code +1} = top (use high), {@code -1} = bottom (use low). */
        public final float mark;

        public Endpoint(int index, float mark) {
            this.index = index;
            this.mark = mark;
        }

        public boolean isTop() {
            return mark > 0;
        }

        @Override
        public String toString() {
            return "Endpoint{i=" + index + ", mark=" + mark + "}";
        }
    }

    /** One ZhongShu (pivot) rectangle derived from Func5/6/7. */
    public static final class PivotBox {
        public final int startIndex;
        public final int endIndex;
        public final float high;
        public final float low;

        public PivotBox(int startIndex, int endIndex, float high, float low) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.high = high;
            this.low = low;
        }

        @Override
        public String toString() {
            return "PivotBox{[" + startIndex + ".." + endIndex + "], high=" + high + ", low=" + low + "}";
        }
    }

    public static boolean isAvailable() {
        return getLib() != null;
    }

    public static String loadedPath() {
        getLib();
        return LOADED_PATH;
    }

    public static String loadError() {
        getLib();
        return LOAD_ERROR;
    }

    public static ChanlunXLib getLib() {
        ChanlunXLib local = LIB;
        if (local != null) {
            return local;
        }
        synchronized (LOCK) {
            if (LIB != null) {
                return LIB;
            }
            tryLoad();
            return LIB;
        }
    }

    private static void tryLoad() {
        List<String> candidates = candidatePaths();
        List<String> errors = new ArrayList<>();

        for (String path : candidates) {
            try {
                File f = new File(path);
                if (!f.isFile()) {
                    errors.add("missing: " + path);
                    continue;
                }
                ChanlunXLib lib = ChanlunXLib.load(f.getAbsolutePath());
                // Touch the ABI so load failures surface early.
                PointerByReference table = new PointerByReference();
                if (!lib.RegisterTdxFunc(table) || table.getValue() == null) {
                    throw new IllegalStateException("RegisterTdxFunc failed for " + path);
                }
                LIB = lib;
                LOADED_PATH = f.getAbsolutePath();
                LOAD_ERROR = null;
                JUL.info("Loaded ChanlunX native library from " + LOADED_PATH);
                return;
            }
            catch (Throwable t) {
                errors.add(path + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        // Last resort: let JNA resolve "ChanlunX" from java.library.path / DYLD_LIBRARY_PATH.
        try {
            ChanlunXLib lib = ChanlunXLib.load();
            PointerByReference table = new PointerByReference();
            if (!lib.RegisterTdxFunc(table) || table.getValue() == null) {
                throw new IllegalStateException("RegisterTdxFunc failed for name ChanlunX");
            }
            LIB = lib;
            LOADED_PATH = "ChanlunX (JNA name lookup)";
            LOAD_ERROR = null;
            JUL.info("Loaded ChanlunX via JNA name lookup");
            return;
        }
        catch (Throwable t) {
            errors.add("name ChanlunX -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        LOAD_ERROR = String.join(" | ", errors);
        JUL.log(Level.SEVERE, "Failed to load ChanlunX native library: " + LOAD_ERROR);
    }

    private static List<String> candidatePaths() {
        List<String> paths = new ArrayList<>();

        String prop = System.getProperty("chanlunx.library.path");
        if (prop != null && !prop.isBlank()) {
            paths.add(prop.trim());
        }

        String env = System.getenv("CHANLUNX_LIBRARY");
        if (env != null && !env.isBlank()) {
            paths.add(env.trim());
        }

        String home = System.getProperty("user.home", "");
        String[] names = libraryFileNames();
        for (String name : names) {
            paths.add(home + "/MotiveWave Extensions/native/" + name);
            paths.add(home + "/MotiveWave Extensions/lib/" + name);
        }

        // Dev tree / ant working directory variants.
        String userDir = System.getProperty("user.dir", ".");
        for (String name : names) {
            paths.add(userDir + "/lib/native/" + name);
            paths.add(userDir + "/../lib/native/" + name);
            paths.add(userDir + "/native/" + name);
        }

        // Classes may live under MotiveWave Extensions/dev — walk up a level.
        try {
            Path self = Path.of(ChanlunXNative.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(self) ? self : self.getParent();
            if (base != null) {
                for (String name : names) {
                    paths.add(base.resolve("../native/" + name).normalize().toString());
                    paths.add(base.resolve("../lib/" + name).normalize().toString());
                    paths.add(base.resolve("../../lib/native/" + name).normalize().toString());
                }
            }
        }
        catch (Exception ignored) {
            // ProtectionDomain may be unavailable under some classloaders.
        }

        return paths;
    }

    private static String[] libraryFileNames() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return new String[] {"libChanlunX.dylib", "ChanlunX.dylib"};
        }
        if (os.contains("win")) {
            return new String[] {"ChanlunX.dll", "libChanlunX.dll"};
        }
        return new String[] {"libChanlunX.so", "ChanlunX.so"};
    }

    // -------------------------------------------------------------------------
    // High-level API
    // -------------------------------------------------------------------------

    /** Standard Bi marks ({@code Func2}): {@code +1} high, {@code -1} low, else 0. */
    public static float[] computeBi2(float[] high, float[] low) {
        return callBi(2, high, low);
    }

    /** Simplified Bi marks ({@code Func1}). */
    public static float[] computeBi1(float[] high, float[] low) {
        return callBi(1, high, low);
    }

    /** Segment endpoints ({@code Func3}) from Bi marks. */
    public static float[] computeDuan(float[] bi, float[] high, float[] low) {
        return callOnBi(3, bi, high, low);
    }

    public static float[] computeZhongShuHigh(float[] bi, float[] high, float[] low) {
        return callOnBi(5, bi, high, low);
    }

    public static float[] computeZhongShuLow(float[] bi, float[] high, float[] low) {
        return callOnBi(6, bi, high, low);
    }

    public static float[] computeZhongShuSignals(float[] bi, float[] high, float[] low) {
        return callOnBi(7, bi, high, low);
    }

    public static List<Endpoint> extractEndpoints(float[] marks) {
        List<Endpoint> out = new ArrayList<>();
        if (marks == null) {
            return out;
        }
        for (int i = 0; i < marks.length; i++) {
            if (marks[i] != 0f) {
                out.add(new Endpoint(i, marks[i]));
            }
        }
        return out;
    }

    /**
     * Pair Func7 start/end signals with Func5/6 levels into pivot boxes.
     */
    public static List<PivotBox> extractPivotBoxes(float[] zg, float[] zd, float[] se) {
        List<PivotBox> boxes = new ArrayList<>();
        if (zg == null || zd == null || se == null) {
            return boxes;
        }
        int n = Math.min(zg.length, Math.min(zd.length, se.length));
        Integer start = null;
        float high = 0f;
        float low = 0f;

        for (int i = 0; i < n; i++) {
            if (se[i] == 1f) {
                start = i;
                high = zg[i];
                low = zd[i];
            }
            else if (se[i] == 2f && start != null) {
                if (zg[i] != 0f) {
                    high = zg[i];
                }
                if (zd[i] != 0f) {
                    low = zd[i];
                }
                if (high != 0f && low != 0f && i >= start) {
                    boxes.add(new PivotBox(start, i, high, low));
                }
                start = null;
            }
            else if (start != null) {
                if (zg[i] != 0f) {
                    high = zg[i];
                }
                if (zd[i] != 0f) {
                    low = zd[i];
                }
            }
        }
        return boxes;
    }

    /** Compact summary of non-zero marks for MotiveWave {@code info()} logging. */
    public static String summarizeMarks(String label, float[] marks, int maxEntries) {
        if (marks == null) {
            return label + ": null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" (n=").append(marks.length).append(", nonzero=");
        List<Endpoint> eps = extractEndpoints(marks);
        sb.append(eps.size()).append("): ");
        int limit = Math.min(maxEntries, eps.size());
        for (int i = 0; i < limit; i++) {
            Endpoint e = eps.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('[').append(e.index).append(':').append(e.mark).append(']');
        }
        if (eps.size() > limit) {
            sb.append(", ... (+").append(eps.size() - limit).append(" more)");
        }
        return sb.toString();
    }

    public static String summarizeBoxes(List<PivotBox> boxes, int maxEntries) {
        if (boxes == null) {
            return "boxes: null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("boxes (count=").append(boxes.size()).append("): ");
        int limit = Math.min(maxEntries, boxes.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(boxes.get(i));
        }
        if (boxes.size() > limit) {
            sb.append("; ... (+").append(boxes.size() - limit).append(" more)");
        }
        return sb.toString();
    }

    public static void logAvailability(Consumer<String> logger) {
        if (logger == null) {
            return;
        }
        if (isAvailable()) {
            logger.accept("ChanlunX native library ready: " + loadedPath());
        }
        else {
            logger.accept("ChanlunX native library NOT available: " + loadError());
        }
    }

    private static float[] callBi(int funcMark, float[] high, float[] low) {
        requireSameLength(high, low, "high/low");
        ChanlunXLib lib = requireLib();
        float[] out = new float[high.length];
        float[] ignore = new float[] {0f};
        boolean ok = lib.ChanlunCall(funcMark, high.length, out, high, low, ignore);
        if (!ok) {
            throw new IllegalStateException("ChanlunCall(" + funcMark + ") returned false");
        }
        return out;
    }

    private static float[] callOnBi(int funcMark, float[] bi, float[] high, float[] low) {
        requireSameLength(bi, high, "bi/high");
        requireSameLength(high, low, "high/low");
        ChanlunXLib lib = requireLib();
        float[] out = new float[bi.length];
        boolean ok = lib.ChanlunCall(funcMark, bi.length, out, bi, high, low);
        if (!ok) {
            throw new IllegalStateException("ChanlunCall(" + funcMark + ") returned false");
        }
        return out;
    }

    private static ChanlunXLib requireLib() {
        ChanlunXLib lib = getLib();
        if (lib == null) {
            throw new IllegalStateException("ChanlunX library not loaded: " + LOAD_ERROR);
        }
        return lib;
    }

    private static void requireSameLength(float[] a, float[] b, String label) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException(label + " arrays must be non-null and same length");
        }
    }
}
