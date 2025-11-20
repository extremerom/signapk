#!/bin/bash
#
# Termux Java Environment Configuration Script
# This script configures the Java environment for Termux to avoid permission errors
# when running Java applications like signapk, apktool, etc.
#
# Usage:
#   source termux-java-env.sh
#   # or
#   . termux-java-env.sh
#

# Check if we're running in Termux
if [ -z "$PREFIX" ] && [ -z "$TERMUX_VERSION" ]; then
    echo "Warning: This script is designed for Termux but doesn't detect Termux environment."
    echo "Continuing anyway..."
fi

# Get user home directory
USER_HOME="${HOME:-/data/data/com.termux/files/home}"

# Set up temp directory
TERMUX_TMPDIR="${TMPDIR:-$USER_HOME/tmp}"

# Create temp directory if it doesn't exist
if [ ! -d "$TERMUX_TMPDIR" ]; then
    mkdir -p "$TERMUX_TMPDIR"
    echo "Created temp directory: $TERMUX_TMPDIR"
fi

# Export environment variables for Java
export TMPDIR="$TERMUX_TMPDIR"
export JAVA_OPTS="-Djava.io.tmpdir=$TERMUX_TMPDIR"

# Also set the system property directly for Java commands
export _JAVA_OPTIONS="-Djava.io.tmpdir=$TERMUX_TMPDIR"

echo "Termux Java environment configured:"
echo "  TMPDIR=$TMPDIR"
echo "  JAVA_OPTS=$JAVA_OPTS"
echo "  _JAVA_OPTIONS=$_JAVA_OPTIONS"
echo ""
echo "You can now run Java applications like:"
echo "  java -jar signapk-all.jar ..."
echo "  java -jar apktool.jar ..."
echo ""
