package dcsp;

import java.io.Serializable;


class TaskAssignment implements Serializable {
    private static final long serialVersionUID = 1L;
    private long numberToVerify;
    private long currentDivisor;
    private long lastDivisor;


    TaskAssignment() {
        this.numberToVerify = 0;
        this.currentDivisor = 0;
        this.lastDivisor = 0;
    }


    TaskAssignment(long numberToVerify, long currentDivisor, long lastDivisor) {
        this.numberToVerify = numberToVerify;
        this.currentDivisor = currentDivisor;
        this.lastDivisor = lastDivisor;
    }


    long getNumberToVerify() {
        return numberToVerify;
    }


    long getCurrentDivisor() {
        return currentDivisor;
    }


    long getLastDivisor() {
        return lastDivisor;
    }
}
