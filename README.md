# SignAPK with ARM64/AArch64 Support

This is a modified version of SignAPK that includes support for ARM64/AArch64 platforms (such as Termux on ARM64 devices).

## Key Changes

### Conscrypt Dependency Strategy

The project uses a hybrid approach for Conscrypt dependencies to support both standard JVM environments and ARM64 platforms:

- **Compile-time**: Uses `conscrypt-openjdk-uber` to provide all necessary classes for compilation
- **Runtime**: Uses `conscrypt-android` which includes native libraries for ARM64/AArch64, ARM, x86, and x86_64
- **Testing**: Uses `conscrypt-openjdk-uber` which works in standard JVM test environments

### Native Library Loading

The `ConscryptNativeLoader` class automatically:
1. Detects the current platform (OS and architecture)
2. Extracts the appropriate native library from the JAR to `~/.conscrypt-native` (or fallback locations)
3. Adds the library directory to `java.library.path` so Conscrypt can find it
4. Loads the library using `System.load()` with the absolute path
5. Falls back gracefully to Bouncy Castle and default Java crypto providers if loading fails

**Note**: The library directory is added to `java.library.path` before loading to ensure Conscrypt can locate the native library when it initializes its internal components.

### Supported Platforms

The built JAR includes native libraries for:
- **ARM64 (aarch64)**: `jni/arm64-v8a/libconscrypt_jni.so`
- **ARM (32-bit)**: `jni/armeabi-v7a/libconscrypt_jni.so`
- **x86_64**: `jni/x86_64/libconscrypt_jni.so`
- **x86 (32-bit)**: `jni/x86/libconscrypt_jni.so`

### JAR File Signing Support

Unlike the original SignAPK which primarily focused on APK files, this version gracefully handles signing JAR files:
- Automatically defaults to minSdkVersion=24 when AndroidManifest.xml is not present
- Displays a warning and continues execution
- Can be overridden with `--min-sdk-version` parameter

## Building

```bash
./gradlew clean build
```

The output JAR will be in `signapk/build/libs/signapk-all.jar`

## Usage

### Signing APK files:

```bash
java -jar signapk-all.jar publickey.x509.pem privatekey.pk8 input.apk output.apk
```

### Signing JAR files (e.g., framework.jar):

```bash
java -jar signapk-all.jar publickey.x509.pem privatekey.pk8 input.jar output.jar
```

The tool will automatically detect that it's a JAR file and use appropriate defaults.

### Specifying minSdkVersion explicitly:

```bash
java -jar signapk-all.jar --min-sdk-version 21 publickey.x509.pem privatekey.pk8 input.jar output.jar
```

## Notes for Termux on ARM64

**TL;DR**: signapk automatically configures itself for Termux. For other Java tools (like apktool), see [TERMUX_GUIDE.md](TERMUX_GUIDE.md).

When running in Termux on ARM64 devices:

1. **Automatic Termux Detection**: The tool automatically detects when running in Termux and configures appropriate directories

2. **Temporary Directory Configuration**: 
   - Automatically uses `$TMPDIR` if set (Termux standard)
   - Falls back to `$HOME/tmp` if needed
   - Never tries to write to `/` (root) which would cause permission errors
   - Creates `$HOME/tmp` if it doesn't exist

3. **Native library location**: The tool tries to extract native libraries to `~/.conscrypt-native` first (which works better in Termux than `/tmp`)

4. **Expected behavior**: 
   - If Conscrypt loads successfully, you'll see: `Successfully loaded Conscrypt native library for aarch64`
   - If it fails, you'll see warnings but the tool will continue using Bouncy Castle
   - In Termux, you'll see: `Configured temp directory for Termux: /data/data/com.termux/files/home/tmp`

5. **Signing JAR files**: When signing framework.jar or other JAR files, the tool will show:
   ```
   Warning: Cannot detect minSdkVersion from input file: No AndroidManifest.xml in APK
   Defaulting to minSdkVersion=24. Use --min-sdk-version to override if needed.
   ```
   This is normal and expected - the signing will proceed successfully.

6. **Performance**: Even if Conscrypt native library fails to load, Bouncy Castle provides all necessary cryptographic operations. The application will work correctly, just potentially slightly slower.

### Using with apktool in Termux

If you're using apktool (or other Java tools) before signapk, they may also need temp directory configuration. See the complete [Termux Usage Guide](TERMUX_GUIDE.md) for details.

Quick fix for apktool in Termux:
```bash
mkdir -p $HOME/tmp
export TMPDIR=$HOME/tmp
export _JAVA_OPTIONS="-Djava.io.tmpdir=$HOME/tmp"

# Now run apktool and signapk
java -jar apktool.jar d app.apk
java -jar apktool.jar b app_decompiled -o modified.apk
java -jar signapk-all.jar testkey.x509.pem testkey.pk8 modified.apk signed.apk
```

## Testing

Run the test suite:

```bash
./gradlew test
```

The tests verify that:
- Conscrypt classes are available on the classpath
- The package structure is correct
- The application can compile and run successfully

## Troubleshooting

### "Permission denied" when creating temporary files

The tool now automatically detects Termux and configures the appropriate temporary directory. It will:
1. Check for `$TMPDIR` environment variable
2. Use `$HOME/tmp` if needed
3. Create the directory if it doesn't exist
4. Never try to write to `/` (root directory)

If you still see permission errors, manually set the temp directory:
```bash
export TMPDIR=$HOME/tmp
mkdir -p $TMPDIR
java -jar signapk-all.jar ...
```

### "Cannot create temporary directory for native library"

If you see this error, the tool will try multiple locations:
1. `~/.conscrypt-native` (preferred for Termux)
2. System temp directory (configured as above)
3. `.conscrypt-native` in current directory

Make sure at least one of these locations is writable.

### "no conscrypt_jni in java.library.path"

This is expected when the Android native library dependencies (like `liblog.so`) are not available. The tool will fall back to Bouncy Castle automatically.

### "Cannot detect minSdkVersion"

When signing JAR files (not APKs), this is expected. The tool defaults to minSdkVersion=24. You can override this with `--min-sdk-version` if needed.
