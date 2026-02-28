#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
GRADLEW="$PROJECT_DIR/gradlew"
OUTPUT_DIR="$PROJECT_DIR/app/build/outputs/apk"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

print_apks() {
    find "$1" -name "*.apk" 2>/dev/null | while read -r f; do
        echo -e "  ${CYAN}$f${NC}"
    done
}

build_debug() {
    echo ""
    echo "========================================"
    echo "  Building SnapReceipt DEBUG APK..."
    echo "========================================"
    echo ""
    "$GRADLEW" assembleDebug --warning-mode=none -p "$PROJECT_DIR"
    echo ""
    echo -e "${GREEN}[SUCCESS]${NC} Debug APK:"
    print_apks "$OUTPUT_DIR/debug"
}

build_release() {
    echo ""
    echo "========================================"
    echo "  Building SnapReceipt RELEASE APK..."
    echo "========================================"
    echo ""
    "$GRADLEW" assembleRelease --warning-mode=none -p "$PROJECT_DIR"
    echo ""
    echo -e "${GREEN}[SUCCESS]${NC} Release APK:"
    print_apks "$OUTPUT_DIR/release"
}

build_both() {
    echo ""
    echo "========================================"
    echo "  Building DEBUG + RELEASE APKs..."
    echo "========================================"
    echo ""
    "$GRADLEW" assembleDebug assembleRelease --warning-mode=none -p "$PROJECT_DIR"
    echo ""
    echo -e "${GREEN}[SUCCESS]${NC} Output APKs:"
    print_apks "$OUTPUT_DIR"
}

clean_build() {
    echo ""
    echo "Cleaning build artifacts..."
    "$GRADLEW" clean --warning-mode=none -p "$PROJECT_DIR"
    echo -e "${GREEN}[DONE]${NC} Clean complete."
}

usage() {
    echo ""
    echo "SnapReceipt Build Script"
    echo "========================"
    echo ""
    echo "Usage: ./scripts/build.sh [command]"
    echo ""
    echo "Commands:"
    echo "  debug     Build debug APK"
    echo "  release   Build release APK (requires signing config in local.properties)"
    echo "  both      Build both debug and release APKs"
    echo "  clean     Clean all build artifacts"
    echo ""
    echo "Examples:"
    echo "  ./scripts/build.sh debug"
    echo "  ./scripts/build.sh release"
    echo "  ./scripts/build.sh both"
    echo ""
}

case "${1:-}" in
    debug)   build_debug ;;
    release) build_release ;;
    both)    build_both ;;
    clean)   clean_build ;;
    *)       usage ;;
esac
