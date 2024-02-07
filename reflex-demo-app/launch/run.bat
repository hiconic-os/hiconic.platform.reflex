@echo off
setlocal

if "%JAVA_HOME%"=="" (
    set JAVA_EXECUTABLE=java    
)else (
    set JAVA_EXECUTABLE="%JAVA_HOME%\bin\java"
)

%JAVA_EXECUTABLE% -Djava.util.logging.config.file="%~dp0\..\conf\logging.properties" -Dreflex.app.dir="%~dp0\.." -jar "%~dp0\..\lib\launch.jar" %*

endlocal