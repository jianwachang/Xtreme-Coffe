@echo off
REM ============================================================
REM  Extreme Coffee - Compila l'APK debug in locale (1 click)
REM  Doppio click su questo file dentro la cartella del progetto.
REM ============================================================
cd /d "%~dp0"

echo.
echo === Compilazione APK debug in corso... ===
echo.

call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo !!! Compilazione FALLITA. Leggi i messaggi sopra. !!!
  pause
  exit /b 1
)

echo.
echo === FATTO! ===
echo APK creato in:
echo   app\build\outputs\apk\debug\app-debug.apk
echo.

REM Apre la cartella dell'APK in Esplora file
start "" "%~dp0app\build\outputs\apk\debug"

pause
