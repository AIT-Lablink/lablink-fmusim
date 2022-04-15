//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import at.ac.ait.lablink.clients.fmusim.services.BooleanDataService;
import at.ac.ait.lablink.clients.fmusim.services.BooleanInputDataNotifier;
import at.ac.ait.lablink.clients.fmusim.services.DoubleDataService;
import at.ac.ait.lablink.clients.fmusim.services.DoubleInputDataNotifier;
import at.ac.ait.lablink.clients.fmusim.services.LongDataService;
import at.ac.ait.lablink.clients.fmusim.services.LongInputDataNotifier;
import at.ac.ait.lablink.clients.fmusim.services.StringDataService;
import at.ac.ait.lablink.clients.fmusim.services.StringInputDataNotifier;

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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;
import java.util.function.BiConsumer;


/**
 * Class FmuSimBase.
 * This class defines interfaces and provides basic
 * functionality used by all FMU simulator clients.
 */
public abstract class FmuSimBase {

  /** Logger. */
  protected static final Logger logger = LogManager.getLogger( "FmuSim" );

  // Flags for CLI setup.
  private static final String CLI_CONF_FLAG = "c";
  private static final String CLI_CONF_LONG_FLAG = "config";
  private static final String CLI_TEST_FLAG = "w";

  // Tags for client setup.
  protected static final String CLIENT_CONFIG_TAG = "Client";
  protected static final String CLIENT_DESC_TAG = "ClientDescription";
  protected static final String CLIENT_GROUP_NAME_TAG = "GroupName";
  protected static final String CLIENT_NAME_TAG = "ClientName";
  protected static final String CLIENT_SCENARIO_NAME_TAG = "ScenarioName";
  protected static final String CLIENT_SHELL_TAG = "ClientShell";
  protected static final String CLIENT_URI_LL_PROPERTIES = "labLinkPropertiesUrl";
  protected static final String CLIENT_URI_SYNC_PROPERTIES = "syncHostPropertiesUrl";

  // Tags for general FMU simulator setup.
  protected static final String FMU_CONFIG_TAG = "FMU";
  protected static final String FMU_INPUT_CONFIG_TAG = "Input";
  protected static final String FMU_OUTPUT_CONFIG_TAG = "Output";

  // Tags for initial variable setup.
  protected static final String FMU_INITIAL_VALUES_TAG = "InitialValues";
  protected static final String FMU_INITIAL_VALUE_NAME_TAG = "VariableName";
  protected static final String FMU_INITIAL_VALUE_TYPE_TAG = "DataType";
  protected static final String FMU_INITIAL_VALUE_TAG = "Value";

  /** Flag for testing (write config and exit). */
  private static boolean writeConfigAndExitFlag;

  /** LabLink client instance. */
  protected LlClient client;

  /// FIXME This data service is just for debugging.
  //protected IImplementedService<Double> debugService;

  // Initial values for FMU variables of type real.
  protected HashMap<String, Double> realInitVals;

  // FMU inputs of type real.
  protected Vector<String> realInputNames;
  protected Vector<Double> realInputs;
  protected Vector<IImplementedService<Double>> realInputServices;

  // FMU outputs of type real.
  protected Vector<String> realOutputNames;
  protected Vector<IImplementedService<Double>> realOutputServices;

  // Initial values for FMU variables of type integer.
  protected HashMap<String, Long> intInitVals;

  // FMU inputs of type integer.
  protected Vector<String> intInputNames;
  protected Vector<Long> intInputs;
  protected Vector<IImplementedService<Long>> intInputServices;

  // FMU outputs of type integer.
  protected Vector<String> intOutputNames;
  protected Vector<IImplementedService<Long>> intOutputServices;

  // Initial values for FMU variables of type boolen.
  protected HashMap<String, Boolean> boolInitVals;

