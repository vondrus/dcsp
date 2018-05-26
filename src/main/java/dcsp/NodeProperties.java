package dcsp;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;


class NodeProperties implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nextNodeName;
    private LinkedBlockingQueue<TaskAssignment> taskAssignmentQueue;
    private boolean masterNode;
    private String resultNodeName;
    private long result;
    private int nodeSeqNumber;


    NodeProperties(String nextNodeName, LinkedBlockingQueue<TaskAssignment> taskAssignmentQueue, boolean masterNode, String resultNodeName, long result, int nodeSeqNumber) {
        this.nextNodeName = nextNodeName;
        this.taskAssignmentQueue = taskAssignmentQueue;
        this.masterNode = masterNode;
        this.resultNodeName = resultNodeName;
        this.result = result;
        this.nodeSeqNumber = nodeSeqNumber;
    }


    String getNextNodeName() {
        return nextNodeName;
    }


    LinkedBlockingQueue<TaskAssignment> getTaskAssignmentQueue() {
        return taskAssignmentQueue;
    }


    boolean getMasterNode() {
        return masterNode;
    }


    String getResultNodeName() {
        return resultNodeName;
    }


    long getResult() {
        return result;
    }


    int getNodeSeqNumber() {
        return nodeSeqNumber;
    }
}
