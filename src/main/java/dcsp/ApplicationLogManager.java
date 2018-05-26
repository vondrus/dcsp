package dcsp;


import java.util.concurrent.atomic.AtomicBoolean;

class ApplicationLogManager {
    private static AtomicBoolean isNewLine = new AtomicBoolean();


    static ApplicationLogger getLogger() {
        return new ApplicationLogger(isNewLine);
    }
}
