@echo off
setlocal EnableDelayedExpansion

set LAUNCH_SCRIPT=%~nx0

if "%JAVA_HOME%"=="" (
    set JAVA_EXECUTABLE=java    
)else (
    set JAVA_EXECUTABLE="%JAVA_HOME%\bin\java"
)

set SHARED_LIB_PATH=%~dp0\..\..\shared-lib
set PRIVATE_LIB_PATH=%~dp0\..\lib

if exist "%PRIVATE_LIB_PATH%" (
    set LIB_PATH=%PRIVATE_LIB_PATH%
) else (
    if exist "%SHARED_LIB_PATH%" (
        set LIB_PATH=%SHARED_LIB_PATH%
    ) else (
        echo could not find any of:
        echo   %PRIVATE_LIB_PATH%
        echo   %SHARED_LIB_PATH%
        exit /b 1
    )
)

set "PACKAGED_JVM_OPTIONS="
set "JVM_OPTIONS_FILE=%~dp0\..\conf\jvm.options"
if exist "%JVM_OPTIONS_FILE%" (
    for /f "usebackq eol=# delims=" %%O in ("%JVM_OPTIONS_FILE%") do set "PACKAGED_JVM_OPTIONS=!PACKAGED_JVM_OPTIONS! %%O"
)

set "CLASSPATH_RESOURCES_OPTION="
if exist "%~dp0\..\classpath-resources\" (
    set "CLASSPATH_RESOURCES_OPTION=-Dreflex.classpath.resources.dir=%~dp0\..\classpath-resources"
)

%JAVA_EXECUTABLE% %PACKAGED_JVM_OPTIONS% %REFLEX_OPTS% %CLASSPATH_RESOURCES_OPTION% -Dreflex.app.dir="%~dp0\.." -Dreflex.launch.script=%LAUNCH_SCRIPT% -Djava.net.useSystemProxies=true -jar "%LIB_PATH%\launch.jar" %*

endlocal
