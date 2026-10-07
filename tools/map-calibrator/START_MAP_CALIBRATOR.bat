@echo off
cd /d "%~dp0"
where python.exe >nul 2>nul
if errorlevel 1 (
  echo Python not found. Install Python 3 or add it to PATH.
  pause
  exit /b 1
)

python.exe -c "from PIL import Image, ImageTk" >nul 2>nul
if errorlevel 1 (
  echo Pillow is not installed for the current Python.
  echo Run: python -m pip install --user Pillow
  pause
  exit /b 1
)

where pythonw.exe >nul 2>nul
if errorlevel 1 (
  echo pythonw.exe not found. Reinstall Python with the standard launcher.
  pause
  exit /b 1
)

start "MHP3rd Map Calibrator" pythonw.exe "%~dp0map_calibrator.py"
