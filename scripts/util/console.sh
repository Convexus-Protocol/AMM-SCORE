#!/bin/bash

set -e

function error {
    echo -ne "\e[91m"
    echo -e  "🚫 ${1}"
    echo -ne "\e[39m"
}

function info {
    echo -ne "\e[32m"
    echo -e  "⌛ ${1}"
    echo -ne "\e[39m"
}

function done {
    echo -ne "\e[32m"
    echo -e  "✅ ${1}"
    echo -ne "\e[39m"
}

function warning {
    echo -ne "\e[93m"
    echo -e  "⚠️ ${1}"
    echo -ne "\e[39m"
}

function debug {
    echo -ne "\e[94m"
    echo -e  "🐛 ${1}"
    echo -ne "\e[39m"
}

function highlight {
    echo -ne "\e[96m"
    echo -e  "🔦 ${1}"
    echo -ne "\e[39m"
}

function success {
    echo -ne "\e[96m"
    echo -e  "[🎉] ${1}"
    echo -ne "\e[39m"
}