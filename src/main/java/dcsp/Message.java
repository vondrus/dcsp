package dcsp;

import java.io.Serializable;


class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Subjects { JOB, TOKEN, TERMINATE, DISJOIN }

    private Subjects subject;
    private int sourceNodeIndex;
    private String sourceNodeName;
    private String destinationNodeName;
    private Object body;


    Message(Subjects subject, int sourceNodeIndex, String sourceNodeName, String destinationNodeName, Object body) {
        this.subject = subject;
        this.sourceNodeIndex = sourceNodeIndex;
        this.sourceNodeName = sourceNodeName;
        this.destinationNodeName = destinationNodeName;
        this.body = body;
    }


    Subjects getSubject() {
        return subject;
    }


    int getSourceNodeIndex() {
        return sourceNodeIndex;
    }


    String getSourceNodeName() {
        return sourceNodeName;
    }


    String getDestinationNodeName() {
        return destinationNodeName;
    }


    Object getBody() {
        return body;
    }


    void setBody(Object body) {
        this.body = body;
    }
}
