@echo off
setlocal
cd /d "%~dp0"
echo Starting TrackIt dependencies...
docker compose up -d postgres redis
if errorlevel 1 (
  echo.
  echo Docker could not start. Open Docker Desktop, wait until it says running, then run this file again.
  pause
  exit /b 1
)
set "JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Kolkata"
echo.
echo Starting TrackIt with animated bus markers...
"%USERPROFILE%\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
if errorlevel 1 (
  echo.
  echo TrackIt stopped with an error. Read the message above.
  pause
)
endlocal
