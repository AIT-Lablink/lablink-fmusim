//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import at.ac.ait.fmipp.imp.FMUModelExchangeV2;
import at.ac.ait.fmipp.imp.IntegratorType;
import at.ac.ait.fmipp.imp.fmiStatus;

import at.ac.ait.lablink.core.client.ci.mqtt.impl.MqttCommInterfaceUtility;
import at.ac.ait.lablink.core.client.ex.ClientNotReadyException;
import at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException;
import at.ac.ait.lablink.core.client.ex.DataTypeNotSupportedException;
import at.ac.ait.lablink.core.client.ex.InvalidCastForServiceValueException;
import at.ac.ait.lablink.core.client.ex.NoServicesInClientLogicException;
import at.ac.ait.lablink.core.client.ex.NoSuchCommInterfaceException;
import at.ac.ait.lablink.core.client.ex.ServiceIsNotRegisteredWithClientException;
import at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType;
import at.ac.ait.lablink.core.client.impl.LlClient;
import at.ac.ait.lablink.core.service.IImplementedService;
import at.ac.ait.lablink.core.utility.Utility;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Class FixedStepFmuModelExchangeAsync.
 *
 * <p>This client simulates FMUs for Model Exchange and synchronizes them periodically to real
 * time. At each synchronization point the client updates its output ports according to the
 * selected FMU model ouput variables. New inputs to the client are delayed to the next
 * synchronization point.
 */
public class FixedStepFmuModelExchangeAsync extends FmuSimBase implements Runnable {

  // Tags for FMU simulator configuration.
  protected static final String FMU_LOGGING_TAG = "LoggingOn";
  protected static final String FMU_TIME_DIFF_RES_TAG = "TimeDiffResolution_s";
  protected static final String FMU_INTEGRATOR_TYPE_TAG = "IntegratorType";
  protected static final String FMU_MODEL_NAME_TAG = "ModelName";
  protected static final String FMU_URI_TAG = "URI";
  protected static final String FMU_DEFAULT_UPDATE_PERIOD_TAG = "DefaulUpdatePeriod_ms";
  protected static final String FMU_NUM_STEPS_TAG = "NSteps";
  protected static final String FMU_NUM_INTEGRATOR_STEPS_TAG = "NIntegratorSteps";
  protected static final String FMU_MODEL_START_TIME_TAG = "ModelStartTime_s";
  protected static final String FMU_MODEL_SCALE_TIME_TAG = "ModelTimeScaleFactor";

  // Tags for input configuration.
  protected static final String INPUT_DATATYPE_TAG = "DataType";
  protected static final String INPUT_ID_TAG = "VariableName";
  protected static final String INPUT_UNIT_TAG = "Unit";

  // Tags for output configuration.
  protected static final String OUTPUT_DATATYPE_TAG = "DataType";
  protected static final String OUTPUT_ID_TAG = "VariableName";
  protected static final String OUTPUT_UNIT_TAG = "Unit";

  /**
   * This instance of class FMUModelExchangeV2 (from the FMI++ Library) implements
   * the actual FMU simulator. It allows to include an FMU for Model Exchange in
   * a fixed-step simulation.
   */
  private FMUModelExchangeV2 fmu;

  /** Name of the FMU model. */
  private String modelName;

  /** Current simulation time (logical simulation time in seconds). */
  private double syncTimeSec;

  /** Simulation time scaling factor (speed-up or slow-down of simulation). */
  private double syncTimeScaleFactor;

  /** Next synchronization point (logical simulation time in seconds). */
  private double nextSyncPointSec;

  /** Period between synchronization points (in seconds). */
  private double stepSizeSec;

  /** Resolution for resolving time differences (in seconds). */
  private double timeDiffResSec;

  /** This flag signals that new inputs for the FMU are available. */
  private boolean inputsAvailable;

  /** Lock used for preventing racing conditions between threads
   *  of the main event loop and the input data notifiers.
   */
  private final Object updateInputsLock = new Object();

  /** Thread pool executor service for synchronization schedules. */
  private static final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor();

