//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.clients.fmusim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URI;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Collection of helper functions for unzipping an FMU.
 */
public class FmuUnzip {

  /**
   * Unzip an FMU file.
   *
   * @param fmuUri URI to FMU.
   * @return string array with (1) the extraction directory as URI and (2) the model name.
   * @throws IOException if the file cannot be opened, if there are problems reading
   *     the zip file or if there are problems creating the files or directories
   */
  public static String[] extractFmu( URI fmuUri ) throws java.io.IOException {

    File fmuFile = new File( fmuUri );

    if ( false == fmuFile.isFile() ) {
      throw new IOException( "not a valid file: "
          + fmuUri.toString() );
    }

    String[] fileNameParts = fmuFile.getName().split( "\\.(?=[^\\.]+$)" );
    String modelName = fileNameParts[0];
    String fileExtension = fileNameParts[1];

    if ( false == fileExtension.toLowerCase().equals( "fmu" ) ) {
      throw new IOException( "not a valid FMU archive name (wrong extension): "
        + fmuUri.toString() );
    }

    File destDir = new File( fmuFile.getParent() + File.separator + modelName );

    FmuUnzip.unzip( fmuFile, destDir );

    String[] ret = { destDir.toURI().toString(), modelName };
    return ret;
  }


  /** 
   * Unzip a file.
   * Based on http://java.sun.com/developer/technicalArticles/Programming/compression/.
   *
   * @param zipFile file to be unzipped
   * @param topDirectory directory to which the ZIP archive will be extracted
   * @throws IOException if the file cannot be opened, if there are problems reading
   *     the zip file or if there are problems creating the files or directories
   */
  private static void unzip( File zipFile, File topDirectory ) throws IOException {
    BufferedOutputStream destination = null;
    final int buffer = 2048;
    byte[] data = new byte[buffer];

    FileInputStream fileInputStream = null;
    ZipInputStream zipInputStream = null;
    File destinationFile = null;
    try {
      fileInputStream = new FileInputStream( zipFile );
      zipInputStream = new ZipInputStream( new BufferedInputStream( fileInputStream ) );
      ZipEntry entry;

      while ( ( entry = zipInputStream.getNextEntry() ) != null ) {
        String entryName = entry.getName();
        destinationFile = new File(topDirectory, entryName);
        File destinationParent = destinationFile.getParentFile();

        // If the directory does not exist, create it.
        if ( !destinationParent.isDirectory() && !destinationParent.mkdirs() ) {
          throw new IOException( "Failed to create \"" + destinationParent + "\"." );
        }

        // If the entry is not a directory, then write the file.
        if (!entry.isDirectory()) {
          // Write the files to the disk.
          try {
            FileOutputStream fos = new FileOutputStream( destinationFile );
            destination = new BufferedOutputStream( fos, buffer );
            int count;
            while ( ( count = zipInputStream.read( data, 0, buffer ) ) != -1 ) {
              destination.write( data, 0, count );
            }
          } finally {
            if ( destination != null ) {
              destination.flush();
              destination.close();
              destination = null;
            }
          }
        }
      }
    } finally {
      if ( destination != null ) {
        try {
          destination.close();
        } catch ( IOException ex ) {
          //logger.warn( "unzip: failed to close {}", destinationFile );
        }
      }
      if ( zipInputStream != null ) {
        zipInputStream.close();
      }
    }
  }

}
