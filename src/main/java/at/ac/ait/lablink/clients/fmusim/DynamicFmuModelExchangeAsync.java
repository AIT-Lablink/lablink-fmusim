//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import at.ac.ait.fmipp.imp.IncrementalFMU;
import at.ac.ait.fmipp.imp.IntegratorType;
import at.ac.ait.fmipp.imp.SWIGTYPE_p_double;
import at.ac.ait.fmipp.imp.SWIGTYPE_p_int;
import at.ac.ait.fmipp.imp.SWIGTYPE_p_std__string;
import at.ac.ait.fmipp.imp.fmippim;

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
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


/**
 * Class DynamicFmuModelExchangeAsync.
 *
 * <p>This client simulates FMUs for Model Exchange and synchronizes them periodically to real
 * time. At each synchronization point the client updates its output ports according to the
 * selected FMU model ouput variables. In case an (internal) event is detected during the
 * integration of the FMU model, the periodic synchronization schedule is adapted such that
 * the output ports are updated at the corresponding event time. New inputs to the client are
 * treated as (external) events and the integration of the FMU model and the synchronization
 * schedule is adapted accordingly.
 */
public class DynamicFmuModelExchangeAsync extends FmuSimBase implements Runnable {

  // Tags for FMU simulator configuration.
  protected static final String FMU_LOGGING_TAG = "LoggingOn";
  protected static final String FMU_TIME_DIFF_RES_TAG = "TimeDiffResolution_s";
  protected static final String FMU_INTEGRATOR_TYPE_TAG = "IntegratorType";
  protected static final String FMU_URI_TAG = "URI";
  protected static final String FMU_DEFAULT_UPDATE_PERIOD_TAG = "DefaulUpdatePeriod_ms";
  protected static final String FMU_NUM_STEPS_TAG = "NSteps";
  protected static final String FMU_NUM_INTEGRATOR_STEPS_TAG = "NIntegratorSteps";
  protected static final String FMU_MODEL_START_TIME_TAG = "ModelStartTime_s";

  // Tags for input configuration.
  protected static final String INPUT_DATATYPE_TAG = "DataType";
  protected static final String INPUT_ID_TAG = "VariableName";
  protected static final String INPUT_UNIT_TAG = "Unit";

  // Tags for output configuration.
  protected static final String OUTPUT_DATATYPE_TAG = "DataType";
  protected static final String OUTPUT_ID_TAG = "VariableName";
  protected static final String OUTPUT_UNIT_TAG = "Unit";

  /** Reference timestamp, represent the starting point of the simulation in real time. */
  private long startTime;

  /**
   * This instance of class IncrementalFMU (from the FMI++ Library) implements
   * the actual FMU simulator. It allows to include an FMU for Model Exchange in
   * a discrete event-based system.
   */
  private IncrementalFMU fmu;

  /** Name of the FMU model. */
  private String modelName;

  /** Default period between synchronization points (in nanoseconds). */
  private long defaultUpdatePeriodNs;

  /** Current synchronization time (logical simulation time in seconds). */
  private double syncTimeSec;

  /** Next predicted synchronization time (logical simulation time in seconds). */
  private double nextSyncTimeSec;

  /** Resolution for resolving time differences (in seconds). */
  private double timeDiffResSec;

  /** Timestamp of the last call to {@link run} or {@link notifyEventLoop} (in nanoseconds). 
    */
  private long lastSyncTimeNs;

  /** Overall initial delay for synchronization schedule (in nanoseconds). */
  private long delayNs;

  /** Delay for synchronization schedule due to FMU synchronization (in nanoseconds). */
  private long syncDelayNs;

  /** Extra delay for synchronization schedule due to system overhead (in nanoseconds). */
  private long driftDelayNs;

  /** This flag signals if a new synchronization schedule needs to scheduled by
   * the thread pool executor service.
   */
  private boolean rescheduleExecutorFlag;

  /** Handle for cancelling synchronization schedules (which run in a separate thread). */
  private ScheduledFuture<?> futureTask;

  /** Lock used for preventing racing conditions between threads 
   *  of the main event loop and the input data notifiers.
   */
  private final Object syncLock = new Object();

  /** Thread pool executor service for synchronization schedules. */
  private static final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor();

  private Vector<Double> realOutputs;
  private Vector<Long> intOutputs;
  private Vector<Character> boolOutputs;
  private Vector<String> strOutputs;

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
    DynamicFmuModelExchangeAsync fmuClient = new DynamicFmuModelExchangeAsync( jsonConfig );

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
  public DynamicFmuModelExchangeAsync( JSONObject jsonConfig ) throws
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

