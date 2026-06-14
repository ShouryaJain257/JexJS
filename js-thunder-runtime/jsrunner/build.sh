#!/bin/bash
# build.sh - Compile the JS Thunder Runtime

set -e

echo "⚡ Building JS Thunder Runtime..."

# Create output directory
mkdir -p out

# Find all Java source files
SOURCES=$(find src -name "*.java")

# Compile
javac -d out $SOURCES

echo "✅ Build successful! Output in ./out/"
echo ""
echo "Usage:"
echo "  java -cp out com.thunder.js.Main <file.js>     # Run a JS file"
echo "  echo 'console.log(42)' | java -cp out com.thunder.js.Main  # Pipe JS code"
