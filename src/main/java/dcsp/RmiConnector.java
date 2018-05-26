package dcsp;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RmiConnector extends Remote {

    JoinObject join(String requesterNodeName) throws RemoteException;

    void receive(Message message) throws RemoteException, InterruptedException;
}