  /**
   * The main method.
   *
   * @param args arguments to main method
   * @throws at.ac.ait.lablink.core.client.ex.ClientNotReadyException
   *   client not ready
   * @throws at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException
   *   comm interface not supported
   * @throws at.ac.ait.lablink.core.client.ex.DataTypeNotSupportedException
   *   data type not supported
   * @throws at.ac.ait.lablink.core.client.ex.InvalidCastForServiceValueException
   *   invalid cast for service value
   * @throws at.ac.ait.lablink.core.client.ex.NoServicesInClientLogicException
   *   no services in client logic
   * @throws at.ac.ait.lablink.core.client.ex.NoSuchCommInterfaceException
   *   no such comm interface
   * @throws at.ac.ait.lablink.core.client.ex.ServiceIsNotRegisteredWithClientException
   *   service is not registered with client
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   * @throws org.apache.commons.cli.ParseException
   *   parse exception
   * @throws org.apache.commons.configuration.ConfigurationException
   *   configuration error
   * @throws org.json.simple.parser.ParseException
   *   parse error
   * @throws java.io.IOException
   *   IO error
   * @throws java.io.IOException
   *   IO exception error
   * @throws java.net.MalformedURLException
   *   malformed URL
   * @throws java.net.URISyntaxException
   *   URI syntax error
   * @throws java.util.NoSuchElementException
   *   no such element
   */
  public static void main( String[] args ) throws
      at.ac.ait.lablink.core.client.ex.ClientNotReadyException,
      at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException,
      at.ac.ait.lablink.core.client.ex.DataTypeNotSupportedException,
      at.ac.ait.lablink.core.client.ex.InvalidCastForServiceValueException,
      at.ac.ait.lablink.core.client.ex.NoServicesInClientLogicException,
      at.ac.ait.lablink.core.client.ex.NoSuchCommInterfaceException,
      at.ac.ait.lablink.core.client.ex.ServiceIsNotRegisteredWithClientException,
      at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType,
      org.apache.commons.cli.ParseException,
      org.apache.commons.configuration.ConfigurationException,
      org.json.simple.parser.ParseException,
      java.io.IOException,
      java.net.MalformedURLException,
      java.net.URISyntaxException,
      java.util.NoSuchElementException {

    // Retrieve configuration.
    JSONObject jsonConfig = FmuSimBase.getConfig( args );

    // Instantiate FMU client.
    FixedStepFmuModelExchangeAsync fmuClient = new FixedStepFmuModelExchangeAsync( jsonConfig );

    if ( true == FmuSimBase.getWriteConfigAndExitFlag() ) {
      // Run a test (write client config and exit).
      TestUtil.writeConfigAndExit( fmuClient );
    } else {
      fmuClient.startEventLoop();
    }
  }

  /**
   * Constructor.
   *
   * @param jsonConfig configuration data (JSON format)
   * @throws at.ac.ait.lablink.core.client.ex.ClientNotReadyException
   *   client not ready
   * @throws at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException
   *   comm interface not supported
   * @throws at.ac.ait.lablink.core.client.ex.DataTypeNotSupportedException
   *   data type not supported
   * @throws at.ac.ait.lablink.core.client.ex.InvalidCastForServiceValueException
   *   invalid cast for service value
   * @throws at.ac.ait.lablink.core.client.ex.NoServicesInClientLogicException
   *   no services in client logic
   * @throws at.ac.ait.lablink.core.client.ex.NoSuchCommInterfaceException
   *   no such comm interface
   * @throws at.ac.ait.lablink.core.client.ex.ServiceIsNotRegisteredWithClientException
   *   service is not registered with client
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   * @throws org.apache.commons.configuration.ConfigurationException
   *   configuration error
   * @throws java.io.IOException
   *   IO exception error
   * @throws java.net.URISyntaxException
   *   URI syntax error
   * @throws java.util.NoSuchElementException
   *   no such element
   */
  public FixedStepFmuModelExchangeAsync( JSONObject jsonConfig ) throws
      at.ac.ait.lablink.core.client.ex.ClientNotReadyException,
      at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException,
      at.ac.ait.lablink.core.client.ex.DataTypeNotSupportedException,
      at.ac.ait.lablink.core.client.ex.InvalidCastForServiceValueException,
      at.ac.ait.lablink.core.client.ex.NoServicesInClientLogicException,
      at.ac.ait.lablink.core.client.ex.NoSuchCommInterfaceException,
      at.ac.ait.lablink.core.client.ex.ServiceIsNotRegisteredWithClientException,
      at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType,
      org.apache.commons.configuration.ConfigurationException,
      java.io.IOException,
      java.net.URISyntaxException,
      java.util.NoSuchElementException {

    super( jsonConfig );
  }


  //
  // Implementation of Runnable interface (main event loop).
  //

