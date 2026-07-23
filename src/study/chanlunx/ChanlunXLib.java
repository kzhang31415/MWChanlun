package study.chanlunx;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

/**
 * JNA bindings for {@code libChanlunX.dylib} / {@code libChanlunX.so} / {@code ChanlunX.dll}.
 *
 * <p>Mirrors the C ABI from the ChanlunX repo ({@code RegisterTdxFunc}, {@code Func1}–{@code Func9},
 * {@code ChanlunCall}).
 */
public interface ChanlunXLib extends Library {

    /** Function-table entry returned by {@link #RegisterTdxFunc}. */
    class PluginTCalcFuncInfo extends Structure {
        public short nFuncMark;
        public Pointer pCallFunc;

        public PluginTCalcFuncInfo() {
            super();
        }

        public PluginTCalcFuncInfo(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("nFuncMark", "pCallFunc");
        }

        public static class ByReference extends PluginTCalcFuncInfo implements Structure.ByReference {
            public ByReference() {
                super();
            }

            public ByReference(Pointer p) {
                super(p);
            }
        }
    }

    boolean RegisterTdxFunc(PointerByReference pInfo);

    /** Simplified Bi endpoints. */
    void Func1(int nCount, float[] pOut, float[] pHigh, float[] pLow, float[] pIgnore);

    /** Standard Bi endpoints (+1 high / -1 low). */
    void Func2(int nCount, float[] pOut, float[] pHigh, float[] pLow, float[] pIgnore);

    /** Segment endpoints (standard drawing). {@code pIn} is Bi marks. */
    void Func3(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** Segment endpoints (1+1 termination). */
    void Func4(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** ZhongShu / pivot high (zg). */
    void Func5(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** ZhongShu / pivot low (zd). */
    void Func6(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** ZhongShu start/end signals (1=start, 2=end). */
    void Func7(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** ZhongShu direction. */
    void Func8(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** Nth same-direction ZhongShu. */
    void Func9(int nCount, float[] pOut, float[] pIn, float[] pHigh, float[] pLow);

    /** Dispatcher for marks 1–9. */
    boolean ChanlunCall(int nFuncMark, int nCount, float[] pOut, float[] a, float[] b, float[] c);

    static ChanlunXLib load() {
        return Native.load("ChanlunX", ChanlunXLib.class);
    }

    static ChanlunXLib load(String absoluteLibraryPath) {
        return Native.load(absoluteLibraryPath, ChanlunXLib.class);
    }
}
