@echo off
setlocal

set LAUNCH_SCRIPT=%~nx0

if "%JAVA_HOME%"=="" (
    set JAVA_EXECUTABLE=java    
)else (
    set JAVA_EXECUTABLE="%JAVA_HOME%\bin\java"
)

%JAVA_EXECUTABLE% %REFLEX_OPTS% -Dreflex.app.dir="%~dp0\.." -Dreflex.launch.script=%LAUNCH_SCRIPT% -jar "%~dp0\..\lib\launch.jar" %*

endlocal