    // This lock prevents the input data notifiers to call "notifyEventLoop" at the same time.
    synchronized ( syncLock ) {
      // Store current timestamp, will be used to compute the time it
      // took to calculate how long it took to synchronize the FMU.
      lastSyncTimeNs = System.nanoTime();

      // Synchronize the FMU (incl. update of client outputs).
      syncTimeSec = nextSyncTimeSec;
      nextSyncTimeSec = syncFmu( syncTimeSec, false );

      // Check if the synchronization schedule of the FMU has to be adapted,
      // i.e., check if the thread executor has to reschedule this task.
      rescheduleExecutorFlag = rescheduleExecutorFlag
          || ( nextSyncTimeSec - syncTimeSec 
              <= 1e-9 * getdefaultUpdatePeriodNs() - timeDiffResSec );

      // Reschedule the current FMU synchronization schedule.
      if ( rescheduleExecutorFlag ) {

        // Cancel the current FMU synchronization schedule.
        futureTask.cancel( true );

        // Compute execution delay due to the time it took to synchronize the FMU.
        syncDelayNs = System.nanoTime() - lastSyncTimeNs;

        // Check if there is a "drift" in the FMU synchronization schedule that should be corrected.
        driftDelayNs = getElapsedTimeInNanos() - Math.round( 1e9 * syncTimeSec );

        // Compute the delay with which the new FMU synchronization schedule should be started.
        if ( driftDelayNs >= 10000000 ) {
          // The "drift" of the FMU synchronization schedule has
          // reached a critical value and requires a correction.
          delayNs = Math.round( 1e9 * nextSyncTimeSec - 1e9 * syncTimeSec )
              - syncDelayNs - driftDelayNs;
        } else {
          delayNs = Math.round( 1e9 * nextSyncTimeSec - 1e9 * syncTimeSec )
              - syncDelayNs;
        }

        // Start a new FMU synchronization schedule.
        futureTask = executor.scheduleAtFixedRate( this,
            delayNs, getdefaultUpdatePeriodNs(), TimeUnit.NANOSECONDS );

        // In case the FMU synchronization schedule does not allow to
        // compensate for all computational and execution delays, try
        // to compensate the delay after the next synchronization step.
        // This happens, when the FMU update step (nextSyncTimeSec - t) is smaller
        // than the sum of accumulated delays.
        rescheduleExecutorFlag = ( delayNs > 0 ) ? false : true;

        if ( true == rescheduleExecutorFlag ) {
          logger.warn( "Delay compensation failed in this synchronization "
              + "step, will try again in next step. Consider to change the "
              + "default FMU update period in case this happens frequently." );
        }
      }

      /// FIXME This data service is just for debugging.
      //double now = getElapsedTimeInSeconds();
      //debugService.setValue( now - syncTimeSec );
    }
  }

  //
  // Implementation of abstract methods specified in class FmuSimBase
  //

  /**
   * Start the event loop of the FMU simulator.
   */
  protected void startEventLoop() {

    // Initialize the FMU synchronization.
    nextSyncTimeSec = syncFmu( syncTimeSec, false );

    // Set the current timestamp as starting point
    // for the time-synchronized updates of the FMU.
    setStartTimeToNow();

    // Start a new FMU synchronization schedule.
    futureTask = executor.scheduleAtFixedRate( this,
        getdefaultUpdatePeriodNs(), getdefaultUpdatePeriodNs(), TimeUnit.NANOSECONDS );

    rescheduleExecutorFlag = false;
  }


  /**
   * This function is called by input data notifiers, to update the
   * FMU inputs and subsequently restart the FMU simulator event loop.
   */
  public void notifyEventLoop() {

    // This lock prevents the main event loop thread to call "run" at the same time.
    synchronized ( syncLock ) {
      // Store current timestamp, will be used to compute the time it
      // took to calculate how long it took to synchronize the FMU.
      lastSyncTimeNs = System.nanoTime();

      // Synchronize the FMU, incl. update of client inputs AND outputs.
      syncTimeSec = getElapsedTimeInSeconds();
      nextSyncTimeSec = syncFmu( syncTimeSec, true );

      // Cancel the current FMU synchronization schedule.
      futureTask.cancel( true );

      // Compute the delay with which the new FMU synchronization schedule should be started.
      delayNs = Math.round( 1e9 * nextSyncTimeSec ) - getElapsedTimeInNanos()
          + lastSyncTimeNs - System.nanoTime();

      // Start a new FMU synchronization schedule.
      futureTask = executor.scheduleAtFixedRate( this,
          delayNs, getdefaultUpdatePeriodNs(), TimeUnit.NANOSECONDS );

      // In case the FMU synchronization schedule does not allow to
      // compensate for all computational and execution delays, try
      // to compensate the delay after the next synchronization step.
      // This happens, when the FMU update step (nextSyncTimeSec - t) is smaller
      // than the sum of accumulated delays.
      rescheduleExecutorFlag = ( delayNs > 0 ) ? false : true;

      if ( true == rescheduleExecutorFlag ) {
        logger.warn( "Delay compensation failed in this synchronization "
            + "step, will try again in next step. Consider to change the "
            + "default FMU update period in case this happens frequently." );
      }

      /// FIXME This data service is just for debugging.
      //double now = getElapsedTimeInSeconds();
      //debugService.setValue( now - syncTimeSec );
    }
  }


  /**
   * Instances of class DynamicFmuModelExchangeAsync store a reference timestamp, which
   * represents the simulation start time. This function sets this reference timestamp
   * to the current system time.
   */
  private void setStartTimeToNow() {
    this.startTime = System.nanoTime();
  }


  /**
   * Instances of class DynamicFmuModelExchangeAsync store a reference timestamp, which
   * represents the simulation start time. This function returns the time difference
   * between the current system time and the reference timestamp in seconds.
   *
   * @return time difference between the current system time and the reference timestamp
   *     in seconds
   */
  private double getElapsedTimeInSeconds() {
    return ( System.nanoTime() - this.startTime ) * 1e-9;
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

    String[] fmuUriAndModelName = FmuUnzip.extractFmu( new URI( rawFmuUri ) );

    char fmuLogging = logging ? (char)1 : (char)0;

    IntegratorType fmuIntegratorType = IntegratorUtil.getIntegratorType( strIntegratorType );

    this.modelName = fmuUriAndModelName[1];

    this.fmu = new IncrementalFMU( fmuUriAndModelName[0], fmuUriAndModelName[1],
        fmuLogging, timeDiffResSec, fmuIntegratorType );
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

    this.fmu.defineRealInputs( SwigTypeUtil.convertToSwigArray( realInputNames ),
        realInputNames.size() );
    this.fmu.defineIntegerInputs( SwigTypeUtil.convertToSwigArray( intInputNames ),
        intInputNames.size() );
    this.fmu.defineBooleanInputs( SwigTypeUtil.convertToSwigArray( boolInputNames ),
        boolInputNames.size() );
    this.fmu.defineStringInputs( SwigTypeUtil.convertToSwigArray( strInputNames ),
        strInputNames.size() );
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

    // Initialize vectors with outputs.
    realOutputs = new Vector<Double>( outputConfigList.size() );
    intOutputs = new Vector<Long>( outputConfigList.size() );
    boolOutputs = new Vector<Character>( outputConfigList.size() );
    strOutputs = new Vector<String>( outputConfigList.size() );

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
        realOutputs.add( 0. ); // TODO set to initial value
      } else if ( dataType.toLowerCase().equals( "long" ) ) {
        logger.info( "add new long output: {}", outputId );
        intOutputNames.add( outputId );
        intOutputs.add( 0L ); // TODO: set to initial value
      } else if ( dataType.toLowerCase().equals( "boolean" ) ) {
        logger.info( "add new boolean output: {}", outputId );
        boolOutputNames.add( outputId );
        boolOutputs.add( (char) 0 ); // TODO: set to initial value
      } else if ( dataType.toLowerCase().equals( "string" ) ) {
        logger.info( "add new string output: {}", outputId );
        strOutputNames.add( outputId );
        strOutputs.add( "" ); // TODO: set to initial value
      } else {
        throw new IllegalArgumentException(
            String.format( "FMU output data type not supported: '%1$s'", dataType )
        );
      }
    }

    this.fmu.defineRealOutputs( SwigTypeUtil.convertToSwigArray( realOutputNames ),
        realOutputNames.size() );
    this.fmu.defineIntegerOutputs( SwigTypeUtil.convertToSwigArray( intOutputNames ),
        intOutputNames.size() );
    this.fmu.defineBooleanOutputs( SwigTypeUtil.convertToSwigArray( boolOutputNames ),
        boolOutputNames.size() );
    this.fmu.defineStringOutputs( SwigTypeUtil.convertToSwigArray( strOutputNames ),
        strOutputNames.size() );
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

    SWIGTYPE_p_std__string realInitVarNames = fmippim.new_string_array( realInitVals.size() );
    SWIGTYPE_p_double realInitVarValues = fmippim.new_double_array( realInitVals.size() );

    SWIGTYPE_p_std__string intInitVarNames = fmippim.new_string_array( intInitVals.size() );
    SWIGTYPE_p_int intInitVarValues = fmippim.new_int_array( intInitVals.size() );

    SWIGTYPE_p_std__string boolInitVarNames = fmippim.new_string_array( boolInitVals.size() );
    String boolInitVarValues = fmippim.new_char_array( boolInitVals.size() );

    SWIGTYPE_p_std__string strInitVarNames = fmippim.new_string_array( strInitVals.size() );
    SWIGTYPE_p_std__string strInitVarValues = fmippim.new_string_array( strInitVals.size() );

    SwigTypeUtil.copyVariableData(
        realInitVals, realInitVarNames, realInitVarValues,
        intInitVals, intInitVarNames, intInitVarValues,
        boolInitVals, boolInitVarNames, boolInitVarValues,
        strInitVals, strInitVarNames, strInitVarValues );

    final long defaultUpdatePeriodMillis = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_DEFAULT_UPDATE_PERIOD_TAG, 1000L );

    final long numSteps = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_NUM_STEPS_TAG, 2L );

    final long numIntSteps = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_NUM_INTEGRATOR_STEPS_TAG, 2L );

    final Number fmuStartTime = ConfigUtil.getOptionalConfigParam( fmuConfig,
        FMU_MODEL_START_TIME_TAG, 0 );

    syncTimeSec = fmuStartTime.doubleValue();
    nextSyncTimeSec = fmuStartTime.doubleValue();
    defaultUpdatePeriodNs = defaultUpdatePeriodMillis * 1000000;

    final double horizon = 1e-3 * defaultUpdatePeriodMillis; // Convert to seconds.
    final double stepSize = horizon / numSteps;
    final double intStepSize = stepSize / numIntSteps;

    String fmuInstanceName = this.client.getName() + this.modelName;

    int status = this.fmu.init( fmuInstanceName,
        realInitVarNames, realInitVarValues, realInitVals.size(),
        intInitVarNames, intInitVarValues, intInitVals.size(),
        boolInitVarNames, boolInitVarValues, boolInitVals.size(),
        strInitVarNames, strInitVarValues, strInitVals.size(),
        fmuStartTime.doubleValue(), horizon, stepSize, intStepSize );

    if ( 1 != status ) {
      throw new RuntimeException( "initialization of FMU failed" );
    }
  }


  //
  // Functionality specific to this FMU simulator.
  //

  /**
   * Instances of class DynamicFmuModelExchangeAsync store a reference timestamp, which
   * represents the simulation start time. This function returns the time difference
   * between the current system time and the reference timestamp in nanoseconds.
   *
   * @return time difference between the current system time and the reference timestamp
   *     in nanoseconds
   */
  private long getElapsedTimeInNanos() {
    return ( System.nanoTime() - this.startTime );
  }


  /**
   * Instances of class DynamicFmuModelExchangeAsync typically update their outputs
   * periodically (except an internal/external event occurs). This function returns
   * the value of this default update period (a.k.a. the integrator horizon) in
   * milliseconds.
   *
   * @return default FMU ouput update period in milliseconds
   */
  private long getDefaultUpdatePeriodMillis() {
    return Math.round( 1e-6 * defaultUpdatePeriodNs );
  }


  /**
   * Instances of class DynamicFmuModelExchangeAsync typically update their outputs
   * periodically (except an internal/external event occurs). This function returns
   * the value of this default update period (a.k.a. the integrator horizon) in
   * nanoseconds.
   *
   * @return default FMU ouput update period in nanoseconds
   */
  private long getdefaultUpdatePeriodNs() {
    return defaultUpdatePeriodNs;
  }


  /**
   * Synchronize the FMU state to the (logical) time t1, update the outputs of the
   * FMU (and the client), update the state of the FMU with the latest inputs (if
   * available) and predict the timestamp of the next synchronization point (time
   * of internal event or according to default update period).
   *
   * @param t1 (logical) timestamp in seconds to which the FMU should be synchronized
   * @param inputsAvailable (logical) set to true if new inputs are available for the FMU
   * @return (logical) timestamp of the next synchronization point in seconds
   */
  private double syncFmu( double t1, boolean inputsAvailable ) {

    // Update the state of the FMU to the requested synchronization time.
    if ( t1 != fmu.updateState( t1 ) ) {
      // Throw an exception in case the update failed.
      throw new RuntimeException(
          "Failed to update FMU state to time t = " + Double.toString( t1 )
      );
    }

    // Update the outputs of the client.
    updateOutputs();

    if ( true == inputsAvailable ) {
      // Copy the current values of the client's double-valued inputs.
      int intEntry = 0;
      SWIGTYPE_p_double realInputVarValues = fmippim.new_double_array( realInputs.size() );
      for ( Double entry : realInputs ) {
        fmippim.double_array_setitem( realInputVarValues, intEntry, entry );
        ++intEntry;
      }

      // Copy the current values of the client's integer-valued inputs.
      intEntry = 0;
      SWIGTYPE_p_int intInputVarValues = fmippim.new_int_array( intInputs.size() );
      for ( Long entry : intInputs ) {
        fmippim.int_array_setitem( intInputVarValues, intEntry, entry.intValue() );
        ++intEntry;
      }

      // Copy the current values of the client's boolean-valued inputs.
      intEntry = 0;
      String boolInputVarValues = fmippim.new_char_array( boolInputs.size() );
      for ( Character entry : boolInputs ) {
        fmippim.char_array_setitem( boolInputVarValues, intEntry, entry );
        ++intEntry;
      }

      // Copy the current values of the client's string-valued inputs.
      intEntry = 0;
      SWIGTYPE_p_std__string strInputVarValues = fmippim.new_string_array( strInputs.size() );
      for ( String entry : strInputs ) {
        fmippim.string_array_setitem( strInputVarValues, intEntry, entry );
        ++intEntry;
      }

      // Set the new inputs before making a prediction.
      fmu.syncState( t1, realInputVarValues, intInputVarValues,
          boolInputVarValues, strInputVarValues );
    }

    // Predict the future state (but make no update yet!), return time for next update.
    double t2 = fmu.predictState( t1 );
    return t2;
  }


  /**
   * Update the client output data services with the current FMU outputs.
   */
  private void updateOutputs() {

    if ( realOutputs.size() > 0 ) {
      SWIGTYPE_p_double realFmuOutputs = fmu.getRealOutputs();
      for ( int pos = 0; pos < realOutputs.size(); ++pos ) {
        realOutputs.setElementAt( fmippim.double_array_getitem( realFmuOutputs, pos ), pos );
      }

      iterateSimultaneously( realOutputs, realOutputServices,
          ( Double od, IImplementedService<Double> sd ) -> {
            sd.setValue( od );
          } );
    }

    if ( intOutputs.size() > 0 ) {
      SWIGTYPE_p_int intFmuOutputs = fmu.getIntegerOutputs();
      for ( int pos = 0; pos < intOutputs.size(); ++pos ) {
        intOutputs.setElementAt( (long) fmippim.int_array_getitem( intFmuOutputs, pos ), pos );
      }

      iterateSimultaneously( intOutputs, intOutputServices,
          ( Long ol, IImplementedService<Long> sl ) -> {
            sl.setValue( ol );
          } );
    }

    if ( boolOutputs.size() > 0 ) {
      String boolFmuOutputs = fmu.getBooleanOutputs();
      for ( int pos = 0; pos < boolOutputs.size(); ++pos ) {
        boolOutputs.setElementAt( fmippim.char_array_getitem( boolFmuOutputs, pos ), pos );
      }

      iterateSimultaneously( boolOutputs, boolOutputServices,
          ( Character ob, IImplementedService<Boolean> sb ) -> {
            sb.setValue( ( (char) ob ) == '1' ? true : false );
          } );
    }

    if ( strOutputs.size() > 0 ) {
      SWIGTYPE_p_std__string strFmuOutputs = fmu.getStringOutputs();
      for ( int pos = 0; pos < strOutputs.size(); ++pos ) {
        strOutputs.setElementAt( fmippim.string_array_getitem( strFmuOutputs, pos ), pos );
      }

      iterateSimultaneously( strOutputs, strOutputServices,
          ( String os, IImplementedService<String> ss ) -> {
            ss.setValue( os );
          } );
    }

  }
}
