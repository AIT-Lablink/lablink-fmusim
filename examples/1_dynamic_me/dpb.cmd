@ECHO OFF

SETLOCAL

REM Load the setup for the examples.
CALL %~DP0\..\setup.cmd

REM Data point bridge configuration.
SET CONFIG_FILE_URI=%LLCONFIG%ait.test.fmusim.dpb.config

REM Logger configuration.
SET LOGGER_CONFIG=-Dlog4j.configurationFile=%LLCONFIG%ait.all.all.log4j2

REM IPv4 configuration.
SET IPV4_CONFIG=-Djava.net.prefIPv4Stack=true

REM Start the data point bridge.
java.exe %IPV4_CONFIG% %LOGGER_CONFIG% -jar %DPB_JAR_FILE% -c %CONFIG_FILE_URI%