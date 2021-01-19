//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim.services;

import at.ac.ait.lablink.core.service.LlServiceBoolean;


/**
 * Class BooleanDataService.
 * 
 * <p>Data service for FMU simulator input/output data of type long.
 */
public class BooleanDataService extends LlServiceBoolean {
  /**
   * @see at.ac.ait.lablink.core.service.LlService#get()
   */
  @Override
  public Boolean get() {
    return this.getCurState();
  }

  /**
   * @see at.ac.ait.lablink.core.service.LlService#set( java.lang.Object )
   */
  @Override
  public boolean set( Boolean newVal ) {
    this.setCurState( newVal );
    return true;
  }
}