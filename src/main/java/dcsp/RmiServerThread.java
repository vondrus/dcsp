package dcsp;

import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;


class RmiServerThread extends Thread {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private SharedObject sharedObject;
    private RmiServer rmiServer;


    RmiServerThread(String name, SharedObject sharedObject) throws ApplicationException {
        super(name);
        this.sharedObject = sharedObject;
    }


    @Override
    public void interrupt() {
        logger.info("Shutting down RMI server...");

        // Remove remote object from RMI runtime
        try {
            boolean b = UnicastRemoteObject.unexportObject(rmiServer, true);
            logger.debug1("Result of removing object " + rmiServer.getClass().getSimpleName() + " from RMI runtime: " + b);
        }
        catch (NoSuchObjectException e) {
            logger.error("The remote object is not currently exported in RMI runtime.");
        }

        // Unbind (disassociate) name of remote object from RMI registry
        try {
            sharedObject.getLocalRmiRegistry().unbind(sharedObject.getNodeName());
        }
        catch (ApplicationException e) {
            // This catch block is intentionally left blank.
        }

        logger.info("RMI server was correctly stopped.");

        super.interrupt();
    }


    @Override
    public void run() {
        logger.info("Starting up RMI server...");

        try {
            rmiServer = new RmiServer(sharedObject);
            sharedObject.getLocalRmiRegistry().bind(rmiServer, sharedObject.getNodeName());
        }
        catch (ApplicationException e) {
            // This catch block is intentionally left blank.
        }
        catch (RemoteException e) {
            logger.error("The remote object cannot be instantiate.");
            e.printStackTrace();
        }

        logger.info("RMI server is running.");

        sharedObject.setRmiServerStarted();
    }
}
