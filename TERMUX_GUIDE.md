# Termux Usage Guide

This guide explains how to use signapk and other Java tools (like apktool) in Termux on ARM64 devices.

## Quick Start

### Method 1: Using the Environment Configuration Script (Recommended)

1. Download and source the configuration script:
```bash
cd ~
# Download the script (if you have it in the repository)
source signapk/scripts/termux-java-env.sh
```

2. Run your Java tools:
```bash
# Now you can run apktool, signapk, or any Java tool
java -jar apktool.jar d app.apk
java -jar signapk-all.jar testkey.x509.pem testkey.pk8 app.apk app-signed.apk
```

### Method 2: Manual Configuration

Set the Java temp directory before running any Java commands:

```bash
# Set up temp directory
mkdir -p $HOME/tmp
export TMPDIR=$HOME/tmp
export _JAVA_OPTIONS="-Djava.io.tmpdir=$HOME/tmp"

# Now run your Java tools
java -jar apktool.jar ...
java -jar signapk-all.jar ...
```

### Method 3: Per-Command Configuration

If you don't want to set environment variables globally, you can set them per command:

```bash
# For apktool
mkdir -p $HOME/tmp
java -Djava.io.tmpdir=$HOME/tmp -jar apktool.jar d app.apk

# For signapk (temp dir is auto-configured, but you can override)
java -Djava.io.tmpdir=$HOME/tmp -jar signapk-all.jar testkey.x509.pem testkey.pk8 app.apk app-signed.apk
```

## Complete Workflow Example

Here's a complete example of decompiling, modifying, recompiling, and signing an APK in Termux:

```bash
#!/bin/bash
# Complete APK modification workflow for Termux

# 1. Set up environment
mkdir -p $HOME/tmp
export TMPDIR=$HOME/tmp
export _JAVA_OPTIONS="-Djava.io.tmpdir=$HOME/tmp"

echo "Environment configured for Termux"
echo "TMPDIR=$TMPDIR"
echo ""

# 2. Decompile APK with apktool
echo "Step 1: Decompiling APK..."
java -jar apktool.jar d original.apk -o app_decompiled

# 3. Make your modifications
echo "Step 2: Make your modifications to files in app_decompiled/"
# ... edit files as needed ...

# 4. Recompile APK
echo "Step 3: Recompiling APK..."
java -jar apktool.jar b app_decompiled -o modified.apk

# 5. Sign the APK
echo "Step 4: Signing APK..."
java -jar signapk-all.jar testkey.x509.pem testkey.pk8 modified.apk modified-signed.apk

echo "Done! Your signed APK is: modified-signed.apk"
```

## Understanding the Permission Error

The error you saw:
```
Exception in thread "main" brut.androlib.exceptions.AndrolibException: java.io.IOException: Permission denied
    at java.base/java.io.File.createTempFile(File.java:2184)
```

This happens because:
1. Java's `File.createTempFile()` tries to use the system temp directory (usually `/tmp`)
2. In Termux, `/tmp` is not writable by default
3. The solution is to configure Java to use `$HOME/tmp` instead

## Why SignApk Works Automatically

The signapk tool in this repository has been modified to automatically detect Termux and configure the temp directory. When you run signapk, you'll see:

```
Configured temp directory for Termux: /data/data/com.termux/files/home/tmp
```

This means signapk has automatically configured itself. However, **other Java tools like apktool need manual configuration** as shown above.

## Permanent Configuration

To make this configuration permanent, add these lines to your `~/.bashrc` or `~/.zshrc`:

```bash
# Java temp directory for Termux
mkdir -p $HOME/tmp
export TMPDIR=$HOME/tmp
export _JAVA_OPTIONS="-Djava.io.tmpdir=$HOME/tmp"
```

Then reload your shell:
```bash
source ~/.bashrc
# or
source ~/.zshrc
```

## Troubleshooting

### Problem: Still getting "Permission denied" errors

**Solution**: Make sure you've set the environment variables:
```bash
echo $TMPDIR
echo $_JAVA_OPTIONS
```

If they're not set, run:
```bash
export TMPDIR=$HOME/tmp
export _JAVA_OPTIONS="-Djava.io.tmpdir=$HOME/tmp"
```

### Problem: apktool fails but signapk works

**Explanation**: signapk auto-configures itself, but apktool doesn't. Use the environment configuration methods above.

### Problem: Temp directory fills up

**Solution**: Clean up old temp files:
```bash
rm -rf $HOME/tmp/*
# Or more safely, only remove files older than 1 day:
find $HOME/tmp -type f -mtime +1 -delete
```

### Problem: Out of space

**Solution**: Check your storage:
```bash
df -h $HOME
du -sh $HOME/tmp
```

## Additional Tips

1. **Use absolute paths**: When working with files, use absolute paths to avoid confusion:
   ```bash
   java -jar ~/tools/signapk-all.jar ~/keys/testkey.x509.pem ~/keys/testkey.pk8 ~/apks/app.apk ~/apks/app-signed.apk
   ```

2. **Check Java version**: Make sure you have a compatible Java version:
   ```bash
   java -version
   ```

3. **Memory settings**: If you're processing large APKs, you might need more memory:
   ```bash
   java -Xmx1G -jar apktool.jar ...
   ```

4. **Combine with other tools**: You can use signapk with other APK tools:
   ```bash
   # Align APK first, then sign
   zipalign -v 4 input.apk aligned.apk
   java -jar signapk-all.jar testkey.x509.pem testkey.pk8 aligned.apk final.apk
   ```

## Getting Help

If you encounter issues:
1. Check that temp directory is writable: `ls -la $HOME/tmp`
2. Verify environment variables: `env | grep -i tmp`
3. Check Java temp directory: `java -XshowSettings:properties 2>&1 | grep tmp`
4. Look for detailed error messages in the output

## Summary

- ✅ signapk: Auto-configures temp directory (no extra setup needed)
- ⚠️ apktool and other Java tools: Need manual temp directory configuration
- 💡 Best practice: Configure environment variables once at the start
- 📝 Make it permanent: Add to ~/.bashrc or ~/.zshrc
