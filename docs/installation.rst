Maven project dependency
========================

Include the Lablink FMU simulator clients to your own Maven setup by including the following dependency into your *pom.xml*:

.. code-block:: xml

   <dependency>
     <groupId>at.ac.ait.lablink.clients</groupId>
     <artifactId>fmusim</artifactId>
     <version>0.0.1</version>
   </dependency>


Installation from source
========================

Installation from source requires a local **Java Development Kit** installation, for instance the `Oracle Java SE Development Kit 13 <https://www.oracle.com/technetwork/java/javase/downloads/index.html>`_ or the `OpenJDK <https://openjdk.java.net/>`_.

Check out the project and compile it with `Maven <https://maven.apache.org/>`__:

.. code-block:: winbatch

   git clone https://github.com/ait-lablink/lablink-fmusim
   cd lablink-fmusim
   mvnw clean package

This will create JAR file *fmusim-<VERSION>-jar-with-dependencies.jar* in subdirectory *target/assembly*.
Furthermore, all required FMI++ shared library files (DLLs, SOs) will be copied to subdirectory *target/natives*.

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
