@ECHO OFF

SETLOCAL

REM Load the setup for the examples.
CALL "%~DP0\..\setup.cmd"

REM Path to class implementing the main routine.
SET FMUSIM=at.ac.ait.lablink.clients.fmusim.FixedStepFmuModelExchangeAsync

REM Logger configuration.
SET LOGGER_CONFIG=-Dlog4j.configurationFile=%LLCONFIG%ait.all.all.log4j2

REM Data point bridge configuration.
SET CONFIG_FILE_URI=%LLCONFIG%ait.test.fmusim.fixedstep_me.fmu.config

REM Add directory with FMI++ shared libraries to system path.
SET PATH=%FMIPP_DLL_DIR%;%PATH%

REM Run the example.
"%JAVA_HOME%\bin\java.exe" %LOGGER_CONFIG% -cp "%FMUSIM_JAR_FILE%" %FMUSIM% -c %CONFIG_FILE_URI%

PAUSE
