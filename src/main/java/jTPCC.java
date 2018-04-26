/*
 * jTPCC - Open Source Java implementation of a TPC-C like benchmark
 *
 * Copyright (C) 2003, Raul Barbosa
 * Copyright (C) 2004-2013, Denis Lussier
 *
 */

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;   
  
public class jTPCC extends JFrame implements jTPCCConfig, ActionListener, WindowListener
{
    private static org.apache.log4j.Logger log = Logger.getLogger(jTPCC.class);

    private JTabbedPane jTabbedPane;
    private JPanel jPanelControl, jPanelConfigSwitch, jPanelOutputSwitch;
    private JButton jButtonNextTerminal, jButtonPreviousTerminal;
    private JOutputArea jOutputAreaControl;
    private JLabel jLabelInformation;
    private ImageIcon imageIconDot;
    private int currentlyDisplayedTerminal;

    private JButton jButtonSelectDatabase, jButtonSelectTerminals, jButtonSelectControls, jButtonSelectWeights;
    private JPanel jPanelConfigDatabase, jPanelConfigTerminals, jPanelConfigControls, jPanelConfigWeights;
    private Border jPanelConfigSwitchBorder;

    private JTextField jTextFieldDatabase, jTextFieldUsername, jTextFieldPassword, jTextFieldDriver, jTextFieldNumTerminals, jTextFieldTransactionsPerTerminal, jTextFieldNumWarehouses, jTextFieldMinutes;
    private JButton jButtonCreateTerminals, jButtonStartTransactions, jButtonStopTransactions;
    private JTextField paymentWeight, orderStatusWeight, deliveryWeight, stockLevelWeight;
    private JRadioButton jRadioButtonTime, jRadioButtonNum;

    private jTPCCTerminal[] terminals;
    private JOutputArea[] terminalOutputAreas;
    private String[] terminalNames;
    private boolean terminalsBlockingExit = false;
    private Random random;
    private long terminalsStarted = 0, sessionCount = 0, transactionCount;

    private long newOrderCounter, sessionStartTimestamp, sessionEndTimestamp, sessionNextTimestamp=0, sessionNextKounter=0;
    private long sessionEndTargetTime = -1, fastNewOrderCounter, recentTpmC=0;
    private boolean signalTerminalsRequestEndSent = false, databaseDriverLoaded = false;

    private FileOutputStream fileOutputStream;
    private PrintStream printStreamReport;
    private String sessionStart, sessionEnd;

    private double tpmC;

    public static void main(String args[]) {
        if (System.getProperty("prop") == null) {
            System.out.println("runBenchmark.sh <property file>");
            return;
        }
        DOMConfigurator.configure("log4j.xml");
        log.info("Starting BenchmarkSQL jTPCC");
        new jTPCC();
    }

    private String getProp (Properties p, String pName) {
        String prop =  p.getProperty(pName);
        log.info(pName + "=" + prop);
        return(prop);
    }


