package dcsp;

import java.io.Serializable;

class Result implements Serializable {
    private static final long serialVersionUID = 1L;
    private String resultNodeName;
    private long result;


    Result(String resultNodeName, long result) {
        this.resultNodeName = resultNodeName;
        this.result = result;
    }


    String getResultNodeName() {
        return resultNodeName;
    }


    void setResultNodeName(String resultNodeName) {
        this.resultNodeName = resultNodeName;
    }


    long getResult() {
        return result;
    }


    void setResult(long result) {
        this.result = result;
    }
}
