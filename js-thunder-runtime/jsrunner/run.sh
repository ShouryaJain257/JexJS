#!/bin/bash
# run.sh - Run a JavaScript file using JS Thunder Runtime

set -e

if [ ! -d "out" ]; then
    echo "Building first..."
    bash build.sh
fi

if [ $# -eq 0 ]; then
    # Read from stdin
    java -cp out com.thunder.js.Main
else
    java -cp out com.thunder.js.Main "$1"
fi
