package dcsp;

import java.net.InetAddress;
import java.util.*;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;


class ApplicationUtils {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();


    // Detect local subnet broadcast address
    static InetAddress getSubnetBroadcastAddress(String preferredNetworkInterface) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface i = interfaces.nextElement();
                for (InterfaceAddress f : i.getInterfaceAddresses()) {
                    if (f.getAddress().isSiteLocalAddress()) {
                        if ((preferredNetworkInterface == null) || (preferredNetworkInterface.equals(i.getName()))) {
                            return f.getBroadcast();
                        }
                    }
                }
            }
        }
        catch(SocketException e) {
            logger.error("Local subnet broadcast address could not be detected.");
        }
        return null;
    }


    // Detect local IP address (not loopback)
    private static String getLocalIpAddress(String preferredNetworkInterface) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface i = interfaces.nextElement();
                for (InterfaceAddress f : i.getInterfaceAddresses()) {
                    if (f.getAddress().isSiteLocalAddress()) {
                        if ((preferredNetworkInterface == null) || (preferredNetworkInterface.equals(i.getName()))) {
                            return f.getAddress().getHostAddress();
                        }
                    }
                }
            }
        }
        catch(SocketException e) {
            logger.error("Local IP address could not be detected.");
        }
        return null;
    }


    // Create unique name of this node
    static void createNodeName(SharedObject sharedObject) {
        String localIpAddress = getLocalIpAddress(sharedObject.getPreferredNetworkInterface());

        if (localIpAddress != null) {
            sharedObject.setNodeName("Node-".concat(localIpAddress.concat("-")
                    .concat(Integer.toString(sharedObject.getUdpServerPort()))));

            sharedObject.setNodeRequest("Request-".concat(localIpAddress.concat("-")
                    .concat(Integer.toString(sharedObject.getUdpServerPort()))));

            sharedObject.setLocalIpAddress(localIpAddress);
        }
    }


    // Get RMI Connector from RMI Registry
    static RmiConnector getRmiConnector(SharedObject sharedObject, String nodeName) {
        String remoteIpAddress = nodeName.substring(nodeName.indexOf('-') + 1, nodeName.lastIndexOf('-'));

        try {
            // It's localhost -> Get local RMI registry for lookup
            if (remoteIpAddress.equals(sharedObject.getLocalIpAddress())) {
                sharedObject.setRemoteRmiRegistry(sharedObject.getLocalRmiRegistry());
            }
            // It's remote host -> Get remote RMI registry for lookup
            else {
                sharedObject.setRemoteRmiRegistry(new RmiRegistry(remoteIpAddress));
            }
            return sharedObject.getRemoteRmiRegistry().lookup(nodeName);
        }
        catch (ApplicationException e) {
            return null;
        }
    }


    // Parse command line argument to long
    static long parseUnsignedLong(String s) {
        try {
            // 1) Is argument parsable number?
            long l = Long.parseLong(s);

            // 2) Is number to verify greater than or equal to MIN_NUMBER_TO_VERIFY?
            if (l >= 0) {
                return l;
            }
            else {
                logger.error("Argument \'" + s + "\' must be unsigned integer.");
            }
        }
        catch (NumberFormatException e) {
            logger.error("Argument \'" + s + "\' is not a parsable number.");
        }
        return -1;
    }

}
