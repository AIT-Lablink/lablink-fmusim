Overview
========

The configuration has to be JSON-formatted.
It is divided into the following categories:

:*Client*: basic configuration of the Lablink client
:*FMU*: basic configuration related to the FMU simulator
:*InitialValues*: configuration of initial values of the FMU instance
:*Input*: configuration of the client's inputs, each associated to an FMU input variable
:*Output*: configuration of the client's outputs, each associated to an FMU output variable

In the following, the configuration parameters for these categories are listed.

.. seealso:: See `below <#example-configuration>`_ for an example of a complete JSON configuration.

Basic Lablink Client Configuration
==================================

.. topic:: Required parameters:

  :*ClientName*: client name
  :*GroupName*: group name
  :*ScenarioName*: scenario name
  :*labLinkPropertiesUrl*: URI to Lablink configuration
  :*syncHostPropertiesUrl*: URI to sync host configuration

.. topic:: Optional parameters:

  :*ClientDescription*: description of the client
  :*ClientShell*: activate Lablink shell (default: ``false``).

FMU Simulator Configuration
===========================

.. topic:: Required parameters:

  :*URI*: URI to the FMU.
    Paths can be specified as ``fmusim://relative/path/to/my.fmu``, where the path will be interpreted relative to the path specified via `system property <https://www.ibm.com/docs/en/sdk-java-technology/7?topic=customization-how-specify-javalangsystem-property>`__ ``dmuDir``.
    For instance, using option ``-DfmuDir=/a/b/c`` when starting the plotter client and specifiying ``plotter://x/y/z.fmu`` will result in CSV files being written to directory ```/a/b/c/x/y/z.fmu``.

.. topic:: Optional parameters:

  :*DefaulUpdatePeriod_ms*: default period of the synchronization schedule in milliseconds (default: ``1000``)
  :*LoggingOn*: turn on/off log messages from the FMU (default: ``false``)
  :*IntegratorType*: select integrator type (default: ``bdf``):
  
    :``eu``: Forward Euler method
    :``rk``: 4th order Runge-Kutta method with constant step size
    :``abm``: Adams-Bashforth-Moulton multistep method with adjustable order and constant step size
    :``ck``: 5th order Runge-Kutta-Cash-Karp method with controlled step size
    :``dp``: 5th order Runge-Kutta-Dormand-Prince method with controlled step size
    :``fe``: 8th order Runge-Kutta-Fehlberg method with controlled step size
    :``bs``: Bulirsch-Stoer method with controlled step size
    :``ro``: 4th order Rosenbrock Method for stiff problems
    :``bdf``: Backwards Differentiation formula from Sundials. This stepper has adaptive step size, error control and an internal algorithm for the event search loop. The order varies between 1 and 5. Well suited for stiff problems.
    :``abm2``: Adams-Bashforth-Moulton method from sundials. This stepper has adaptive step size, error control, and an internal algorithm for the event search loop. The order varies between 1 and 12. Well suited for smooth problems.
  
  :*ModelTimeScaleFactor*: simulation time scaling factor, i.e., speed-up or slow-down of progress of logical simulation time (default: ``1``)
  :*ModelStartTime_s*: start time (logical simulation time) for FMU model (default: ``0``)
  :*TimeDiffResolution_s*: resolution for resolving time differences in seconds (default: ``1e-4``)

Optional parameters for **DynamicFmuModelExchangeAsync only** (for expert users):

:*NIntegratorSteps*: number of integration intervals within a synchronozation period (default: ``2``)
:*NSteps*: number of integration steps within each integration interval for fixed-step integrators (default: ``2``)

Initial Value Configuration
===========================

Configuration for each FMU model variable that should be initialized with a specific (non-default) value:

:*VariableName*: name of the FMU variable
:*DataType*: type of the FMU variable, allowed values are ``double``, ``long``, ``boolean`` and ``string``
:*Value*: initial value

Input and Output Configuration
==============================



.. topic:: Required configuration parameters for each input/output:

  :*VariableName*: name of the client's input/output port, has to correspond to an appropriate FMU variable
  :*DataType*: data type of the client's input/output port, has to be compatible to the corresponding FMU variable type; allowed values are ``double``, ``long``, ``boolean`` and ``string``

.. topic:: Optional configuration parameters for each input/output:

  :*Unit*: unit associated to the client's input/output port

Example Configuration
=====================

The following is an example configuration for a *DynamicFmuModelExchangeAsync* client:

.. code-block:: json

   {
     "Client": {
       "ClientDescription": "FMU async simulator example.",
       "ClientName": "TestFMUAsync",
       "ClientShell": true,
       "GroupName": "FMUSimDemo",
       "ScenarioName": "FMUSimAsync",
       "labLinkPropertiesUrl": "http://localhost:10101/get?id=ait.all.all.llproperties",
       "syncHostPropertiesUrl": "http://localhost:10101/get?id=ait.test.fmusim.async.sync-host.properties"
     },
     "FMU": {
       "DefaulUpdatePeriod_ms": 1000,
       "IntegratorType": "bdf",
       "TimeDiffResolution": 1e-06,
       "URI": "file:///C:/Development/lablink/lablink-fmusim/src/test/resources/zigzag.fmu"
     },
     "InitialValues": [
       {
         "DataType": "double",
         "Value": 0,
         "VariableName": "integrator.y_start"
       },
       {
         "DataType": "double",
         "Value": 0.8,
         "VariableName": "k"
       }
     ],
     "Input": [
       {
         "DataType": "double",
         "Unit": "none",
         "VariableName": "k"
       }
     ],
     "Output": [
       {
         "DataType": "double",
         "Unit": "none",
         "VariableName": "x"
       },
       {
         "DataType": "double",
         "Unit": "none",
         "VariableName": "derx"
       }
     ]
   }