  // FMU inputs of type boolean.
  protected Vector<String> boolInputNames;
  protected Vector<Character> boolInputs;
  protected Vector<IImplementedService<Boolean>> boolInputServices;

  // FMU outputs of type boolean.
  protected Vector<String> boolOutputNames;
  protected Vector<IImplementedService<Boolean>> boolOutputServices;

  // Initial values for FMU variables of type string.
  protected HashMap<String, String> strInitVals;

  // FMU inputs of type string.
  protected Vector<String> strInputNames;
  protected Vector<String> strInputs;
  protected Vector<IImplementedService<String>> strInputServices;

  // FMU outputs of type string.
  protected Vector<String> strOutputNames;
  protected Vector<IImplementedService<String>> strOutputServices;


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
  public FmuSimBase( JSONObject jsonConfig ) throws
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

    // Load SWIG Java wrapper for FMI++ library.
    System.loadLibrary( "fmippim_wrap_java" );

    // Retrieve basic client configuration.
    JSONObject clientConfig = ConfigUtil.<JSONObject>getRequiredConfigParam( jsonConfig,
        CLIENT_CONFIG_TAG, String.format( "Client configuration (JSON object with tag '%1$s') "
        + "is missing", CLIENT_CONFIG_TAG ) );

    // Basic client configuration.
    configureClient( clientConfig );

    // Retrieve config for FMU wrapper.
    JSONObject fmuConfig = ConfigUtil.<JSONObject>getRequiredConfigParam( jsonConfig,
        FMU_CONFIG_TAG, String.format( "FMU simulator configuration (JSON object with tag "
        + "'%1$s') is missing", FMU_CONFIG_TAG ) );

    // Configure the FMU wrapper.
    configureFmu( fmuConfig );

    // Retrieve config for inputs.
    JSONArray inputConfigList = ConfigUtil.<JSONArray>getRequiredConfigParam( jsonConfig,
        FMU_INPUT_CONFIG_TAG, String.format( "FMU simulator input definition (JSON array with tag "
        + "'%1$s') is missing", FMU_INPUT_CONFIG_TAG ) );

    // Initialize vectors for input names.
    realInputNames = new Vector<String>( inputConfigList.size() );
    intInputNames = new Vector<String>( inputConfigList.size() );
    boolInputNames = new Vector<String>( inputConfigList.size() );
    strInputNames = new Vector<String>( inputConfigList.size() );

    // Initialize vectors for inputs.
    realInputs = new Vector<Double>( inputConfigList.size() );
    intInputs = new Vector<Long>( inputConfigList.size() );
    boolInputs = new Vector<Character>( inputConfigList.size() );
    strInputs = new Vector<String>( inputConfigList.size() );

    // Retrieve initial variables data.
    JSONArray initValuesConfigList = ConfigUtil.<JSONArray>getRequiredConfigParam( jsonConfig,
        FMU_INITIAL_VALUES_TAG, String.format( "FMU initial values configuration (JSON array with "
        + "tag '%1$s') is missing", FMU_INITIAL_VALUES_TAG ) );

    // Initialize maps for  initial values.
    realInitVals = new HashMap<>();
    intInitVals = new HashMap<>();
    boolInitVals = new HashMap<>();
    strInitVals = new HashMap<>();

    // Retrieve info on initial FMU variable start values.
    retrieveInitialVariableInfo( initValuesConfigList );

    // Add inputs to the client.
    configureInputs( inputConfigList );

    // Retrieve config for outputs.
    JSONArray outputConfigList = ConfigUtil.<JSONArray>getRequiredConfigParam( jsonConfig,
        FMU_OUTPUT_CONFIG_TAG, String.format( "FMU simulator output definition (JSON array with "
        + "tag '%1$s') is missing", FMU_OUTPUT_CONFIG_TAG ) );

    // Initialize vectors with output names.
    realOutputNames = new Vector<String>( outputConfigList.size() );
    intOutputNames = new Vector<String>( outputConfigList.size() );
    boolOutputNames = new Vector<String>( outputConfigList.size() );
    strOutputNames = new Vector<String>( outputConfigList.size() );