  /**
   * Main function called by the FMU simulator event loop.
   */
  public void run() {

    // Compute timestamp of next synchronization point.
    nextSyncPointSec = nextSyncPointSec + syncTimeScaleFactor * stepSizeSec;

    // In case an internal event is encountered, the integration stops. Therefore, the FMU model's
    // integration method may be called several times until next synchronization point is reached.
    while ( syncTimeSec < nextSyncPointSec - timeDiffResSec ) {
      syncTimeSec = fmu.integrate( nextSyncPointSec );
    }

    // Update the outputs of the client.
    updateOutputs();

    // This lock prevents the input data notifiers to call "notifyEventLoop" at the same time.
    synchronized ( updateInputsLock ) {

      if ( true == inputsAvailable ) {
        updateInputs();
        inputsAvailable = false;
      }
    }
  }

  //
  // Implementation of abstract methods specified in class FmuSimBase
  //

  /**
   * Start the event loop of the FMU simulator.
   */
  protected void startEventLoop() {

    // In case an internal event is encountered, the integration stops. Therefore, the FMU model's
    // integration method may be called several times until next synchronization point is reached.
    while ( syncTimeSec < nextSyncPointSec - timeDiffResSec ) {
      syncTimeSec = fmu.integrate( nextSyncPointSec );
    }

    // Update the outputs of the client.
    updateOutputs();

    // Initialize flag.
    inputsAvailable = false;

    // Start a new FMU synchronization schedule.
    long updatePeriodMillis = Math.round( 1e3 * stepSizeSec );
    executor.scheduleAtFixedRate( this,
        updatePeriodMillis, updatePeriodMillis, TimeUnit.MILLISECONDS );

  }


  /**
   * This function is called by input data notifiers, to signal that new inputs are available.
   */
  public void notifyEventLoop() {

    synchronized ( updateInputsLock ) {
      // Set flag to true.
      inputsAvailable = true;
    }
  }


  /**
   * Configure the FMU simulator.
   *
   * @param fmuConfig configuration data (JSON format)
   */
  protected void configureFmu( JSONObject fmuConfig ) throws
      java.io.IOException,
      java.net.URISyntaxException {

    logger.info( "Configuring the FMU simulator ..." );

    String rawFmuUri = ConfigUtil.<String>getRequiredConfigParam( fmuConfig,
        FMU_URI_TAG, String.format( "FMU URI missing (%1$s)", FMU_URI_TAG ) );

    boolean logging = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_LOGGING_TAG, false );