    public jTPCC()
    {
        super("BenchmarkSQL v" + JTPCCVERSION);

        // load the ini file
        Properties ini = new Properties();
        try {
          ini.load( new FileInputStream(System.getProperty("prop")));
        } catch (IOException e) {
          log.error("could not load properties file");
        }
                                                                               
        log.info("");
        log.info("+-------------------------------------------------------------+");
        log.info("              BenchmarkSQL v" + JTPCCVERSION);
        log.info("+-------------------------------------------------------------+");
        log.info(" (c) 2003, Raul Barbosa");
        log.info(" (c) 2004-2013, Denis Lussier");
        log.info("+-------------------------------------------------------------+");
        log.info("");
        String  iDriver             = getProp(ini,"driver");
        String  iConn               = getProp(ini,"conn");
        String  iUser               = getProp(ini,"user");
        String  iPassword           = getProp(ini,"password");
        String  iWarehouses         = getProp(ini,"warehouses");
        String  iTerminals          = getProp(ini,"terminals");
        String  iPaymentWeight      = getProp(ini,"paymentWeight");
        String  iOrderStatusWeight  = getProp(ini,"orderStatusWeight");
        String  iDeliveryWeight     = getProp(ini,"deliveryWeight");
        String  iStockLevelWeight   = getProp(ini,"stockLevelWeight");
        String  iRunTxnsPerTerminal = getProp(ini,"runTxnsPerTerminal");
        String  iRunMins            = getProp(ini,"runMins");
        String  sRunMinsBool        = getProp(ini,"runMinsBool");
        log.info("");

        boolean iRunMinsBool = false;
        if (sRunMinsBool.equals("true")) iRunMinsBool = true;
                                                                               
        this.random = new Random(System.currentTimeMillis());
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setSize(800, 680);
        this.setLocation(112, 30);
        this.setIconImage((new ImageIcon("images/icon.gif")).getImage());
        this.addWindowListener((WindowListener)this);

        imageIconDot = new ImageIcon("images/dot.gif");

        jPanelControl = new JPanel();
        jPanelControl.setLayout(new BorderLayout());
        JPanel jPanelConfig = new JPanel();
        jPanelConfig.setLayout(new BorderLayout());
        jPanelControl.add(jPanelConfig, BorderLayout.NORTH);


        JPanel jPanelConfigSelect = new JPanel();
        jPanelConfigSelect.setBorder(BorderFactory.createTitledBorder
          (BorderFactory.createEtchedBorder(), " Options ", TitledBorder.CENTER, TitledBorder.TOP));
        GridLayout jPanelConfigSelectLayout = new GridLayout(4, 1);
        jPanelConfigSelectLayout.setVgap(10);
        jPanelConfigSelect.setLayout(jPanelConfigSelectLayout);
        jButtonSelectDatabase = new JButton("Database");
        jButtonSelectDatabase.addActionListener(this);
        jPanelConfigSelect.add(jButtonSelectDatabase);
        jButtonSelectTerminals = new JButton("Terminals");
        jButtonSelectTerminals.addActionListener(this);
        jPanelConfigSelect.add(jButtonSelectTerminals);
        jButtonSelectWeights = new JButton("Weights");
        jButtonSelectWeights.addActionListener(this);
        jPanelConfigSelect.add(jButtonSelectWeights);
        jButtonSelectControls = new JButton("Controls");
        jButtonSelectControls.addActionListener(this);
        jPanelConfigSelect.add(jButtonSelectControls);
        jPanelConfig.add(jPanelConfigSelect, BorderLayout.WEST);


        jPanelConfigSwitch = new JPanel();
        jPanelConfig.add(jPanelConfigSwitch, BorderLayout.CENTER);
        jPanelConfigSwitchBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.CENTER, TitledBorder.TOP);
        jPanelConfigSwitch.setBorder(jPanelConfigSwitchBorder);


        jPanelConfigDatabase = new JPanel();
        jPanelConfigDatabase.setLayout(new GridLayout(3, 1));
        JPanel jPanelConfigDatabase3 = new JPanel();
        jPanelConfigDatabase.add(jPanelConfigDatabase3);
        jPanelConfigDatabase3.add(new JLabel("URL"));

        jTextFieldDatabase = new JTextField(iConn);
        jTextFieldDatabase.setPreferredSize(new Dimension(325, (int)jTextFieldDatabase.getPreferredSize().getHeight()));
        jPanelConfigDatabase3.add(jTextFieldDatabase);

        jPanelConfigDatabase3.add(new JLabel("      Driver"));
        jTextFieldDriver = new JTextField(iDriver);
        jTextFieldDriver.setPreferredSize(new Dimension(175, (int)jTextFieldDriver.getPreferredSize().getHeight()));
        jPanelConfigDatabase3.add(jTextFieldDriver);

        jPanelConfigDatabase.add(new JPanel());
        JPanel jPanelConfigDatabase2 = new JPanel();
        jPanelConfigDatabase.add(jPanelConfigDatabase2);
        jPanelConfigDatabase2.add(new JLabel("Username"));
        jTextFieldUsername = new JTextField(iUser);
        jTextFieldUsername.setPreferredSize(new Dimension(100, (int)jTextFieldUsername.getPreferredSize().getHeight()));
        jPanelConfigDatabase2.add(jTextFieldUsername);
        jPanelConfigDatabase2.add(new JLabel("      Password"));
        jTextFieldPassword = new JPasswordField(iPassword);
        jTextFieldPassword.setPreferredSize(new Dimension(100, (int)jTextFieldPassword.getPreferredSize().getHeight()));
        jPanelConfigDatabase2.add(jTextFieldPassword);


