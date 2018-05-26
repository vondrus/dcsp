package dcsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;


class UdpClient {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private int udpServerPort;
    private DatagramSocket udpClientSocket;
    private InetAddress udpServerAddress;


    UdpClient(InetAddress udpServerAddress, int udpServerPort) {
        try {
            this.udpServerPort = udpServerPort;
            this.udpClientSocket = new DatagramSocket();
            this.udpServerAddress = udpServerAddress;
        }
        catch (SocketException e) {
            logger.error(e.getMessage());
            this.udpClientSocket = null;
        }
    }


    private void sendPacket(byte[] data, boolean broadcast) {
        if (udpClientSocket != null) {
            try {
                udpClientSocket.setBroadcast(broadcast);
                DatagramPacket packet = new DatagramPacket(data, data.length, udpServerAddress, udpServerPort);
                udpClientSocket.send(packet);
                logger.trace("Sent packet [len: "
                        + packet.getLength()
                        + ", to: "
                        + packet.getAddress().getHostAddress()
                        + ":"
                        + packet.getPort()
                        + "]: "
                        + new String(Arrays.copyOfRange(packet.getData(), 0, packet.getLength())));
            }
            catch (SocketException e) {
                logger.error("SocketException: " + e.getMessage());
            }
            catch (IOException e) {
                logger.error("IOException: " + e.getMessage());
            }
        }
        else {
            logger.error("Client UDP socket does not exist, datagram can not be sent.");
        }
    }


    void sendBroadcastRequest(String s) {
        sendPacket(s.getBytes(), true);
    }


    void sendBroadcastAnswer(String s) {
        sendPacket(s.getBytes(), false);
    }
}
