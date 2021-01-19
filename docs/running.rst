Invoking the clients from the command line
==========================================

When running the clients, the use of the ``-c`` command line flag followed by the URI to the configuration (see :doc:`here <configuration>`) is mandatory.
Furthermore, the path to the directory containing the FMI++ shared library files (by default in subdirectory *target/natives* when :doc:`building from source <installation>`) has to be added to the system path.

For example, on Windows this could look something like this:

.. code-block:: winbatch

   SET FMIPP_DLL_DIR=\path\to\lablink-fmusim\target\natives
   SET PATH=%FMIPP_DLL_DIR%;%PATH%
   
   SET LLCONFIG=http://localhost:10101/get?id=
   SET CONFIG_FILE_URI=%LLCONFIG%ait.test.fmusim.dynamic_me.fmu.config
   
   SET FMUSIM=at.ac.ait.lablink.clients.fmu.DynamicFmuModelExchangeAsync
   SET FMUSIM_JAR_FILE=\path\to\lablink-fmusim\target\assembly\fmusim-<VERSION>-jar-with-dependencies.jar
   
   java.exe -cp %FMUSIM_JAR_FILE% %FMUSIM% -c %CONFIG_FILE_URI%