        jPanelConfigTerminals = new JPanel();
        jPanelConfigTerminals.setLayout(new GridLayout(2, 1));
        JPanel jPanelConfigTerminals1 = new JPanel();
        jPanelConfigTerminals.add(jPanelConfigTerminals1);
        jPanelConfigTerminals1.add(new JLabel("Number of Terminals"));
        jTextFieldNumTerminals = new JTextField(iTerminals);
        jTextFieldNumTerminals.setPreferredSize(new Dimension(40, (int)jTextFieldNumTerminals.getPreferredSize().getHeight()));
        jPanelConfigTerminals1.add(jTextFieldNumTerminals);
        jPanelConfigTerminals1.add(new JLabel("       Warehouses"));
        jTextFieldNumWarehouses = new JTextField(iWarehouses);
        jTextFieldNumWarehouses.setPreferredSize(new Dimension(40, (int)jTextFieldNumWarehouses.getPreferredSize().getHeight()));
        jPanelConfigTerminals1.add(jTextFieldNumWarehouses);
        JPanel jPanelConfigTerminals2 = new JPanel();
        jPanelConfigTerminals.add(jPanelConfigTerminals2);
        jPanelConfigTerminals2.add(new JLabel("Execute"));
        JPanel jPanelConfigTerminals21 = new JPanel();
        jPanelConfigTerminals21.setLayout(new GridLayout(2, 1));
        jPanelConfigTerminals2.add(jPanelConfigTerminals21);
        jTextFieldMinutes = new JTextField(iRunMins);
        jTextFieldMinutes.setPreferredSize(new Dimension(40, (int)jTextFieldMinutes.getPreferredSize().getHeight()));
        jPanelConfigTerminals21.add(jTextFieldMinutes);
        jTextFieldTransactionsPerTerminal = new JTextField(iRunTxnsPerTerminal);
        jTextFieldTransactionsPerTerminal.setPreferredSize(new Dimension(40, (int)jTextFieldTransactionsPerTerminal.getPreferredSize().getHeight()));
        jPanelConfigTerminals21.add(jTextFieldTransactionsPerTerminal);
        JPanel jPanelConfigTerminals22 = new JPanel();
        jPanelConfigTerminals22.setLayout(new GridLayout(2, 1));
        jPanelConfigTerminals2.add(jPanelConfigTerminals22);
        ButtonGroup buttonGroupTimeNum = new ButtonGroup();
        jRadioButtonTime = new JRadioButton("Minutes", iRunMinsBool);
        buttonGroupTimeNum.add(jRadioButtonTime);
        jPanelConfigTerminals22.add(jRadioButtonTime);
        jRadioButtonNum = new JRadioButton("Transactions per terminal", !iRunMinsBool);
        buttonGroupTimeNum.add(jRadioButtonNum);
        jPanelConfigTerminals22.add(jRadioButtonNum);


        jPanelConfigWeights = new JPanel();
        jPanelConfigWeights.setLayout(new GridLayout(2, 1));
        jPanelConfigWeights.add(new JPanel());
        JPanel jPanelConfigWeights1 = new JPanel();
        jPanelConfigWeights.add(jPanelConfigWeights1);
        jPanelConfigWeights1.add(new JLabel("Payment %"));
        paymentWeight = new JTextField(iPaymentWeight);
        paymentWeight.setPreferredSize(new Dimension(40, (int)paymentWeight.getPreferredSize().getHeight()));
        jPanelConfigWeights1.add(paymentWeight);
        jPanelConfigWeights1.add(new JLabel("       Order-Status %"));
        orderStatusWeight = new JTextField(iOrderStatusWeight);
        orderStatusWeight.setPreferredSize(new Dimension(40, (int)orderStatusWeight.getPreferredSize().getHeight()));
        jPanelConfigWeights1.add(orderStatusWeight);
        jPanelConfigWeights1.add(new JLabel("       Delivery %"));
        deliveryWeight = new JTextField(iDeliveryWeight);
        deliveryWeight.setPreferredSize(new Dimension(40, (int)deliveryWeight.getPreferredSize().getHeight()));
        jPanelConfigWeights1.add(deliveryWeight);
        jPanelConfigWeights1.add(new JLabel("       Stock-Level %"));
        stockLevelWeight = new JTextField(iStockLevelWeight);
        stockLevelWeight.setPreferredSize(new Dimension(40, (int)stockLevelWeight.getPreferredSize().getHeight()));
        jPanelConfigWeights1.add(stockLevelWeight);


