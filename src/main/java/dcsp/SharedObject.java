package dcsp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


class SharedObject {
    static final int RMI_REGISTRY_PORT = 2345;
    static final int UDP_SERVER_INIT_PORT = 6789;
    private static final String DEFAULT_NUMBER_TO_VERIFY = "62710561";

    private int udpServerPort;
    private boolean rmiServerStarted;
    private RmiRegistry localRmiRegistry;
    private RmiRegistry remoteRmiRegistry;
    private int nodeIndex;
    private int nodeSeqNumber;
    private String nodeName;
    private String nodeRequest;
    private String nextNodeName;
    private RmiConnector nextNodeRmiConnector;
    private String localIpAddress;
    private String preferredNetworkInterface;
    private OperatingState operatingState;
    private LinkedBlockingQueue<Message> messageQueue;
    private LinkedBlockingQueue<TaskAssignment> taskAssignmentQueue;
    private long taskSleep;
    private boolean masterNode;
    private String masterNodeName;


    SharedObject(String[] args) {
        this.udpServerPort = -1;        // -1 = not set up yet, 0 = Error, 1+ = OK
        this.operatingState = OperatingState.INITIALIZING;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.taskAssignmentQueue = new LinkedBlockingQueue<>();

        evaluateCLA(args);
    }


    private void evaluateCLA(String[] args) {
        switch (args.length) {
            case 3:
                this.preferredNetworkInterface = args[2];

            case 2:
                this.taskSleep = ApplicationUtils.parseUnsignedLong(args[1]);

            case 1:
                this.putIntoTaskAssignmentQueue(TaskAssignmentWrapper.create(args[0]));
                break;

            default:
                this.putIntoTaskAssignmentQueue(TaskAssignmentWrapper.create(DEFAULT_NUMBER_TO_VERIFY));
        }
    }


    synchronized int getUdpServerPort() {
        while (udpServerPort < 0) {
            try {
                wait();
            }
            catch (InterruptedException e) {
                // This catch block is intentionally left blank.
            }
        }
        return udpServerPort;
    }


    synchronized void setUdpServerPort(int udpServerPort) {
        this.udpServerPort = udpServerPort;
        notify();
    }


    synchronized void waitForRmiServerStarted() {
        while (!rmiServerStarted) {
            try {
                wait();
            }
            catch (InterruptedException e) {
                // This catch block is intentionally left blank.
            }
        }
    }


    synchronized void setRmiServerStarted() {
        this.rmiServerStarted = true;
        notify();
    }


    synchronized RmiRegistry getLocalRmiRegistry() {
        return localRmiRegistry;
    }


    synchronized void setLocalRmiRegistry(RmiRegistry localRmiRegistry) {
        this.localRmiRegistry = localRmiRegistry;
    }


    synchronized RmiRegistry getRemoteRmiRegistry() {
        return remoteRmiRegistry;
    }


    synchronized void setRemoteRmiRegistry(RmiRegistry remoteRmiRegistry) {
        this.remoteRmiRegistry = remoteRmiRegistry;
    }


    synchronized String getNodeName() {
        return nodeName;
    }


    synchronized void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }


    synchronized String getNodeRequest() {
        return nodeRequest;
    }


    synchronized void setNodeRequest(String nodeRequest) {
        this.nodeRequest = nodeRequest;
    }


    synchronized String getNextNodeName() {
        return nextNodeName;
    }


    synchronized void setNextNodeName(String nextNodeName) {
        this.nextNodeName = nextNodeName;
    }


    synchronized RmiConnector getNextNodeRmiConnector() {
        return nextNodeRmiConnector;
    }


    synchronized void setNextNodeRmiConnector(RmiConnector nextNodeRmiConnector) {
        this.nextNodeRmiConnector = nextNodeRmiConnector;
    }


    synchronized String getLocalIpAddress() {
        return localIpAddress;
    }


    synchronized void setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
    }


    synchronized String getPreferredNetworkInterface() {
        return preferredNetworkInterface;
    }


    long getTaskSleep() {
        return taskSleep;
    }


    synchronized OperatingState getOperatingState() {
        return operatingState;
    }


    synchronized void setOperatingState(OperatingState operatingState) {
        this.operatingState = operatingState;
    }


    void putIntoMessageQueue(Message message) throws InterruptedException {
        messageQueue.put(message);
    }


    Message pollFromMessageQueue(long timeout) throws InterruptedException {
        return messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
    }


    boolean putIntoTaskAssignmentQueue(TaskAssignment taskAssignment) {
        return taskAssignmentQueue.offer(taskAssignment);
    }


    TaskAssignment pollFromTaskAssignmentQueue() {
        return taskAssignmentQueue.poll();
    }


    TaskAssignment peekAtTaskAssignmentQueue() {
        return taskAssignmentQueue.peek();
    }


    LinkedBlockingQueue<TaskAssignment> getTaskAssignmentQueue() {
        return taskAssignmentQueue;
    }


    int getNodeIndex() {
        return nodeIndex;
    }


    void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }


    int getNodeSeqNumber() {
        return this.nodeSeqNumber;
    }


    void setNodeSeqNumber(int nodeSeqNumber) {
        this.nodeSeqNumber = nodeSeqNumber;
    }


    int incNodeSeqNumber() {
        this.nodeSeqNumber++;
        return this.nodeSeqNumber;
    }


    boolean isMasterNode() {
        return masterNode;
    }


    void setMasterNode(boolean masterNode) {
        this.masterNode = masterNode;
    }


    String getMasterNodeName() {
        return masterNodeName;
    }


    void setMasterNodeName(String masterNodeName) {
        this.masterNodeName = masterNodeName;
    }
}
