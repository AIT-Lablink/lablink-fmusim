Maven project dependency
========================

The Lablink FMU simulator client's compiled Java package is available on the |MCR|_.
Use it in your local Maven_ setup by including the following dependency into your *pom.xml*:

.. code-block:: xml

   <dependency>
     <groupId>at.ac.ait.lablink.clients</groupId>
     <artifactId>fmusim</artifactId>
     <version>0.0.1</version>
   </dependency>

.. note:: You may have to adapt this snippet to use the latest version, please check the |MCR|_.

Installation from source
========================

Installation from source requires JDK 25.
The Java bindings ``at.ac.ait.fmipp:imp:2.1.0``, which :github_blob:`pom.xml <pom.xml>` depends on, are Java 25 bytecode.

Check out the project and compile it with Maven_:

.. code-block:: winbatch

   git clone https://github.com/ait-lablink/lablink-fmusim
   cd lablink-fmusim
   mvnw clean package

This will create JAR file *fmusim-<VERSION>-jar-with-dependencies.jar* in subdirectory *target/assembly*.
Furthermore, all required FMI++ shared library files (DLLs, SOs) will be copied to subdirectory *target/natives*.

Docker image
============

A ready-to-run image is published on `Docker Hub <https://hub.docker.com/r/ait1/lablink-fmusim>`_ as ``ait1/lablink-fmusim:latest``:

.. code-block:: winbatch

   docker pull ait1/lablink-fmusim:latest

The image starts ``at.ac.ait.lablink.clients.fmusim.DynamicFmuModelExchangeAsync``.
Set ``FMUSIM_CLASS`` to ``at.ac.ait.lablink.clients.fmusim.FixedStepFmuModelExchangeAsync`` to run the fixed-step client.
See :doc:`here <running>` for how to start the container, and :doc:`here <examples>` for the Docker Compose examples.

An FMU loaded by this image has to contain ``binaries/linux64``.
The unpacked Linux build of the zigzag model is :github_tree:`src/test/resources/zigzag-linux64 <src/test/resources/zigzag-linux64>`.
:github_blob:`src/test/resources/zigzag.fmu <src/test/resources/zigzag.fmu>` contains Windows binaries only.
FMI++ v2.1 accepts both ``file:///path`` and the ``file:/path`` form produced by Java's ``File.toURI()`` on Linux.
The client passes that form to FMI++ after it extracts an FMU.


Troubleshooting the installation
================================

**Error message:**

.. code-block:: none

   [ERROR] Failed to execute goal on project fmusim: Could not resolve dependencies for project at.ac.ait.lablink.clients:fmusim:jar:0.0.1:
   Could not find artifact at.ac.ait.fmipp:libfmipp:zip:natives-libfmipp-sundials-windows-release-x64:0.0.1 in central (https://repo1.maven.org/maven2)

The FMU simulator requires the `FMI++ Library <http://fmipp.sourceforge.net>`__ to be installed, including the Java bindings.
They should be avialable via the `AIT Lablink repository <https://github.com/orgs/AIT-Lablink/packages>`__.
In case they are not installed (or not for your specific OS), you can install them yourself to your local Maven repository.
To do so, follow the FMI++ instructions and additionally specify the `CMake <https://cmake.org/>`__ flag ``JAVA_MAVEN_INSTALL:BOOL=ON`` to automatically install the generated files to your local Maven repository.
In this case, you will have to include your local Maven repository to your setup (see `here <https://maven.apache.org/settings.html>`__ ).


.. |MCR| replace:: Maven Central Repository
.. _MCR: https://search.maven.org/artifact/at.ac.ait.lablink.clients/fmusim
.. _Maven: https://maven.apache.org
