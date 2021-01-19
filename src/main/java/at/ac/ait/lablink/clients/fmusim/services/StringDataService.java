//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim.services;

import at.ac.ait.lablink.core.service.LlServiceString;


/**
 * Class StringDataService.
 * 
 * <p>Data service for FMU simulator input/output data of type long.
 */
public class StringDataService extends LlServiceString {
  /**
   * @see at.ac.ait.lablink.core.service.LlService#get()
   */
  @Override
  public String get() {
    return this.getCurState();
  }

  /**
   * @see at.ac.ait.lablink.core.service.LlService#set( java.lang.Object )
   */
  @Override
  public boolean set( String newVal ) {
    this.setCurState( newVal );
    return true;
  }
}