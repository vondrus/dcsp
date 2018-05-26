package dcsp;

import java.util.Properties;
import java.rmi.RemoteException;

import static java.lang.Thread.sleep;


public class Application {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private enum ExitMode { CORRECT, ERROR, SIGNAL  }
    private static final String VERSION = "0.8";
    private static final String MAIN_THREAD_NAME = "Application";
    private static final String INTERRUPT_THREAD_NAME = "Interrupt";
    private static final String UDPSERVER_THREAD_NAME = "UDP server";
    private static final String RMISERVER_THREAD_NAME = "RMI server";


    public static void main(String[] args) throws InterruptedException {

        logger.info("DCSP application version " + VERSION + " is started.");

        Thread.currentThread().setName(MAIN_THREAD_NAME);

        ExitMode exitMode = ExitMode.ERROR;


        // Create blank shared object -----------------------------[ EVALUATE COMMAND LINE ARGUMENTS ]----
        SharedObject sharedObject = new SharedObject(args);
        if ((sharedObject.peekAtTaskAssignmentQueue() == null) ||
            (sharedObject.getTaskSleep() < 0)) {
            System.exit(-1);
        }


        // Register a shutdown hook ------------------------------------------[ SHUTDOWN HOOK THREAD ]----
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().equals(MAIN_THREAD_NAME)) {
                    try {
                        t.interrupt();
                        t.join();
                        logger.warning("DCSP application was stopped by signal.");
                        break;
                    } catch (InterruptedException e) {
                        // This catch block is intentionally left blank.
                    }
                }
            }
            // >>> Exit point of application <<<
        }, INTERRUPT_THREAD_NAME));


        // Start Security Manager if none is running
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
            logger.debug1("The SecurityManager was not found, started another.");
        } else {
            logger.debug1("OK, the SecurityManager was found.");
        }


        // Declare RMI server thread reference
        RmiServerThread rmiServerThread = null;

        // Prepare and start UDP server thread
        UdpServerThread udpServerThread = new UdpServerThread(UDPSERVER_THREAD_NAME, sharedObject);
        udpServerThread.start();

        // Give UDP server thread time to establish socket (to find free port)
        if (sharedObject.getUdpServerPort() != 0) {

            // Create unique name of this node
            ApplicationUtils.createNodeName(sharedObject);

            // Item A.1 in the RMI FAQ (https://docs.oracle.com/javase/7/docs/technotes/guides/rmi/faq.html#domain)
            Properties props = System.getProperties();
            props.setProperty("java.rmi.server.hostname", sharedObject.getLocalIpAddress());

            try {
                // Detect or eventually create RMI registry
                sharedObject.setLocalRmiRegistry(new RmiRegistry());

                // Just prepare RMI server thread
                rmiServerThread = new RmiServerThread(RMISERVER_THREAD_NAME, sharedObject);


                // Discover local subnet -------------------------------------[ DISCOVER OTHER NODES ]----
                logger.info("Discovering local subnet...");

                // Set appropriate operating mode
                sharedObject.setOperatingState(OperatingState.DISCOVERING);

                // Try more UDP ports consecutively
                for (int i = 0; i < 5; i++) {
                    // Broadcast request datagram
                    UdpClient udpClient = new UdpClient(
                            ApplicationUtils.getSubnetBroadcastAddress(sharedObject.getPreferredNetworkInterface()),
                            SharedObject.UDP_SERVER_INIT_PORT + i);
                    udpClient.sendBroadcastRequest(sharedObject.getNodeRequest());

                    // Give network some time to interchange datagrams
                    sleep(200);
                }


                // Evaluate received offers -----------------------------------[ IS THERE SOME NODE? ]----

                // [A] Some offers was received => There is at least one node
                if (sharedObject.getMasterNodeName() != null) {

                    RmiConnector masterRmiConnector = ApplicationUtils.getRmiConnector(sharedObject, sharedObject.getMasterNodeName());
                    if (masterRmiConnector != null) {

                        // Start RMI server thread
                        rmiServerThread.start();
                        sharedObject.waitForRmiServerStarted();

                        try {
                            JoinObject joinObject = masterRmiConnector.join(sharedObject.getNodeName());
                            if (joinObject != null) {

                                sharedObject.setNextNodeName(joinObject.getNextNodeName());
                                sharedObject.setNodeIndex(joinObject.getNodeIndex());

                                RmiConnector neighborRmiConnector = ApplicationUtils.getRmiConnector(sharedObject, joinObject.getNextNodeName());
                                if (neighborRmiConnector != null) {

                                    sharedObject.setNextNodeRmiConnector(neighborRmiConnector);
                                    logger.rmi("Joined to the ring. This: "
                                            + sharedObject.getNodeName()
                                            + " --> Next: "
                                            + joinObject.getNextNodeName()
                                            + " (this index = "
                                            + joinObject.getNodeIndex()
                                            + ")."
                                    );

                                    logger.major("Starting as one from more nodes... "
                                            + "(Node name: " + sharedObject.getNodeName()
                                            + ", index: " + sharedObject.getNodeIndex()
                                            + ")"
                                    );

                                    // Set node as a non master
                                    sharedObject.setMasterNode(false);

                                    // Set appropriate operating mode
                                    sharedObject.setOperatingState(OperatingState.WORKING);

                                    // Remove initial task assignment from queue
                                    sharedObject.pollFromTaskAssignmentQueue();

                                    // Start "core logic"
                                    StateMachine stateMachine = new StateMachine(sharedObject);
                                    stateMachine.start();

                                    exitMode = ExitMode.CORRECT;
                                }
                                else {
                                    logger.error("getRmiConnector(): Null response from node " + joinObject.getNextNodeName() + ".");
                                }
                            }
                            else {
                                logger.error("join(): Null response to ask for join.");
                            }
                        }
                        catch (RemoteException e) {
                            logger.error("join(): " + e.getMessage());
                        }
                    }
                    else {
                        logger.error("getRandomNodeRmiConnector(): Cannot create RMI connector to any node.");
                    }
                }

                // [B] No offer was received => I'm the only node
                else {
                    // Start RMI server thread
                    rmiServerThread.start();
                    sharedObject.waitForRmiServerStarted();

                    logger.major("Starting as an only node... "
                            + "(Node name: " + sharedObject.getNodeName()
                            + ", index: " + sharedObject.getNodeIndex()
                            + ")"
                    );

                    // Set node as a master, node count and node index
                    sharedObject.setMasterNode(true);

                    // Set appropriate operating mode
                    sharedObject.setOperatingState(OperatingState.WORKING);

                    // Start "core logic"
                    StateMachine stateMachine = new StateMachine(sharedObject);
                    stateMachine.start();

                    exitMode = ExitMode.CORRECT;
                }

            }
            catch (InterruptedException e) {
                logger.signal();
                exitMode = ExitMode.SIGNAL;
            }
            catch (ApplicationException e) {
                if (e.getExceptionMessage() != null) {
                    logger.error(e.getExceptionMessage());
                }
            }

        }   // getUdpServerPort()


        // -----------------------------------------------------------------[ MAIN THREAD EXIT POINT ]----
        // Stop RMI server and wait for it
        if (rmiServerThread != null) {
            rmiServerThread.interrupt();
            rmiServerThread.join();
        }

        // Stop UDP server and wait for it
        udpServerThread.interrupt();
        udpServerThread.join();

        if (exitMode == ExitMode.CORRECT) {
            logger.info("DCSP application was correctly stopped.");
        }
        else if (exitMode == ExitMode.ERROR) {
            logger.error("DCSP application was unexpectedly stopped.");
        }

    }   // main()

}   // class Application
