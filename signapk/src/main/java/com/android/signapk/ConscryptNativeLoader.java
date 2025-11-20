/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.signapk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Helper class to load Conscrypt native libraries for ARM64/AArch64 and other platforms.
 * This is needed when using conscrypt-android in a non-Android environment (like Termux).
 */
public class ConscryptNativeLoader {
    
    private static boolean loaded = false;
    
    /**
     * Attempts to load the Conscrypt native library for the current platform.
     * The library is extracted from the JAR to a temporary location and loaded from there.
     * 
     * @return true if the library was successfully loaded, false otherwise
     */
    public static synchronized boolean loadNativeLibrary() {
        if (loaded) {
            return true;
        }
        
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        // Determine the platform-specific library path and name
        String libraryPath = null;
        String libraryName = null;
        
        if (osName.contains("linux")) {
            if (osArch.contains("aarch64") || osArch.contains("arm64")) {
                libraryPath = "/jni/arm64-v8a/libconscrypt_jni.so";
                libraryName = "libconscrypt_jni.so";
            } else if (osArch.contains("amd64") || osArch.contains("x86_64")) {
                libraryPath = "/jni/x86_64/libconscrypt_jni.so";
                libraryName = "libconscrypt_jni.so";
            } else if (osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i686")) {
                libraryPath = "/jni/x86/libconscrypt_jni.so";
                libraryName = "libconscrypt_jni.so";
            } else if (osArch.contains("arm")) {
                libraryPath = "/jni/armeabi-v7a/libconscrypt_jni.so";
                libraryName = "libconscrypt_jni.so";
            }
        }
        
        if (libraryPath == null) {
            System.err.println("Warning: Unsupported platform for Conscrypt native library: " + 
                             osName + " / " + osArch);
            System.err.println("Conscrypt will not be available. Using default Java crypto providers.");
            return false;
        }
        
        try {
            // Check if the library exists in the JAR
            InputStream libraryStream = ConscryptNativeLoader.class.getResourceAsStream(libraryPath);
            if (libraryStream == null) {
                System.err.println("Warning: Conscrypt native library not found in JAR: " + libraryPath);
                System.err.println("Conscrypt will not be available. Using default Java crypto providers.");
                return false;
            }
            
            // Create a temporary file for the library
            File tempDir = Files.createTempDirectory("conscrypt-native").toFile();
            tempDir.deleteOnExit();
            
            File tempLibrary = new File(tempDir, libraryName);
            tempLibrary.deleteOnExit();
            
            // Extract the library to the temporary file
            try (FileOutputStream out = new FileOutputStream(tempLibrary)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = libraryStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                libraryStream.close();
            }
            
            // Make the library executable (important on Unix-like systems)
            tempLibrary.setExecutable(true);
            tempLibrary.setReadable(true);
            
            // Load the library
            System.load(tempLibrary.getAbsolutePath());
            
            loaded = true;
            System.out.println("Successfully loaded Conscrypt native library for " + osArch);
            return true;
            
        } catch (IOException e) {
            System.err.println("Warning: Failed to extract Conscrypt native library: " + e.getMessage());
            System.err.println("Conscrypt will not be available. Using default Java crypto providers.");
            return false;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: Failed to load Conscrypt native library: " + e.getMessage());
            System.err.println("Conscrypt will not be available. Using default Java crypto providers.");
            return false;
        }
    }
}
