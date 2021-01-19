@ECHO OFF

SETLOCAL

REM Load the setup for the examples.
CALL %~DP0\..\setup.cmd

REM Specify configuration file.
SET CONFIG_FILE_NAME=test-config.db

REM Start configuration server.
java.exe -jar %CONFIG_JAR_FILE% -a run -d %~DP0\%CONFIG_FILE_NAME%