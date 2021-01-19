@ECHO OFF

SETLOCAL

REM Load the setup for the examples.
CALL %~DP0\..\setup.cmd

REM Path to class implementing the main routine.
SET PLOT=at.ac.ait.lablink.clients.plotter.PlotterAsync

REM Data point bridge configuration.
SET CONFIG_FILE_URI=%LLCONFIG%ait.test.fmusim.plotter.config

REM Logger configuration.
SET LOGGER_CONFIG=-Dlog4j.configurationFile=%LLCONFIG%ait.all.all.log4j2

REM Specify the maximum size (in bytes) of the memory allocation pool.
SET MEMORY_FLAG=-Xmx1024M

REM Run the example.
java.exe %LOGGER_CONFIG% %MEMORY_FLAG% -cp %PLOT_JAR_FILE% %PLOT% -c %CONFIG_FILE_URI%