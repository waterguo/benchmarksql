/*
 * ExecJDBC - Command line program to process SQL DDL statements, from   
 *             a text input file, to any JDBC Data Source
 *
 * Copyright (C) 2004-2013, Denis Lussier
 *
 */

import org.apache.log4j.*;

import java.io.*;
import java.sql.*;
import java.util.*;


public class ExecJDBC {

  static Logger log = Logger.getLogger(ExecJDBC.class);
  
  public static void main(String[] args) {

    PropertyConfigurator.configure("log4j.xml");
    log.info("Starting BenchmarkSQL ExecJDBC");

    Connection conn = null;
    Statement stmt = null;
    String rLine = null;
    StringBuffer sql = new StringBuffer();

    try {

    Properties ini = new Properties();
    ini.load( new FileInputStream(System.getProperty("prop")));
                                                                                
    // Register jdbcDriver
    Class.forName(ini.getProperty( "driver" ));

    // make connection
    conn = DriverManager.getConnection(ini.getProperty("conn"),
      ini.getProperty("user"),ini.getProperty("password"));
    conn.setAutoCommit(true); 
                                                                                
    // Create Statement
    stmt = conn.createStatement();
                                                                                
      // Open inputFile
      BufferedReader in = new BufferedReader
        (new FileReader(jTPCCUtil.getSysProp("commandFile",null)));
  
      // loop thru input file and concatenate SQL statement fragments
      while((rLine = in.readLine()) != null) {

         String line = rLine.trim();

         if (line.length() == 0) {
           log.info("");
         } else {
           if (line.startsWith("--")) {
              log.info(line);  // print comment line
           } else {
               sql.append(line);
               if (line.endsWith(";")) {
                  execJDBC(stmt, sql);
                  sql = new StringBuffer();
               } else {
                 sql.append("\n");
               }
           }

         } //end if 
        
      } //end while

      in.close();

    } catch(IOException ie) {
        log.info(ie.getMessage());
    
    } catch(SQLException se) {
        log.info(se.getMessage());

    } catch(Exception e) {
        e.printStackTrace();

    //exit Cleanly
    } finally {
      try {
        if (conn !=null)
           conn.close();
      } catch(SQLException se) {
        se.printStackTrace();
      } // end finally

    } // end try


    log.info("");
                  

  } // end main


  static void execJDBC(Statement stmt, StringBuffer sql) {

    log.info(sql);

    try {

      stmt.execute(sql.toString().replace(';',' '));
    
    }catch(SQLException se) {
      log.error(se.getMessage());
    } // end try

  } // end execJDBCCommand

} // end ExecJDBC Class