        jPanelConfigControls = new JPanel();
        jPanelConfigControls.setLayout(new GridLayout(2, 1));
        jPanelConfigControls.add(new JPanel());
        JPanel jPanelConfigControls1 = new JPanel();
        jPanelConfigControls.add(jPanelConfigControls1);
        jButtonCreateTerminals = new JButton("Create Terminals");
        jButtonCreateTerminals.setEnabled(true);
        jButtonCreateTerminals.addActionListener(this);
        jPanelConfigControls1.add(jButtonCreateTerminals);
        jButtonStartTransactions = new JButton("Start Transactions");
        jButtonStartTransactions.setEnabled(false);
        jButtonStartTransactions.addActionListener(this);
        jPanelConfigControls1.add(jButtonStartTransactions);
        jButtonStopTransactions = new JButton("Stop Transactions");
        jButtonStopTransactions.setEnabled(false);
        jButtonStopTransactions.addActionListener(this);
        jPanelConfigControls1.add(jButtonStopTransactions);


        setActiveConfigPanel(jPanelConfigDatabase, "Database");


        jOutputAreaControl = new JOutputArea();

        jPanelControl.add(jOutputAreaControl, BorderLayout.CENTER);
        jLabelInformation = new JLabel("", JLabel.CENTER);
        updateInformationLabel();


        jTabbedPane = new JTabbedPane();
        jTabbedPane.addTab("Control", imageIconDot, jPanelControl);


        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(jTabbedPane, BorderLayout.CENTER);
        this.getContentPane().add(jLabelInformation, BorderLayout.SOUTH);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
        updateInformationLabel();


