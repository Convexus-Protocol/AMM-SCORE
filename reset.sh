#!/bin/bash

find . -name ".project" | xargs rm -f
find . -name ".classpath" | xargs rm -f

find . -name "bin" -type d -not -path "./venv/*" | while read i; do rm -rf "$i"; done
find . -name ".settings" -type d | while read i; do rm -rf "$i"; done
find . -name "build" -type d | while read i; do rm -rf "$i"; done
rm -rf .gradle
rm -rf ./build

/mnt/c/Program\ Files/Microsoft\ VS\ Code/Code.exe ./workspace.code-workspace