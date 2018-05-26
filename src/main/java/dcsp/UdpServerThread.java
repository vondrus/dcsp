package dcsp;

import java.net.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


class UdpServerThread extends Thread {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private SharedObject sharedObject;
    private static final int UDP_SOCKET_BIND_ATTEMPTS = 10;
    private static final int UDP_BUFFER_LENGTH = 2048;
    private int udpServerInitPort;
    private int udpServerPort;
    private DatagramSocket udpServerSocket;
    private boolean isRunning;


    UdpServerThread(String name, SharedObject sharedObject) {
        super(name);
        this.sharedObject = sharedObject;
        this.udpServerInitPort = SharedObject.UDP_SERVER_INIT_PORT;
        this.udpServerPort = 0;
        this.isRunning = true;
    }


    @Override
    public void interrupt() {
        logger.info("Shutting down UDP server...");
        this.isRunning = false;
        this.udpServerSocket.close();
        super.interrupt();
    }


    @Override
    public void run() {
        byte[] buffer = new byte[UDP_BUFFER_LENGTH];
        DatagramPacket packet;
        String message;

        logger.info("Starting up UDP server...");

        // Try to create socket and bind this DatagramSocket to a specific port
        for (int i = 0; i < UDP_SOCKET_BIND_ATTEMPTS; i++) {
            try {
                udpServerSocket = new DatagramSocket(udpServerInitPort, InetAddress.getByName("0.0.0.0"));
                udpServerSocket.setBroadcast(true);
                udpServerPort = udpServerInitPort;
                break;
            }
            catch (UnknownHostException e) {
                logger.error(e.getMessage());
            }
            catch (SocketException e) {
                udpServerInitPort++;
            }
        }

        // Setup UDP server port in shared object -> notify main thread
        sharedObject.setUdpServerPort(udpServerPort);

        // Receiving loop
        if (udpServerPort != 0) {
            logger.info("UDP server is running (port " + udpServerPort + ").");
            while (isRunning) {
                try {
                    packet = new DatagramPacket(buffer, buffer.length);
                    udpServerSocket.receive(packet);

                    // Some datagram was received
                    message = new String(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
                    InetAddress clientAddress = packet.getAddress();
                    logger.trace("Received packet [len: "
                            + packet.getLength()
                            + ", from: "
                            + clientAddress.getHostAddress()
                            + ":"
                            + packet.getPort()
                            + "]: "
                            + message);

                    // Processing the datagram
                    OperatingState operatingState = sharedObject.getOperatingState();
                    if (operatingState != OperatingState.INITIALIZING) {

                        // Operating state: WORKING
                        if (operatingState == OperatingState.WORKING) {
                            // Received REQUEST
                            if (message.indexOf("Request-") == 0) {
                                // (Not from myself) AND (I'm master node) -> Send ANSWER
                                if ((! message.equals(sharedObject.getNodeRequest())) &&
                                    (sharedObject.isMasterNode())) {
                                    int clientPort = Integer.parseInt(message.substring(message.lastIndexOf('-') + 1, message.length()));
                                    UdpClient udpClient = new UdpClient(clientAddress, clientPort);
                                    udpClient.sendBroadcastAnswer(sharedObject.getNodeName());
                                    logger.info("Request datagram was received. Answer datagram was sent.");
                                }
                            }
                        }

                        // Operating state: DISCOVERING
                        else if (operatingState == OperatingState.DISCOVERING) {
                            // Received ANSWER (list of nodes)
                            if (message.indexOf("Node-") == 0) {
                                sharedObject.setMasterNodeName(message);
                                logger.info("Answer datagram was received. Master node is " + message);
                            }
                        }
                    }
                }

                // Some IO error occurred
                catch (IOException e) {
                    if (isRunning) {
                        logger.error(e.getMessage());
                    }
                }
            }
            logger.info("UDP server was correctly stopped.");
        }

        // PROBLEM: There is not a free port for server
        else {
            logger.error("No free UDP port for server was found.");
        }
    }
}