    // Add outputs to the client.
    configureOutputs( outputConfigList );

    /// FIXME This data service is just for debugging.
    //DoubleDataService debugDataService = new DoubleDataService();
    //debugDataService.setName( "debug" );
    //MqttCommInterfaceUtility.addDataPointProperties( debugDataService,
    //    "debug", "debug", "debug", "" );
    //client.addService( debugDataService );

    // Initialize the FMU.
    initFmu( fmuConfig );

    // Create the client.
    client.create();

    // Initialize the client.
    client.init();

    // Start the client.
    client.start();

    // Initialize the data services for all outputs.
    initializeAllOutputDataServices();
  }

  //
  // API for data notifiers.
  //

  /**
   * Set value of real input variable.
   *
   * @param val value of input variable
   * @param pos position of input variable in vector of inputs
   */
  public void setRealInput( Double val, int pos  ) {
    realInputs.setElementAt( val, pos );
  }


  /**
   * Set value of integer input variable.
   *
   * @param val value of input variable
   * @param pos position of input variable in vector of inputs
   */
  public void setIntegerInput( Long val, int pos  ) {
    intInputs.setElementAt( val, pos );
  }


  /**
   * Set value of boolean input variable.
   *
   * @param val value of input variable
   * @param pos position of input variable in vector of inputs
   */
  public void setBooleanInput( Boolean val, int pos  ) {
    boolInputs.setElementAt( val ? (char) 1 : (char) 0, pos );
  }


  /**
   * Set value of string input variable.
   *
   * @param val value of input variable
   * @param pos position of input variable in vector of inputs
   */
  public void setStringInput( String val, int pos  ) {
    strInputs.setElementAt( val, pos );
  }


  /**
   * This function is called by input data notifiers, to update the
   * FMU inputs and subsequently restart the FMU simulator event loop.
   */
  public abstract void notifyEventLoop();


  //
  // Interfaces to be implemented by clients.
  //

  /**
   * Start the event loop of the FMU simulator.
   */
  protected abstract void startEventLoop();


  /**
   * Configure the LabLink client data services, which serve as outputs for the FMU simulator.
   *
   * @param outputConfigList configuration data (JSON format)
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   */
  protected abstract void configureOutputs( JSONArray outputConfigList ) throws
      at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType;


  /**
   * Initialize the FMU simulator.
   *
   * @param fmuConfig configuration data (JSON format)
   */
  protected abstract void initFmu( JSONObject fmuConfig );


  /**
   * Configure the FMU simulator.
   *
   * @param fmuConfig FMU simulator configuration data (JSON format)
   * @throws java.io.IOException
   *   IO exception error
   * @throws java.net.URISyntaxException
   *   URI syntax error
   */
  protected abstract void configureFmu( JSONObject fmuConfig ) throws
      java.io.IOException,
      java.net.URISyntaxException;


  /**
   * Configure the LabLink client data services, which serve as inputs for the FMU simulator.
   *
   * @param inputConfigList configuration data (JSON format)
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   */
  protected abstract void configureInputs( JSONArray inputConfigList ) throws
      at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType;


  //
  // Functionality for configuring implemented clients.
  //

  /**
   * Configure the LabLink client.
   *
   * @param clientConfig configuration data (JSON format)
   * @throws at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException
   *   comm interface not supported
   */
  protected void configureClient( JSONObject clientConfig ) throws
      at.ac.ait.lablink.core.client.ex.CommInterfaceNotSupportedException {

    logger.info( "Basic client configuration ..." );

    // General Lablink properties configuration.
    String llPropUri = ConfigUtil.<String>getRequiredConfigParam( clientConfig,
        CLIENT_URI_LL_PROPERTIES, String.format( "LabLink client configuration URI missing "
        + "(%1$s)", CLIENT_URI_LL_PROPERTIES ) );

    // Sync properties configuration.
    String llSyncUri = ConfigUtil.<String>getRequiredConfigParam( clientConfig,
        CLIENT_URI_SYNC_PROPERTIES, String.format( "Sync host configuration URI missing "
        + "(%1$s)", CLIENT_URI_SYNC_PROPERTIES ) );

    // Scenario name.
    String scenarioName = ConfigUtil.<String>getRequiredConfigParam( clientConfig,
        CLIENT_SCENARIO_NAME_TAG, String.format( "Scenario name missing (%1$s)",
        CLIENT_SCENARIO_NAME_TAG ) );

    // Group name.
    String groupName = ConfigUtil.<String>getRequiredConfigParam( clientConfig,
        CLIENT_GROUP_NAME_TAG, String.format( "Group name missing (%1$s)",
        CLIENT_GROUP_NAME_TAG ) );

    // Client name.
    String clientName = ConfigUtil.<String>getRequiredConfigParam( clientConfig,
        CLIENT_NAME_TAG, String.format( "Client name missing (%1$s)", CLIENT_NAME_TAG ) );

    // Client description (optional).
    String clientDesc = ConfigUtil.getOptionalConfigParam( clientConfig,
        CLIENT_DESC_TAG, clientName );

    // Activate shell (optional, default: false).
    boolean giveShell = ConfigUtil.getOptionalConfigParam( clientConfig,
        CLIENT_SHELL_TAG, false );

    boolean isPseudo = false;

    // Declare the client with required interface.
    client = new LlClient( clientName,
        MqttCommInterfaceUtility.SP_ACCESS_NAME, giveShell, isPseudo );

    // Specify client configuration (no sync host).
    MqttCommInterfaceUtility.addClientProperties( client, clientDesc,
        scenarioName, groupName, clientName, llPropUri, llSyncUri, null );
  }


  /**
   * Create a new client input.
   *
   * @param inputId name of input signal
   * @param dataType type of data associated to input signal (double or long)
   * @param unit unit associated to input signal
   * @param inputVectorPos position of associated entry in vector of input variable values
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   */
  protected void addInputDataService( String inputId, String unit,
      String dataType, int inputVectorPos )
      throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType {

    // Data service description.
    String serviceDesc = String.format( "FMU input variable %1$s (%2$s)",
        inputId, dataType );

    if ( dataType.toLowerCase().equals( "double" ) ) {
      // Create new data service.
      DoubleDataService dataService = new DoubleDataService();
      dataService.setName( inputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          inputId, serviceDesc, inputId, unit );

      // Add notifier.
      dataService.addStateChangeNotifier( new DoubleInputDataNotifier( this, inputVectorPos ) );

      // Add service to the client.
      client.addService( dataService );
    } else if ( dataType.toLowerCase().equals( "long" ) ) {
      // Create new data service.
      LongDataService dataService = new LongDataService();
      dataService.setName( inputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          inputId, serviceDesc, inputId, unit );

      // Add notifier.
      dataService.addStateChangeNotifier( new LongInputDataNotifier( this, inputVectorPos ) );

      // Add service to the client.
      client.addService( dataService );
    } else if ( dataType.toLowerCase().equals( "boolean" ) ) {
      // Create new data service.
      BooleanDataService dataService = new BooleanDataService();
      dataService.setName( inputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          inputId, serviceDesc, inputId, unit );

      // Add notifier.
      dataService.addStateChangeNotifier( new BooleanInputDataNotifier( this, inputVectorPos ) );

      // Add service to the client.
      client.addService( dataService );
    } else if ( dataType.toLowerCase().equals( "string" ) ) {
      // Create new data service.
      StringDataService dataService = new StringDataService();
      dataService.setName( inputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          inputId, serviceDesc, inputId, unit );

      // Add notifier.
      dataService.addStateChangeNotifier( new StringInputDataNotifier( this, inputVectorPos ) );

      // Add service to the client.
      client.addService( dataService );
    } else {
      throw new IllegalArgumentException(
          String.format( "FMU input data type not supported: '%1$s'", dataType )
      );
    }
  }


  /**
   * Create a new client output.
   *
   * @param outputId name of output signal
   * @param dataType type of data associated to output signal (double or long)
   * @param unit unit associated to output signal
   * @throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType
   *   service type does not match client type
   */
  protected void addOutputDataService( String outputId, String dataType, String unit )
      throws at.ac.ait.lablink.core.client.ex.ServiceTypeDoesNotMatchClientType {

    // Data service description.
    String serviceDesc = String.format( "FMU output variable %1$s (%2$s)",
        outputId, dataType );

    if ( dataType.toLowerCase().equals( "double" ) ) {
      // Create new data service.
      DoubleDataService dataService = new DoubleDataService();
      dataService.setName( outputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          outputId, serviceDesc, outputId, unit );

      // Add service to the client.
      client.addService( dataService );
    } else if ( dataType.toLowerCase().equals( "long" ) ) {
      // Create new data service.
      LongDataService dataService = new LongDataService();
      dataService.setName( outputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          outputId, serviceDesc, outputId, unit );

      // Add service to the client.
      client.addService( dataService );
    } else if ( dataType.toLowerCase().equals( "boolean" ) ) {
      // Create new data service.
      BooleanDataService dataService = new BooleanDataService();
      dataService.setName( outputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          outputId, serviceDesc, outputId, unit );

      // Add service to the client.
      client.addService( dataService );
    } else if ( dataType.toLowerCase().equals( "string" ) ) {
      // Create new data service.
      StringDataService dataService = new StringDataService();
      dataService.setName( outputId );

      // Specify data service properties.
      MqttCommInterfaceUtility.addDataPointProperties( dataService,
          outputId, serviceDesc, outputId, unit );

      // Add service to the client.
      client.addService( dataService );
    } else {
      throw new IllegalArgumentException(
          String.format( "FMU output data type not supported: '%1$s'", dataType )
      );
    }
  }


  /**
   * Retrieve information for initializing variables and parameters during FMU initialization.
   *
   * @param initValuesConfigList initial values configuration data (JSON format)
   */
  protected void retrieveInitialVariableInfo( JSONArray initValuesConfigList ) {

    @SuppressWarnings( "rawtypes" )
    Iterator initValuesConfigListIter = initValuesConfigList.iterator();

    // TODO: Start values are only retrieved from client configuration,
    // but default values should be read from model description before.
    while ( initValuesConfigListIter.hasNext() ) {
      JSONObject initValueConfig = (JSONObject) initValuesConfigListIter.next();

      String varName = ConfigUtil.<String>getRequiredConfigParam(
          initValueConfig, FMU_INITIAL_VALUE_NAME_TAG,
          String.format( "variable name of initial value is missing (%1$s.%2$s)",
              FMU_INITIAL_VALUES_TAG, FMU_INITIAL_VALUE_NAME_TAG )
      );

      String dataType = ConfigUtil.<String>getRequiredConfigParam(
          initValueConfig, FMU_INITIAL_VALUE_TYPE_TAG,
          String.format( "data type of initial value is missing (%1$s.%2$s)",
              FMU_INITIAL_VALUES_TAG, FMU_INITIAL_VALUE_TYPE_TAG )
      );

      String errValueMissing = String.format( "initial value is missing (%1$s.%2$s)",
          FMU_INITIAL_VALUES_TAG, FMU_INITIAL_VALUE_TAG );

      if ( dataType.toLowerCase().equals( "double" ) ) {
        // JSON makes no difference between integers and integer-valued doubles.
        // Hence, use Number instead of Double for retrieving these parameters.
        Number initVal = ConfigUtil.<Number>getRequiredConfigParam( initValueConfig,
            FMU_INITIAL_VALUE_TAG, errValueMissing );
        realInitVals.put( varName, initVal.doubleValue() );
        logger.info( "found initial value: {} = {} ({})", varName, initVal, dataType );
      } else if ( dataType.toLowerCase().equals( "long" ) ) {
        Long initVal = ConfigUtil.<Long>getRequiredConfigParam( initValueConfig,
            FMU_INITIAL_VALUE_TAG, errValueMissing );
        intInitVals.put( varName, initVal );
        logger.info( "found initial value: {} = {} ({})", varName, initVal, dataType );
      } else if ( dataType.toLowerCase().equals( "boolean" ) ) {
        Boolean initVal = ConfigUtil.<Boolean>getRequiredConfigParam( initValueConfig,
            FMU_INITIAL_VALUE_TAG, errValueMissing );
        boolInitVals.put( varName, initVal );
        logger.info( "found initial value: {} = {} ({})", varName, initVal, dataType );
      } else if ( dataType.toLowerCase().equals( "string" ) ) {
        String initVal = ConfigUtil.<String>getRequiredConfigParam( initValueConfig,
            FMU_INITIAL_VALUE_TAG, errValueMissing );
        strInitVals.put( varName, initVal );
        logger.info( "found initial value: {} = {} ({})", varName, initVal, dataType );
      } else {
        throw new IllegalArgumentException(
            String.format( "data type not supported: '%1$s'", dataType )
        );
      }
    }
  }


  /**
   * Parse the command line arguments to retrieve the configuration.
   *
   * @param args arguments to main method
   * @return configuration data (JSON format)
   * @throws org.apache.commons.cli.ParseException
   *   parse exception
   * @throws org.apache.commons.configuration.ConfigurationException
   *   configuration error
   * @throws org.json.simple.parser.ParseException
   *   parse error
   * @throws java.io.IOException
   *   IO error
   * @throws java.net.MalformedURLException
   *   malformed URL
   * @throws java.util.NoSuchElementException
   *   no such element
   */
  protected static JSONObject getConfig( String[] args ) throws
      org.apache.commons.cli.ParseException,
      org.apache.commons.configuration.ConfigurationException,
      org.json.simple.parser.ParseException,
      java.io.IOException,
      java.net.MalformedURLException,
      java.util.NoSuchElementException {

    // Define command line option.
    Options cliOptions = new Options();
    cliOptions.addOption( CLI_CONF_FLAG, CLI_CONF_LONG_FLAG, true, "FMU sim configuration URI" );
    cliOptions.addOption( CLI_TEST_FLAG, false, "write config and exit" );

    // Parse command line options.
    CommandLineParser parser = new BasicParser();
    CommandLine commandLine = parser.parse( cliOptions, args );

    // Set flag for testing (write config and exit).
    writeConfigAndExitFlag = commandLine.hasOption( CLI_TEST_FLAG );

    // Retrieve configuration URI from command line.
    String configUri = commandLine.getOptionValue( CLI_CONF_FLAG );

    // Get configuration URL, resolve environment variables if necessary.
    URL fullConfigUrl = new URL( Utility.parseWithEnvironmentVariable( configUri ) );

    // Read configuration, remove existing comments.
    Scanner scanner = new Scanner( fullConfigUrl.openStream() );
    String rawConfig = scanner.useDelimiter( "\\Z" ).next();
    rawConfig = rawConfig.replaceAll( "#.*#", "" );

    // Check if comments have been removed properly.
    int still = rawConfig.length() - rawConfig.replace( "#", "" ).length();
    if ( still > 0 ) {
      throw new IllegalArgumentException(
          String.format( "Config file contains at least %1$d line(s) with incorrectly"
              + "started/terminated comments: %2$s", still, fullConfigUrl.toString() )
        );
    }

    logger.info( "Parsing configuration file..." );

    // Parse configuration (JSON format).
    JSONParser jsonParser = new JSONParser();
    JSONObject jsonConfig = ( JSONObject ) jsonParser.parse( rawConfig );

    return jsonConfig;
  }


  /**
   * This function initializes all of the client's output data services.
   */
  private void initializeAllOutputDataServices() {
    realOutputServices = this.<Double>initializeOutputDataServices( realOutputNames );
    intOutputServices = this.<Long>initializeOutputDataServices( intOutputNames );
    boolOutputServices = this.<Boolean>initializeOutputDataServices( boolOutputNames );
    strOutputServices = this.<String>initializeOutputDataServices( strOutputNames );

    /// FIXME This data service is just for debugging.
    //debugService =
    //    ( IImplementedService<Double> ) this.client.getImplementedServices().get( "debug" );
  }


  /**
   * Helper function: initialize an output data service.
   *
   * @param outputNames vector containing names of output data service
   * @param <T> data type of output data service
   * @return vector of output data service instances
   */
  @SuppressWarnings( "unchecked" )
  private <T> Vector<IImplementedService<T>>
      initializeOutputDataServices( Vector<String> outputNames ) {

    Vector<IImplementedService<T>> outputServices =
        new Vector<IImplementedService<T>>( outputNames.size() );

    for ( String id : outputNames ) {
      outputServices.add(
          ( IImplementedService<T> ) this.client.getImplementedServices().get( id )
      );
    }

    return outputServices;
  }


  //
  // Functionality for testing.
  //

  /**
   * Retrieve value of flag {@code writeConfigAndExitFlag} (used for testing).
   *
   * @return value of flag {@code writeConfigAndExitFlag}
   */
  public static boolean getWriteConfigAndExitFlag() {
    return writeConfigAndExitFlag;
  }


  /**
   * Returns the yellow pages info (JSON format) of the Lablink client.
   *
   * @return yellow pages
   */
  public String getYellowPageJson() {
    return this.client.getYellowPageJson();
  }


  /**
   * Retrieve the URI of the FMU.
   *
   * @param rawFmuUri URI to FMU as parsed from the configuration (string)
   * @return URI to FMU (URI)
   * @throws java.io.IOException
   *   IO exception
   * @throws java.net.URISyntaxException
   *   URI syntax exception
   */
  protected URI getFmuFileUri( String rawFmuUri ) throws
      java.io.IOException,
      java.net.URISyntaxException {

    URI checkUri = new URI( rawFmuUri );
    File checkFile;

    if ( checkUri.getScheme().equals( "fmusim" ) ) {
      String strFmuDir = System.getProperty( "fmuDir" );
      if ( strFmuDir == null ) {
        strFmuDir = System.getProperty( "user.dir" );
      }
      checkFile = new File( strFmuDir, checkUri.getSchemeSpecificPart() );
    } else {
      checkFile = new File( checkUri );
    }

    if ( checkFile.isFile() == false ) {
      throw new IOException(
        String.format( "Not a valid file: '%1$s'", checkFile.getPath() )
        );
    }

    return checkFile.toURI();
  }


  //
  // Helper Functions.
  //

  /**
   * Helper function: iterate simultaneously over two containers and perform an action
   * on the entries.
   *
   * @param <T1> c1 data type
   * @param <T2> c2 data type
   * @param c1 iterable container
   * @param c2 iterable container
   * @param consumer instance of class BiConsumer that performs an action on the container entries
   */
  protected <T1, T2> void iterateSimultaneously(
      Iterable<T1> c1, Iterable<T2> c2, BiConsumer<T1, T2> consumer ) {
    Iterator<T1> i1 = c1.iterator();
    Iterator<T2> i2 = c2.iterator();
    while ( i1.hasNext() && i2.hasNext() ) {
      consumer.accept( i1.next(), i2.next() );
    }
  }

}