        if(e.getSource() == jButtonCreateTerminals)
        {
            stopInputAreas();
            fastNewOrderCounter = 0;


            try
            {
                String driver = jTextFieldDriver.getText();
                printMessage("Loading database driver: \'" + driver + "\'...");
                Class.forName(driver);
                databaseDriverLoaded = true;
            }
            catch(Exception ex)
            {
                errorMessage("Unable to load the database driver!");
                databaseDriverLoaded = false;
                restartInputAreas();
            }


            if(databaseDriverLoaded)
            {
                try
                {
                    boolean limitIsTime = jRadioButtonTime.isSelected();
                    int numTerminals = -1, transactionsPerTerminal = -1, numWarehouses = -1;
                    int paymentWeightValue = -1, orderStatusWeightValue = -1, deliveryWeightValue = -1, stockLevelWeightValue = -1;
                    long executionTimeMillis = -1;

                    try
                    {
                        numWarehouses = Integer.parseInt(jTextFieldNumWarehouses.getText());
                        if(numWarehouses <= 0)
                            throw new NumberFormatException();
                    }
                    catch(NumberFormatException e1)
                    {
                        errorMessage("Invalid number of warehouses!");
                        throw new Exception();
                    }

                    try
                    {
                        numTerminals = Integer.parseInt(jTextFieldNumTerminals.getText());
                        if(numTerminals <= 0 || numTerminals > 10*numWarehouses)
                            throw new NumberFormatException();
                    }
                    catch(NumberFormatException e1)
                    {
                        errorMessage("Invalid number of terminals!");
                        throw new Exception();
                    }

                    if(limitIsTime)
                    {
                        try
                        {
                            executionTimeMillis = Long.parseLong(jTextFieldMinutes.getText()) * 60000;
                            if(executionTimeMillis <= 0)
                                throw new NumberFormatException();
                        }
                        catch(NumberFormatException e1)
                        {
                            errorMessage("Invalid number of minutes!");
                            throw new Exception();
                        }
                    }
                    else
                    {
                        try
                        {
                            transactionsPerTerminal = Integer.parseInt(jTextFieldTransactionsPerTerminal.getText());
                            if(transactionsPerTerminal <= 0)
                                throw new NumberFormatException();
                        }
                        catch(NumberFormatException e1)
                        {
                            errorMessage("Invalid number of transactions per terminal!");
                            throw new Exception();
                        }
                    }

                    try
                    {
                        paymentWeightValue = Integer.parseInt(paymentWeight.getText());
                        orderStatusWeightValue = Integer.parseInt(orderStatusWeight.getText());
                        deliveryWeightValue = Integer.parseInt(deliveryWeight.getText());
                        stockLevelWeightValue = Integer.parseInt(stockLevelWeight.getText());

                        if(paymentWeightValue < 0 || orderStatusWeightValue < 0 || deliveryWeightValue < 0 || stockLevelWeightValue < 0)
                            throw new NumberFormatException();
                    }
                    catch(NumberFormatException e1)
                    {
                        errorMessage("Invalid number in mix percentage!");
                        throw new Exception();
                    }

                    if(paymentWeightValue + orderStatusWeightValue + deliveryWeightValue + stockLevelWeightValue > 100)
                    {
                        errorMessage("Sum of mix percentage parameters exceeds 100%!");
                        throw new Exception();
                    }

                    newOrderCounter = 0;
                    printMessage("Session #" + (++sessionCount) + " started!");
                    if(!limitIsTime)
                        printMessage("Creating " + numTerminals + " terminal(s) with " + transactionsPerTerminal + " transaction(s) per terminal...");
                    else
                        printMessage("Creating " + numTerminals + " terminal(s) with " + (executionTimeMillis/60000) + " minute(s) of execution...");
                    printMessage("Transaction Weights: " + (100 - (paymentWeightValue + orderStatusWeightValue + deliveryWeightValue + stockLevelWeightValue)) + "% New-Order, " + paymentWeightValue + "% Payment, " + orderStatusWeightValue + "% Order-Status, " + deliveryWeightValue + "% Delivery, " + stockLevelWeightValue + "% Stock-Level");

                    log.info("Number of Terminals\t" + numTerminals);
                    log.info("\nTerminal\tHome Warehouse");

                    terminals = new jTPCCTerminal[numTerminals];
                    terminalOutputAreas = new JOutputArea[numTerminals];
                    terminalNames = new String[numTerminals];
                    terminalsStarted = numTerminals;
                    try
                    {
                        String database = jTextFieldDatabase.getText();
                        String username = jTextFieldUsername.getText();
                        String password = jTextFieldPassword.getText();

                        int[][] usedTerminals = new int[numWarehouses][10];
                        for(int i = 0; i < numWarehouses; i++)
                            for(int j = 0; j < 10; j++)
                                usedTerminals[i][j] = 0;

                        for(int i = 0; i < numTerminals; i++)
                        {
                            int terminalWarehouseID;
                            int terminalDistrictID;
                            do
                            {
                                terminalWarehouseID = (int)randomNumber(1, numWarehouses);
                                terminalDistrictID = (int)randomNumber(1, 10);
                            }
                            while(usedTerminals[terminalWarehouseID-1][terminalDistrictID-1] == 1);
                            usedTerminals[terminalWarehouseID-1][terminalDistrictID-1] = 1;

                            String terminalName = "Term-" + (i>=9 ? ""+(i+1) : "0"+(i+1));
                            Connection conn = null;
                            printMessage("Creating database connection for " + terminalName + "...");
                            conn = DriverManager.getConnection(database, username, password);
                            conn.setAutoCommit(false);

                            JOutputArea terminalOutputArea = new JOutputArea();
                            long maxChars = 150000/numTerminals;
                            if(maxChars > JOutputArea.DEFAULT_MAX_CHARS) maxChars = JOutputArea.DEFAULT_MAX_CHARS;
                            if(maxChars < 2000) maxChars = 2000;
                            terminalOutputArea.setMaxChars(maxChars);

                            jTPCCTerminal terminal = new jTPCCTerminal
                              (terminalName, terminalWarehouseID, terminalDistrictID, conn, 
                               transactionsPerTerminal, paymentWeightValue, orderStatusWeightValue, 
                               deliveryWeightValue, stockLevelWeightValue, numWarehouses, this);

                            terminals[i] = terminal;
                            terminalOutputAreas[i] = terminalOutputArea;
                            terminalNames[i] = terminalName;
                            log.info(terminalName + "\t" + terminalWarehouseID);
                        }

                        sessionEndTargetTime = executionTimeMillis;
                        signalTerminalsRequestEndSent = false;

                        log.info
                          ("\nTransaction\tWeight\n% New-Order\t" + 
                           (100 - (paymentWeightValue + orderStatusWeightValue + deliveryWeightValue + stockLevelWeightValue)) + 
                           "\n% Payment\t" + paymentWeightValue + "\n% Order-Status\t" + orderStatusWeightValue + 
                           "\n% Delivery\t" + deliveryWeightValue + "\n% Stock-Level\t" + stockLevelWeightValue);

                        log.info("\n\nTransaction Number\tTerminal\tType\tExecution Time (ms)\t\tComment");

                        printMessage("Created " + numTerminals + " terminal(s) successfully!");
                    }
                    catch(Exception e1)
                    {
                        log.error("\nThis session ended with errors!", e1);
                            printStreamReport.close();
                            fileOutputStream.close();

                        throw new Exception();
                    }

                    jButtonStartTransactions.setEnabled(true);
                }
                catch(Exception ex)
                {
                    restartInputAreas();
                }
            }
        }


