package dcsp;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class RmiServer extends UnicastRemoteObject implements RmiConnector, Serializable {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private static final long serialVersionUID = 1L;
    private SharedObject sharedObject;


    RmiServer(SharedObject sharedObject) throws RemoteException {
        super();
        this.sharedObject = sharedObject;
    }


    @Override
    public JoinObject join(String requesterNodeName) throws RemoteException {
        logger.rmi("Received request for join from node " + requesterNodeName + ".");

        String rvNextNodeName = sharedObject.getNextNodeName();

        // This is the only node (alone).
        if (rvNextNodeName == null) {
            rvNextNodeName = sharedObject.getNodeName();
        }

        // Try to create RMI connector to requester
        RmiConnector rmiConnector = ApplicationUtils.getRmiConnector(sharedObject, requesterNodeName);

        // OK:
        if (rmiConnector != null) {
            sharedObject.setNextNodeName(requesterNodeName);
            sharedObject.setNextNodeRmiConnector(rmiConnector);

            logger.rmi("Sending response to request. Next node is " + rvNextNodeName + ".");

            return new JoinObject(rvNextNodeName, sharedObject.incNodeSeqNumber());
        }

        // ERROR:
        else {
            sharedObject.setNextNodeName(null);
            sharedObject.setNextNodeRmiConnector(null);

            logger.error("Cannot create RMI connector to requester.");

            return null;
        }
    }


    @Override
    public void receive(Message message) throws RemoteException, InterruptedException {
        sharedObject.putIntoMessageQueue(message);
    }
}
