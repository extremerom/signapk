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
2. Extracts the appropriate native library from the JAR
3. Loads the library from a temporary location (tries `~/.conscrypt-native` first for Termux compatibility)
4. Falls back gracefully to Bouncy Castle and default Java crypto providers if loading fails

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

When running in Termux on ARM64 devices:

1. **Native library location**: The tool tries to extract native libraries to `~/.conscrypt-native` first (which works better in Termux than `/tmp`)

2. **Expected behavior**: 
   - If Conscrypt loads successfully, you'll see: `Successfully loaded Conscrypt native library for aarch64`
   - If it fails, you'll see warnings but the tool will continue using Bouncy Castle

3. **Signing JAR files**: When signing framework.jar or other JAR files, the tool will show:
   ```
   Warning: Cannot detect minSdkVersion from input file: No AndroidManifest.xml in APK
   Defaulting to minSdkVersion=24. Use --min-sdk-version to override if needed.
   ```
   This is normal and expected - the signing will proceed successfully.

4. **Performance**: Even if Conscrypt native library fails to load, Bouncy Castle provides all necessary cryptographic operations. The application will work correctly, just potentially slightly slower.

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

### "Cannot create temporary directory for native library"

If you see this error, the tool will try multiple locations:
1. `~/.conscrypt-native` (preferred for Termux)
2. System temp directory
3. `.conscrypt-native` in current directory

Make sure at least one of these locations is writable.

### "no conscrypt_jni in java.library.path"

This is expected when the Android native library dependencies (like `liblog.so`) are not available. The tool will fall back to Bouncy Castle automatically.

### "Cannot detect minSdkVersion"

When signing JAR files (not APKs), this is expected. The tool defaults to minSdkVersion=24. You can override this with `--min-sdk-version` if needed.