        if(e.getSource() == jButtonStartTransactions)
        {
            jButtonStartTransactions.setEnabled(false);

            sessionStart = getCurrentTime();
            sessionStartTimestamp = System.currentTimeMillis();
            sessionNextTimestamp = sessionStartTimestamp;
            if(sessionEndTargetTime != -1)
                sessionEndTargetTime += sessionStartTimestamp;

            synchronized(terminals)
            {
                printMessage("Starting all terminals...");
                transactionCount = 1;
                for(int i = 0; i < terminals.length; i++)
                    (new Thread(terminals[i])).start();
            }

            printMessage("All terminals started executing " + sessionStart);
            jButtonStopTransactions.setEnabled(true);
        }


        if(e.getSource() == jButtonStopTransactions)
        {
            signalTerminalsRequestEnd(false);
        }


        if(e.getSource() == jButtonSelectDatabase) setActiveConfigPanel(jPanelConfigDatabase, "Database");
        if(e.getSource() == jButtonSelectTerminals) setActiveConfigPanel(jPanelConfigTerminals, "Terminals");
        if(e.getSource() == jButtonSelectControls) setActiveConfigPanel(jPanelConfigControls, "Controls");
        if(e.getSource() == jButtonSelectWeights) setActiveConfigPanel(jPanelConfigWeights, "Transaction Weights");

