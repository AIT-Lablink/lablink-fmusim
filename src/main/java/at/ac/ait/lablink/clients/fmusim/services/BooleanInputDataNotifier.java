//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim.services;

import at.ac.ait.lablink.clients.fmusim.FmuSimBase;

import at.ac.ait.lablink.core.service.IServiceStateChangeNotifier;
import at.ac.ait.lablink.core.service.LlService;


/**
 * Class BooleanInputDataNotifier.
 */
public class BooleanInputDataNotifier implements IServiceStateChangeNotifier<LlService, Boolean> {

  private final FmuSimBase fmuSim;
  private final int position;


  /**
   * Constructor.
   *
   * @param fmu associated FMU simulator instance
   * @param pos position of input variable in the client's vector of inputs
   */
  public BooleanInputDataNotifier( FmuSimBase fmu, int pos ) {
    fmuSim = fmu;
    position = pos;
  }


  /**
   * Whenever a the state of the associated data service changes (i.e., a new
   * input arrives), set the corresponding FMU input.
   */
  @Override
  public void stateChanged( LlService service, Boolean oldVal, Boolean newVal ) {
    // No need to handle synchronization for the next
    // line, because Java's Vector class handles this.
    fmuSim.setBooleanInput( newVal, position );
    
    // Notify the FMU event loop that a new input has been set.
    fmuSim.notifyEventLoop();
  }
}
