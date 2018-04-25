*********************************************************************
Change Log:

Version 3.0.9 2013-03-21 denisl
   - Fix runLoader.sh to work with new log4j

Version 3.0.8 2013-03-20 denisl
   - Config log4j for rotating log files once per minute
   - Default flat file location to '/tmp/csv/' in
     table copies script


Version 3.0.6 2013-02-05 denisl
   - Drop incomplete & untested Windoze '.bat' scripts
   - Standardize logging with log4j
   - Improve Logging with meaningful DEBUG and INFO levels
   - Simplify "build.xml" to eliminate nbproject dependency
   - Defaults read in from propeerties
   - Groudwork laid to eliminate the GUI
   - Default GUI console to PostgreSQL and 10 Warehouses

Version 2.3.5  2013-01-29 denisl
   - Cleanup the formatting & content of README.txt

Version 2.3.4  2013-01-29 denisl
   - Default build is now with JDK 1.6 and JDBC 4 Postgres 9.2 driver
   - Remove outdated JDBC 3 drivers (for JDK 1.5).  You can run as 
     before by a JDBC4 driver from any supported vendor.
   - Remove ExecJDBC warning about trying to rollback when in 
     autocommit mode
   - Remove the extraneous COMMIT statements from the DDL scripts 
     since ExecJDBC runs in autocommit mode
   - Fix the version number displayed in the console

Version 2.3.3  2010-11-19 sjm  
   - Added DB2 LUW V9.7 support, and supercedes patch 2983892
   - No other changes from 2.3.2

*********************************************************************


Instructions for running
------------------------
Use of JDK6 or JDK7 is required.   Sample JDBC Connection Property 
files are provided as follows:
  props.pg  : for PostgreSQL/EnterpriseDB
  props.db2 : for DB2 LUW
  props.ora : for Oracle

1. Go to the 'run' directory, edit the appropriate "props.???" 
   file to point to the database instance you'd like to test.   

2. Run the "sqlTableCreates" to create the base tables.

        $ ./runSQL.sh props.pg sqlTableCreates


3. Run the Loader command file to load all of the default data 
   for a benchmark:


  A.) Approximately half a million rows (per Warehouse) will be loaded 
      across 9 tables.  

        $ ./runLoader.sh props.pg numWarehouses 1

      NOTE: You should run the sqlTableTruncates scripts if your tables
            are not already empty.
      
  B.) Alternatively, for PostgreSQL & DB2, you may choose to generate the 
      load data out to CSV files where it can be efficiently be 
      bulk loaded into the database as many times as required by your 
      testing.

      $ ./runLoader.sh props.pg numWarehouses 1 fileLocation /tmp/csv/   
        
      These CSV files can be bulk loaded as follows:
        $  ./runSQL.sh props.pg sqlTableCopies

      You may truncate the data via:

        $  ./runSQL.h props.pg sqlTableTruncates

4. Run the "runSQL" command file to execute the SQL script 
   "sqlIndexCreates" to create the primary keys & other indexes 
   on the tables.

        $  ./runSQL.sh props.pg sqlIndexCreates


5. Run the "runBenchmark" command file to execute the swing GUI 
   application to test the database.  Don't forget to set the number of 
   warehouses equal to the number you created in step 3. For each run, a 
   report will be placed in run/reports.  A sample report is included.

       $  ./runBenchmark.sh props.pg

6. Operational Notes to minimize problems:  
   (a) executing runBenchmark will start the GUI. 

       Click the Database button to view properties file settings. No 
         changes are needed if the properties settings are correct.

       Click the Terminals button and specify desired settings. 
         Specify the same number of warehouses as you created.
         Select either "Minutes" or "Transactions per terminal" 
         and blank out the other setting.

       Click the Weights button and specify desired settings

       Click the Controls button, then click Create Terminals. One 
       DB connection per Terminal is created. 

       Click Start Transactions to start the benchmark.

   (b) If changing the number of terminals between runs, it is best 
       to close the GUI window and re-execute runBenchmark .

   (c) If the benchmark runs properly, all database connections are 
       terminated at completion. You may need to manually
       terminate connections if this is not the case

   (d) When done, close the GUI window 




Instructions for re-building from source
----------------------------------------

Use of JDK 1.6 & ANT 1.8 is recommended.  Build with the "ant" 
command from the base directory. 

