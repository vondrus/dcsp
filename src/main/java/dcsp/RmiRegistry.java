package dcsp;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.NotBoundException;


class RmiRegistry {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private Registry registry;


    RmiRegistry() throws ApplicationException {
        try {
            // Try to create reference to the registry on localhost
            registry = LocateRegistry.getRegistry(SharedObject.RMI_REGISTRY_PORT);

            String[] rmiRegistryContent = this.getArrayOfRmiRegistryContent();
            if (rmiRegistryContent == null) {
                // It seems no registry running on localhost...
                try {
                    // Create a new one (it's possible on localhost only).
                    registry = LocateRegistry.createRegistry(SharedObject.RMI_REGISTRY_PORT);
                    logger.info("No RMI registry detected on localhost, a new one has been created (port " + SharedObject.RMI_REGISTRY_PORT + ").");
                }
                catch (RemoteException e) {
                    logger.error("RMI registry could not be created.");
                    throw new ApplicationException(null);
                }
            }
            else {
                logger.info("RMI registry detected on localhost (port " + SharedObject.RMI_REGISTRY_PORT + ").");
                logger.debug1("List of bound objects: " + this.getStringOfRmiRegistryContent());
            }
        }
        catch (RemoteException e) {
            logger.error(e.getMessage());
        }
    }


    RmiRegistry(String host) throws ApplicationException {
        try {
            // Try to create reference to the registry on remote host
            registry = LocateRegistry.getRegistry(host, SharedObject.RMI_REGISTRY_PORT);

            String[] rmiRegistryContent = this.getArrayOfRmiRegistryContent();
            if (rmiRegistryContent == null) {
                // It seems no registry running on remote host...
                logger.error("No RMI registry detected on " + host);
            }
            else {
                logger.info("RMI registry detected on " + host + " (port " + SharedObject.RMI_REGISTRY_PORT + ").");
                logger.debug1("List of bound objects: " + this.getStringOfRmiRegistryContent());
            }
        }
        catch (RemoteException e) {
            logger.error(e.getMessage());
        }
    }


    void bind(Remote obj, String rmiRegistryObjectName) throws ApplicationException {
        try {
            registry.rebind(rmiRegistryObjectName, obj);
            logger.debug1("The object " + obj.getClass().getSimpleName() + " is successfully bound with name " + rmiRegistryObjectName + ".");
        }
        catch (RemoteException e) {
            logger.error("The object " + obj.getClass().getSimpleName() + " cannot be bind with name " + rmiRegistryObjectName + ".");
            e.printStackTrace();
            throw new ApplicationException(null);
        }
    }


    void unbind(String rmiRegistryObjectName) throws ApplicationException {
        try {
            registry.unbind(rmiRegistryObjectName);
            logger.debug1("The object with name " + rmiRegistryObjectName + " is successfully unbound.");
        }
        catch (RemoteException e) {
            logger.warning("The object cannot be unbind.");
            throw new ApplicationException(null);
        }
        catch (NotBoundException e) {
            logger.warning("The name " + rmiRegistryObjectName + " is not currently bound.");
            throw new ApplicationException(null);
        }
    }


    RmiConnector lookup(String rmiRegistryObjectName) throws ApplicationException {
        RmiConnector remoteObject;

        try {
            remoteObject = (RmiConnector) registry.lookup(rmiRegistryObjectName);
            return remoteObject;
        }
        catch (RemoteException e) {
            logger.error("Lookup to the registry cannot be done.");
            throw new ApplicationException(null);
        }
        catch (NotBoundException e) {
            logger.error("The name " + rmiRegistryObjectName + " is not currently bound.");
            throw new ApplicationException(null);
        }
    }


    private String[] getArrayOfRmiRegistryContent() {
        String[] rmiRegistryContent = null;

        try {
            rmiRegistryContent = registry.list();
        }
        catch (RemoteException e) {
            // This catch block is intentionally left blank.
        }

        return rmiRegistryContent;
    }


    private String getStringOfRmiRegistryContent() {
        String[] rmiRegistryContent = this.getArrayOfRmiRegistryContent();

        if (rmiRegistryContent != null) {
            return Arrays.stream(rmiRegistryContent).collect(Collectors.joining(", "));
        }
        else {
            return null;
        }
    }
}
