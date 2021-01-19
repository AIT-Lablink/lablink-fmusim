//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import at.ac.ait.fmipp.imp.SWIGTYPE_p_double;
import at.ac.ait.fmipp.imp.SWIGTYPE_p_int;
import at.ac.ait.fmipp.imp.SWIGTYPE_p_std__string;
import at.ac.ait.fmipp.imp.fmippim;


import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


/**
 * Collection of helper functions for handling SWIG data types (FMI++ Java wrapper).
 */
public class SwigTypeUtil {
  
  /**
   * Copy contents of vector of strings to SWIG string array.
   *
   * @param vec vector of strings
   * @return SWIG string array
   */
  public static SWIGTYPE_p_std__string convertToSwigArray( Vector<String> vec ) {
    // Create SWIG string array with correct size.
    SWIGTYPE_p_std__string arr = fmippim.new_string_array( vec.size() );

    // Iterate through vector elements and insert them into SWIG string array.
    int pos = 0;
    for ( String str : vec ) {
      fmippim.string_array_setitem( arr, pos, str );
      ++pos;
    }

    return arr;
  }
  
  
  /**
   * Copy data from SWIG arrays to maps. Do this for all four data types supported by FMI.
   *
   * @param realVals map filled with content from realVarNames (keys) and realVarValues (values)
   * @param realVarNames keys for map realVals
   * @param realVarValues values for map realVals
   * @param intVals map filled with content from intVarNames (keys) and intVarValues (values)
   * @param intVarNames keys for map intVals
   * @param intVarValues values for map intVals
   * @param boolVals map filled with content from boolVarNames (keys) and boolVarValues (values)
   * @param boolVarNames keys for map boolVals
   * @param boolVarValues values for map boolVals
   * @param strVals map filled with content from strVarNames (keys) and strVarValues (values)
   * @param strVarNames keys for map strVals
   * @param strVarValues values for map strVals
   */
  public static void copyVariableData( HashMap<String, Double> realVals,
      SWIGTYPE_p_std__string realVarNames, SWIGTYPE_p_double realVarValues,
      HashMap<String, Long> intVals, SWIGTYPE_p_std__string intVarNames,
      SWIGTYPE_p_int intVarValues, HashMap<String, Boolean> boolVals,
      SWIGTYPE_p_std__string boolVarNames, String boolVarValues,
      HashMap<String, String> strVals, SWIGTYPE_p_std__string strVarNames,
      SWIGTYPE_p_std__string strVarValues ) {

    int intEntry = 0;
    for ( Map.Entry<String, Double> entry : realVals.entrySet() ) {
      fmippim.string_array_setitem( realVarNames, intEntry, entry.getKey() );
      fmippim.double_array_setitem( realVarValues, intEntry, entry.getValue() );
      ++intEntry;
    }

    intEntry = 0;
    for ( Map.Entry<String, Long> entry : intVals.entrySet() ) {
      fmippim.string_array_setitem( intVarNames, intEntry, entry.getKey() );
      fmippim.int_array_setitem( intVarValues, intEntry, entry.getValue().intValue() );
      ++intEntry;
    }

    intEntry = 0;
    for ( Map.Entry<String, Boolean> entry : boolVals.entrySet() ) {
      fmippim.string_array_setitem( boolVarNames, intEntry, entry.getKey() );
      fmippim.char_array_setitem( boolVarValues, intEntry,
          entry.getValue() ? (char) 1 : (char) 0 );
      ++intEntry;
    }

    intEntry = 0;
    for ( Map.Entry<String, String> entry : strVals.entrySet() ) {
      fmippim.string_array_setitem( strVarNames, intEntry, entry.getKey() );
      fmippim.string_array_setitem( strVarValues, intEntry, entry.getValue() );
      ++intEntry;
    }
  }

}