        updateInformationLabel();
    }

    private void signalTerminalsRequestEnd(boolean timeTriggered)
    {
        jButtonStopTransactions.setEnabled(false);
        synchronized(terminals)
        {
            if(!signalTerminalsRequestEndSent)
            {
                if(timeTriggered)
                    printMessage("The time limit has been reached.");
                printMessage("Signalling all terminals to stop...");
                signalTerminalsRequestEndSent = true;

                for(int i = 0; i < terminals.length; i++)
                    if(terminals[i] != null)
                        terminals[i].stopRunningWhenPossible();

                printMessage("Waiting for all active transactions to end...");
            }
        }
    }

    public void signalTerminalEnded(jTPCCTerminal terminal, long countNewOrdersExecuted)
    {
        synchronized(terminals)
        {
            boolean found = false;
            terminalsStarted--;
            for(int i = 0; i < terminals.length && !found; i++)
            {
                if(terminals[i] == terminal)
                {
                    terminals[i] = null;
                    terminalNames[i] = "(" + terminalNames[i] + ")";
                    newOrderCounter += countNewOrdersExecuted;
                    found = true;
                }
            }
        }

        if(terminalsStarted == 0)
        {
            jButtonStopTransactions.setEnabled(false);
            sessionEnd = getCurrentTime();
            sessionEndTimestamp = System.currentTimeMillis();
            sessionEndTargetTime = -1;
            printMessage("All terminals finished executing " + sessionEnd);
            endReport();
            terminalsBlockingExit = false;
            printMessage("Session #" + sessionCount + " finished!");
            restartInputAreas();
        }
    }

    public void signalTerminalEndedTransaction(String terminalName, String transactionType, long executionTime, String comment, int newOrder)
    {
        if(comment == null) comment = "None";

        log.info("" + transactionCount + "\t" + terminalName + "\t" + transactionType + "\t" + executionTime + "\t\t" + comment);
        transactionCount++;
        fastNewOrderCounter += newOrder;

        if(sessionEndTargetTime != -1 && System.currentTimeMillis() > sessionEndTargetTime)
        {
            signalTerminalsRequestEnd(true);
        }

        updateInformationLabel();
    }

    private void endReport() {
        log.info("");
        log.info("Measured tpmC     = " + tpmC);
        log.info("Session Start     = " + sessionStart );
        log.info("Session End       = " + sessionEnd);
        log.info("Transaction Count = " + (transactionCount-1));
    }

    private void setActiveConfigPanel(JPanel panel, String title)
    {
        jPanelControl.invalidate();
        jPanelConfigSwitch.invalidate();
        jPanelConfigSwitch.removeAll();
        jPanelConfigSwitch.add(panel);
        ((TitledBorder)jPanelConfigSwitch.getBorder()).setTitle(" "+title+" ");
        jPanelControl.validate();
        jPanelConfigSwitch.validate();
        jPanelControl.repaint();
        jPanelConfigSwitch.repaint();
    }

    private void printMessage(String message)
    {
        log.info(message);
    }

    private void errorMessage(String message)
    {
        log.error(message);
    }

    private void exit() {
        System.exit(0);
    }


    private void stopInputAreas()
    {
        terminalsBlockingExit = true;
        jButtonCreateTerminals.setEnabled(false);
        jTextFieldTransactionsPerTerminal.setEnabled(false);
        jTextFieldMinutes.setEnabled(false);
        jRadioButtonTime.setEnabled(false);
        jRadioButtonNum.setEnabled(false);
        jTextFieldNumTerminals.setEnabled(false);
        jTextFieldNumWarehouses.setEnabled(false);
        jTextFieldDatabase.setEnabled(false);
        jTextFieldUsername.setEnabled(false);
        jTextFieldPassword.setEnabled(false);
        jTextFieldDriver.setEnabled(false);
        paymentWeight.setEnabled(false);
        orderStatusWeight.setEnabled(false);
        deliveryWeight.setEnabled(false);
        stockLevelWeight.setEnabled(false);
        this.repaint();
    }

    private void restartInputAreas()
    {
        terminalsBlockingExit = false;
        jButtonCreateTerminals.setEnabled(true);
        jTextFieldTransactionsPerTerminal.setEnabled(true);
        jTextFieldMinutes.setEnabled(true);
        jRadioButtonTime.setEnabled(true);
        jRadioButtonNum.setEnabled(true);
        jTextFieldNumTerminals.setEnabled(true);
        jTextFieldNumWarehouses.setEnabled(true);
        jTextFieldDatabase.setEnabled(true);
        jTextFieldUsername.setEnabled(true);
        jTextFieldPassword.setEnabled(true);
        jTextFieldDriver.setEnabled(true);
        jButtonStopTransactions.setEnabled(false);
        paymentWeight.setEnabled(true);
        orderStatusWeight.setEnabled(true);
        deliveryWeight.setEnabled(true);
        stockLevelWeight.setEnabled(true);
        this.repaint();
    }

    private void updateInformationLabel()
    {
        String informativeText = "";
        long currTimeMillis = System.currentTimeMillis();

        if(fastNewOrderCounter != 0)
        {
            tpmC = (6000000*fastNewOrderCounter/(currTimeMillis - sessionStartTimestamp))/100.0;
            informativeText = "Running Average tpmC: " + tpmC + "      ";
        }

        if(currTimeMillis > sessionNextTimestamp) 
        {
            sessionNextTimestamp += 5000;  /* check this every 5 seconds */
            recentTpmC = (fastNewOrderCounter - sessionNextKounter) * 12; 
            sessionNextKounter = fastNewOrderCounter;
        }

        if(fastNewOrderCounter != 0)
        {
            informativeText += "Current tpmC: " + recentTpmC + "     ";
        }

        long freeMem = Runtime.getRuntime().freeMemory() / (1024*1024);
        long totalMem = Runtime.getRuntime().totalMemory() / (1024*1024);
        informativeText += "Memory Usage: " + (totalMem - freeMem) + "MB / " + totalMem + "MB";

        synchronized(jLabelInformation)
        {
            jLabelInformation.setText(informativeText);
            jLabelInformation.repaint();
        }
    }

    private long randomNumber(long min, long max)
    {
        return (long)(random.nextDouble() * (max-min+1) + min);
    }

    private String getCurrentTime()
    {
        return dateFormat.format(new java.util.Date());
    }

    private String getFileNameSuffix()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(new java.util.Date());
    }

    public void windowClosing(WindowEvent e)
    {
        exit();
    }

    public void windowOpened(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }
}
