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
3. Loads the library from a temporary location
4. Falls back gracefully to Bouncy Castle and default Java crypto providers if loading fails

### Supported Platforms

The built JAR includes native libraries for:
- **ARM64 (aarch64)**: `jni/arm64-v8a/libconscrypt_jni.so`
- **ARM (32-bit)**: `jni/armeabi-v7a/libconscrypt_jni.so`
- **x86_64**: `jni/x86_64/libconscrypt_jni.so`
- **x86 (32-bit)**: `jni/x86/libconscrypt_jni.so`

## Building

```bash
./gradlew clean build
```

The output JAR will be in `signapk/build/libs/signapk-all.jar`

## Usage

Same as the original SignAPK:

```bash
java -jar signapk-all.jar publickey.x509.pem privatekey.pk8 input.jar output.jar
```

## Notes for Termux on ARM64

When running in Termux on ARM64 devices, the native Conscrypt library should load successfully because:
1. The JAR includes the ARM64 native library (`libconscrypt_jni.so`)
2. Termux provides the Android system libraries that Conscrypt depends on (like `liblog.so`)
3. The automatic platform detection will select the correct ARM64 library

If the native library fails to load, the application will automatically fall back to using Bouncy Castle and the default Java cryptographic providers, ensuring the application still works.

## Testing

Run the test suite:

```bash
./gradlew test
```

The tests verify that:
- Conscrypt classes are available on the classpath
- The package structure is correct
- The application can compile and run successfully