    timeDiffResSec = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_TIME_DIFF_RES_TAG, 1e-4 );

    String strIntegratorType = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_INTEGRATOR_TYPE_TAG, "bdf" );

    URI fmuFileUri = getFmuFileUri( rawFmuUri );

    String[] fmuUriAndModelName = FmuUnzip.extractFmu( fmuFileUri );

    int fmuLogging = logging ? 1 : 0;

    IntegratorType fmuIntegratorType = IntegratorUtil.getIntegratorType( strIntegratorType );

    this.modelName = fmuUriAndModelName[1];

    this.fmu = new FMUModelExchangeV2( fmuUriAndModelName[0], fmuUriAndModelName[1],
        fmuLogging, false, timeDiffResSec, fmuIntegratorType );
  }


  /**
   * Configure the LabLink client data services, which serve as inputs for the FMU simulator.
   *
   * @param inputConfigList configuration data (JSON format)
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   */
  protected void configureInputs( JSONArray inputConfigList ) throws
      at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType {

    logger.info( "Configuring client inputs..." );

    @SuppressWarnings( "rawtypes" )
    Iterator inputConfigListIter = inputConfigList.iterator();

    // Create data point consumer for each input.
    while ( inputConfigListIter.hasNext() ) {
      JSONObject inputConfig = (JSONObject) inputConfigListIter.next();

      String inputId = ConfigUtil.<String>getRequiredConfigParam( inputConfig, INPUT_ID_TAG,
          String.format( "FMU input name missing (%1$s)", INPUT_ID_TAG ) );

      String dataType = ConfigUtil.<String>getRequiredConfigParam( inputConfig, INPUT_DATATYPE_TAG,
          String.format( "FMU input data type missing (%1$s)", INPUT_DATATYPE_TAG ) );

      String unit = ConfigUtil.getOptionalConfigParam( inputConfig, INPUT_UNIT_TAG, "" );

      int pos;

      if ( dataType.toLowerCase().equals( "double" ) ) {
        logger.info( "add new double input: {}", inputId );
        realInputNames.add( inputId );
        realInputs.add( 0. ); // TODO set to initial value
        pos = realInputs.size() - 1;
      } else if ( dataType.toLowerCase().equals( "long" ) ) {
        logger.info( "add new long input: {}", inputId );
        intInputNames.add( inputId );
        intInputs.add( 0L ); // TODO: set to initial value
        pos = intInputs.size() - 1;
      } else if ( dataType.toLowerCase().equals( "boolean" ) ) {
        logger.info( "add new boolean input: {}", inputId );
        boolInputNames.add( inputId );
        boolInputs.add( (char) 0 ); // TODO: set to initial value
        pos = boolInputs.size() - 1;
      } else if ( dataType.toLowerCase().equals( "string" ) ) {
        logger.info( "add new string input: {}", inputId );
        strInputNames.add( inputId );
        strInputs.add( "" ); // TODO: set to initial value
        pos = strInputs.size() - 1;
      } else {
        throw new IllegalArgumentException(
            String.format( "FMU input data type not supported: '%1$s'", dataType )
        );
      }

      this.addInputDataService( inputId, unit, dataType, pos );
    }
  }


  /**
   * Configure the LabLink client data services, which serve as outputs for the FMU simulator.
   *
   * @param outputConfigList configuration data (JSON format)
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   */
  protected void configureOutputs( JSONArray outputConfigList ) throws
      at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType {

    logger.info( "Configuring client outputs..." );

    @SuppressWarnings( "rawtypes" )
    Iterator outputConfigListIter = outputConfigList.iterator();

    while ( outputConfigListIter.hasNext() ) {
      JSONObject outputConf = (JSONObject) outputConfigListIter.next();

      String outputId = ConfigUtil.<String>getRequiredConfigParam( outputConf, OUTPUT_ID_TAG,
          String.format( "FMU output name missing (%1$s)", OUTPUT_ID_TAG ) );

      String dataType = ConfigUtil.<String>getRequiredConfigParam( outputConf, OUTPUT_DATATYPE_TAG,
          String.format( "FMU output data type missing (%1$s)", OUTPUT_DATATYPE_TAG ) );

      String unit = ConfigUtil.getOptionalConfigParam( outputConf, OUTPUT_UNIT_TAG, "" );

      this.addOutputDataService( outputId, dataType, unit );

      if ( dataType.toLowerCase().equals( "double" ) ) {
        logger.info( "add new double output: {}", outputId );
        realOutputNames.add( outputId );
      } else if ( dataType.toLowerCase().equals( "long" ) ) {
        logger.info( "add new long output: {}", outputId );
        intOutputNames.add( outputId );
      } else if ( dataType.toLowerCase().equals( "boolean" ) ) {
        logger.info( "add new boolean output: {}", outputId );
        boolOutputNames.add( outputId );
      } else if ( dataType.toLowerCase().equals( "string" ) ) {
        logger.info( "add new string output: {}", outputId );
        strOutputNames.add( outputId );
      } else {
        throw new IllegalArgumentException(
            String.format( "FMU output data type not supported: '%1$s'", dataType )
        );
      }
    }
  }


  /**
   * Initialize the FMU simulator.
   *
   * @param fmuConfig configuration data (JSON format)
   * @param initValuesConfig FMU initial values configuration (JSON format)
   */
  protected void initFmu( JSONObject fmuConfig, JSONArray initValuesConfig ) {

    HashMap<String, Double> realInitVals = new HashMap<>();
    HashMap<String, Long> intInitVals = new HashMap<>();
    HashMap<String, Boolean> boolInitVals = new HashMap<>();
    HashMap<String, String> strInitVals = new HashMap<>();

    retrieveInitialVariableInfo( initValuesConfig,
        realInitVals, intInitVals, boolInitVals, strInitVals );

    final long defaultUpdatePeriodMillis = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_DEFAULT_UPDATE_PERIOD_TAG, 1000L );

    final Number fmuStartTime = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_MODEL_START_TIME_TAG, 0 );

    final Number fmuScaleTime = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_MODEL_SCALE_TIME_TAG, 1 );

    syncTimeSec = 0;
    nextSyncPointSec = fmuStartTime.doubleValue();
    syncTimeScaleFactor = fmuScaleTime.doubleValue();
    stepSizeSec = 1e-3 * defaultUpdatePeriodMillis;

    String fmuInstanceName = String.format( "%1$s_%2$s", this.client.getName(), this.modelName );

    fmiStatus status = fmu.instantiate( fmuInstanceName );

    if ( fmiStatus.fmiOK != status ) {
      throw new RuntimeException( "instantiation of FMU failed" );
    }

    applyInitialValues( realInitVals, intInitVals, boolInitVals, strInitVals );

    status = fmu.initialize();

    if ( fmiStatus.fmiOK != status ) {
      throw new RuntimeException( "initialization of FMU failed" );
    }
  }


  //
  // Functionality specific to this FMU simulator.
  //

  private void applyInitialValues( HashMap<String, Double> realInitVals,
      HashMap<String, Long> intInitVals,
      HashMap<String, Boolean> boolInitVals,
      HashMap<String, String> strInitVals ) {

    fmiStatus status;

    for ( Map.Entry<String, Double> entry : realInitVals.entrySet() ) {
      status = fmu.setRealValue( entry.getKey(), entry.getValue() );

      if ( fmiStatus.fmiOK != status ) {
        throw new RuntimeException( String.format( "setting initial value failed: %1$s = %2$s",
            entry.getKey(), entry.getValue() ) );
      }
    }

    for ( Map.Entry<String, Long> entry : intInitVals.entrySet() ) {
      status = fmu.setIntegerValue( entry.getKey(), entry.getValue().intValue() );

      if ( fmiStatus.fmiOK != status ) {
        throw new RuntimeException( String.format( "setting initial value failed: %1$s = %2$s",
            entry.getKey(), entry.getValue() ) );
      }
    }

    for ( Map.Entry<String, Boolean> entry : boolInitVals.entrySet() ) {
      status = fmu.setBooleanValue( entry.getKey(), entry.getValue() ? (char) 1 : (char) 0 );

      if ( fmiStatus.fmiOK != status ) {
        throw new RuntimeException( String.format( "setting initial value failed: %1$s = %2$s",
            entry.getKey(), entry.getValue() ) );
      }
    }

    for ( Map.Entry<String, String> entry : strInitVals.entrySet() ) {
      status = fmu.setStringValue( entry.getKey(), entry.getValue() );

      if ( fmiStatus.fmiOK != status ) {
        throw new RuntimeException( String.format( "setting initial value failed: %1$s = %2$s",
            entry.getKey(), entry.getValue() ) );
      }
    }
  }


  /**
   * Update the client output data services with the current FMU outputs.
   */
  private void updateInputs() {

    fmiStatus status;

    if ( realInputNames.size() > 0 ) {
      iterateSimultaneously( realInputNames, realInputs, ( String rin, Double riv ) -> {
        if ( fmiStatus.fmiOK != fmu.setRealValue( rin, riv ) ) {
          throw new RuntimeException( String.format( "setting value failed: %1$s = %2$s",
              rin, riv ) );
        }
      } );
    }

    if ( intInputNames.size() > 0 ) {
      iterateSimultaneously( intInputNames, intInputs, ( String iin, Long iiv ) -> {
        if ( fmiStatus.fmiOK != fmu.setIntegerValue( iin, iiv.intValue() ) ) {
          throw new RuntimeException( String.format( "setting value failed: %1$s = %2$s",
              iin, iiv ) );
        }
      } );
    }

    if ( boolInputNames.size() > 0 ) {
      iterateSimultaneously( boolInputNames, boolInputs, ( String bin, Character biv ) -> {
        if ( fmiStatus.fmiOK != fmu.setBooleanValue( bin, biv ) ) {
          throw new RuntimeException( String.format( "setting value failed: %1$s = %2$s",
              bin, biv ) );
        }
      } );
    }

    if ( strInputNames.size() > 0 ) {
      iterateSimultaneously( strInputNames, strInputs, ( String sin, String siv ) -> {
        if ( fmiStatus.fmiOK != fmu.setStringValue( sin, siv ) ) {
          throw new RuntimeException( String.format( "setting value failed: %1$s = %2$s",
              sin, siv ) );
        }
      } );
    }

    fmu.raiseEvent();
    fmu.handleEvents();
  }


  /**
   * Update the client output data services with the current FMU outputs.
   */
  private void updateOutputs() {

    if ( realOutputNames.size() > 0 ) {
      iterateSimultaneously( realOutputNames, realOutputServices,
          ( String ron, IImplementedService<Double> ros ) -> {
            ros.setValue( fmu.getRealValue( ron ) );
          } );
    }

    if ( intOutputNames.size() > 0 ) {
      iterateSimultaneously( intOutputNames, intOutputServices,
          ( String ion, IImplementedService<Long> ios ) -> {
            ios.setValue( (long) fmu.getIntegerValue( ion ) );
          } );
    }

    if ( boolOutputNames.size() > 0 ) {
      iterateSimultaneously( boolOutputNames, boolOutputServices,
          ( String bon, IImplementedService<Boolean> bos ) -> {
            bos.setValue( fmu.getBooleanValue( bon ) == (char) 1 ? true : false );
          } );
    }

    if ( strOutputNames.size() > 0 ) {
      iterateSimultaneously( strOutputNames, strOutputServices,
          ( String son, IImplementedService<String> sos ) -> {
            sos.setValue( fmu.getStringValue( son ) );
          } );
    }
  }
}
