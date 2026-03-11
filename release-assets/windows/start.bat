@echo off
setlocal
cd /d "%~dp0"

if not exist "ModernMusicBot.properties" (
  echo Missing ModernMusicBot.properties
  echo Copy ModernMusicBot.properties.example to ModernMusicBot.properties and set your bot token.
  pause
  exit /b 1
)

set "JAVA_BIN=java"
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)

"%JAVA_BIN%" --enable-native-access=ALL-UNNAMED -jar DiscordModernMusicBot.jar ModernMusicBot.properties
pause