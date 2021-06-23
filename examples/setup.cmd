@ECHO OFF

REM =============================================================
REM Edit the following variables to comply with your local setup.
REM =============================================================

REM Connection string for configuration server.
SET LLCONFIG=http://localhost:10101/get?id=

REM Version of FMU simulator package.
SET VERSION=0.0.1

REM Root directory of FMU simulator package (only change this if you really know what you are doing).
SET FMUSIM_ROOT_DIR=%~DP0..

REM Path to Java JAR file of FMU simulator package.
SET FMUSIM_JAR_FILE=%FMUSIM_ROOT_DIR%\target\assembly\fmusim-0.0.1-jar-with-dependencies.jar 

REM Directory with FMI++ shared libraries.
SET FMIPP_DLL_DIR=%FMUSIM_ROOT_DIR%\target\natives

REM Path to Java JAR file of data point bridge.
SET DPB_JAR_FILE=%FMUSIM_ROOT_DIR%\target\dependency\dpbridge-0.0.1-jar-with-dependencies.jar

REM Path to Java JAR file of data plotter.
SET PLOT_JAR_FILE=%FMUSIM_ROOT_DIR%\target\dependency\plotter-0.0.2-jar-with-dependencies.jar

REM Path to Java JAR file of config server.
SET CONFIG_JAR_FILE=%FMUSIM_ROOT_DIR%\target\dependency\config-0.1.0-jar-with-dependencies.jar

REM Check if environment variable JAVA_HOME has been defined.
IF NOT DEFINED JAVA_HOME (
    ECHO WARNING: environment variable JAVA_HOME not has been defined!
    PAUSE
)
