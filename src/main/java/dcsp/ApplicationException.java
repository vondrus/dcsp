package dcsp;


@SuppressWarnings("unused")
class ApplicationException extends Exception {
    private Throwable originException;
    private String exceptionMessage;


    ApplicationException(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }


    ApplicationException(Throwable originException, String exceptionMessage) {
        this.originException = originException;
        this.exceptionMessage = exceptionMessage;
    }


    Throwable getOriginException() {
        return originException;
    }


    String getExceptionMessage() {
        return exceptionMessage;
    }
}
