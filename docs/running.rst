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

   SET FMUSIM=at.ac.ait.lablink.clients.fmusim.DynamicFmuModelExchangeAsync
   SET FMUSIM_JAR_FILE=\path\to\lablink-fmusim\target\assembly\fmusim-<VERSION>-jar-with-dependencies.jar

   java.exe -cp %FMUSIM_JAR_FILE% %FMUSIM% -c %CONFIG_FILE_URI%

Running with Docker
===================

Pull ``ait1/lablink-fmusim:latest`` from Docker Hub (see :doc:`installation <installation>`).
The image starts the class named by ``FMUSIM_CLASS`` with ``-c`` set to ``${LLCONFIG}${FMUSIM_CONFIG}``.
The defaults are ``LLCONFIG=http://lablink-config:10101/get?id=``, ``FMUSIM_CONFIG=lablink-fmusim.config``, and ``FMUSIM_CLASS=at.ac.ait.lablink.clients.fmusim.DynamicFmuModelExchangeAsync``, so the image expects a configuration server reachable as the host ``lablink-config``.
``MEMORY_FLAG`` is the JVM heap setting and defaults to ``-Xmx512m``.
``LOGGER_ID`` is the logging configuration id and defaults to ``ait.all.log4j2``.
``FMU_DIR`` is passed as the ``fmuDir`` system property and is the base directory for an ``fmusim://`` URI in the configuration.

``--interactive`` and ``--tty`` attach the terminal.
A configuration with ``ClientShell`` set to ``true`` reads commands from that terminal.

The FMU file given by the URI in the FMU configuration has to contain ``binaries/linux64``.
With ``FMU_DIR`` set, an ``fmusim://`` URI is resolved inside that directory.
The examples pack the unpacked Linux zigzag model and run the whole stack with Docker Compose.
See :doc:`here <examples>`.

On Windows, a configuration server on the host is reachable as ``host.docker.internal``:

.. code-block:: winbatch

   docker run --rm --interactive --tty -v C:\path\to\model.fmu:/work/model.fmu -e LLCONFIG=http://host.docker.internal:10101/get?id= -e FMUSIM_CONFIG=ait.test.fmusim.dynamic_me.fmu.config ait1/lablink-fmusim:latest

On Linux, add that hostname explicitly:

.. code-block:: bash

   docker run --rm --interactive --tty --add-host=host.docker.internal:host-gateway -v /path/to/model.fmu:/work/model.fmu -e LLCONFIG=http://host.docker.internal:10101/get?id= -e FMUSIM_CONFIG=ait.test.fmusim.dynamic_me.fmu.config ait1/lablink-fmusim:latest

In that configuration, the FMU URI has to be the path of the mounted file inside the container, for instance ``file:///work/model.fmu``.
Arguments after the image name are passed through to the client.
