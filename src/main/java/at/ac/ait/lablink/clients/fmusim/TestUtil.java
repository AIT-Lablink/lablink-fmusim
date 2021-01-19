//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * Collection of helper functions for testing.
 */
public class TestUtil {
  

  /**
   * Run a test (write config and exit).
   *
   * @param sim FMU simulator
   */
  public static void writeConfigAndExit( FmuSimBase sim ) {

    String clientConfig = sim.getYellowPageJson();

    try {
      Files.write( Paths.get( "client_config.json" ), clientConfig.getBytes() );
    } catch ( IOException ex ) {
      System.exit( 1 );
    }

    System.exit( 0 );
  } 
  
}