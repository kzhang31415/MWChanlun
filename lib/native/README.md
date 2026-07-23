# ChanlunX native library

Place the compiled ChanlunX shared library here:

- macOS: `libChanlunX.dylib`
- Linux: `libChanlunX.so`
- Windows: `ChanlunX.dll`

The MotiveWave studies load it through JNA (`study.chanlunx.ChanlunXNative`).

## Deploy path

`build/build.xml` copies this file to:

```text
~/MotiveWave Extensions/native/libChanlunX.dylib
```

JNA (`jna-*.jar`) is copied to:

```text
~/MotiveWave Extensions/lib/
```

## Override

If MotiveWave cannot find the dylib automatically:

```bash
# JVM system property (preferred)
-Dchanlunx.library.path=/absolute/path/to/libChanlunX.dylib

# or environment variable
export CHANLUNX_LIBRARY=/absolute/path/to/libChanlunX.dylib
```
