@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java from: %JAVA_HOME%
java -version
echo.
echo Starting Gradle build...
"%~dp0gradlew.bat" assembleDebug
