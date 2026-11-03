#!/bin/bash

echo "========================================="
echo "  RASTREO DE INSTALACIÓN DE APK"
echo "========================================="
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}[1/4] Limpiando logs...${NC}"
adb logcat -c
echo -e "${GREEN}✓ Logs limpiados${NC}"
echo ""

echo -e "${BLUE}[2/4] Iniciando monitoreo...${NC}"
echo -e "${YELLOW}Ahora presiona el botón de compilar en CodeAssist${NC}"
echo -e "${YELLOW}Los logs se mostrarán en tiempo real...${NC}"
echo ""

# Función para mostrar logs con colores
adb logcat | while read line; do
    # Resaltar eventos de MainFragment
    if echo "$line" | grep -q "MainFragment"; then
        echo -e "${GREEN}[MainFragment]${NC} $line"
    # Resaltar eventos de ApkInstaller
    elif echo "$line" | grep -q "ApkInstaller"; then
        echo -e "${BLUE}[ApkInstaller]${NC} $line"
    # Resaltar errores
    elif echo "$line" | grep -q "E/"; then
        echo -e "${RED}[ERROR]${NC} $line"
    # Resaltar advertencias
    elif echo "$line" | grep -q "W/"; then
        echo -e "${YELLOW}[WARN]${NC} $line"
    # Resaltar eventos de compilación
    elif echo "$line" | grep -q -E "(Compilation|AAPT2|JavaCompile|DexTask|PackageTask|SignTask)"; then
        echo -e "${GREEN}[BUILD]${NC} $line"
    # Resaltar eventos de instalación de paquetes
    elif echo "$line" | grep -q -E "(PackageInstaller|install|Intent.ACTION_VIEW)"; then
        echo -e "${BLUE}[INSTALL]${NC} $line"
    fi
done

