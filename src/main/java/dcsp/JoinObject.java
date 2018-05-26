package dcsp;

import java.io.Serializable;


// Attention! Modifier "public" is important. It prevents java.lang.IllegalAccessError.
public class JoinObject implements Serializable{
    private static final long serialVersionUID = 1L;
    private String nextNodeName;
    private int nodeIndex;


    JoinObject(String nextNodeName, int nodeIndex) {
        this.nextNodeName = nextNodeName;
        this.nodeIndex = nodeIndex;
    }


    String getNextNodeName() {
        return nextNodeName;
    }


    int getNodeIndex() {
        return nodeIndex;
    }
}
