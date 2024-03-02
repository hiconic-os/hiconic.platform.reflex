@echo off
setlocal
set REFLEX_OPTS=-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y
CALL %~dp0/run %*
endlocal