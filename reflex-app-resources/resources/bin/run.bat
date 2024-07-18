@echo off
setlocal

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

%JAVA_EXECUTABLE% %REFLEX_OPTS% -Dreflex.app.dir="%~dp0\.." -Dreflex.launch.script=%LAUNCH_SCRIPT% -jar "%LIB_PATH%\launch.jar" %*

endlocal