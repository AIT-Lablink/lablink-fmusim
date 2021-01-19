//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import at.ac.ait.fmipp.imp.IntegratorType;


/**
 * Collection of helper functions related to the FMI++ integrator.
 */
public class IntegratorUtil {

  /**
   * Convert string to FMI++ integrator type .
   *
   * @param type integrator type as string (case insensitive)
   * @return FMI++ integrator type
   * @throws IllegalArgumentException in case the supplied string does not correspond 
   *     to any known FMI++ integrator type
   */
  public static IntegratorType getIntegratorType( String type ) throws IllegalArgumentException {
    if ( type.toLowerCase().equals( "eu" ) ) {
      return IntegratorType.eu;
    } else if ( type.toLowerCase().equals( "rk" ) ) {
      return IntegratorType.rk;
    } else if ( type.toLowerCase().equals( "abm" ) ) {
      return IntegratorType.abm;
    } else if ( type.toLowerCase().equals( "ck" ) ) {
      return IntegratorType.ck;
    } else if ( type.toLowerCase().equals( "dp" ) ) {
      return IntegratorType.dp;
    } else if ( type.toLowerCase().equals( "fe" ) ) {
      return IntegratorType.fe;
    } else if ( type.toLowerCase().equals( "bs" ) ) {
      return IntegratorType.bs;
    } else if ( type.toLowerCase().equals( "ro" ) ) {
      return IntegratorType.ro;
    } else if ( type.toLowerCase().equals( "bdf" ) ) {
      return IntegratorType.bdf;
    } else if ( type.toLowerCase().equals( "abm2" ) ) {
      return IntegratorType.abm2;
    } else {
      throw new IllegalArgumentException(
          String.format( "Integrator type not supported: '%1$s'", type )
      );
    }
  }

}
