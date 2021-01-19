//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class InputDataNotifierBase.
 */
public class InputDataNotifierBase {

  protected static final Logger logger = LogManager.getLogger( "InputDataNotifier" );


  public static final class AccessToken { 
    private AccessToken() {} 
  }
  
  protected static final AccessToken access = new AccessToken();
}
