package dcsp;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;


class ApplicationLogger {
    private enum AnsiColors { RESET, BLACK, RED, GREEN, GREEN_INTENSIVE, YELLOW, BLUE, PURPLE, CYAN, WHITE, WHITE_INTENSIVE }
    private static final Map<AnsiColors, String> ANSI_COLORS;
    static {
        Map<AnsiColors, String> map = new HashMap<>();
        map.put(AnsiColors.RESET, "\u001B[0m");
        map.put(AnsiColors.BLACK, "\u001B[30m");
        map.put(AnsiColors.RED, "\u001B[31m");
        map.put(AnsiColors.GREEN, "\u001B[32m");
        map.put(AnsiColors.GREEN_INTENSIVE, "\u001B[1m\u001B[32m");
        map.put(AnsiColors.YELLOW, "\u001B[33m");
        map.put(AnsiColors.BLUE, "\u001B[34m");
        map.put(AnsiColors.PURPLE, "\u001B[35m");
        map.put(AnsiColors.CYAN, "\u001B[36m");
        map.put(AnsiColors.WHITE, "\u001B[37m");
        map.put(AnsiColors.WHITE_INTENSIVE, "\u001B[1m\u001B[37m");

        ANSI_COLORS = Collections.unmodifiableMap(map);
    }
    private SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss.SSS");
    private AtomicBoolean isNewLine;


    public ApplicationLogger(AtomicBoolean isNewLine) {
        this.isNewLine = isNewLine;
    }


    void taskActivity(long currentDivisor, long lastDivisor) {
        System.out.printf("\r%s                                       Task is running (divisor: %d/%d)...%s",
            ANSI_COLORS.get(AnsiColors.CYAN), currentDivisor, lastDivisor, ANSI_COLORS.get(AnsiColors.RESET));

        isNewLine.set(false);
    }


    private void print(String entry, String className, String methodName, String level, AnsiColors color) {

        if (! isNewLine.get())
            System.out.println();

        String threadName = Thread.currentThread().getName();
        if (threadName.length() > 11) {
            threadName = threadName.substring(0, 11);
        }

        System.out.println(String.format("%s%s | %-7s | %-11s | %s.%s - %s%s",
            ANSI_COLORS.get(color), sdfDate.format(new Date()), level, threadName,
            className, methodName, entry, ANSI_COLORS.get(AnsiColors.RESET)));

        isNewLine.set(true);
    }


    void trace(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "TRACE", AnsiColors.BLUE);
    }


    void debug1(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "DEBUG1", AnsiColors.GREEN);
    }


    void debug2(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "DEBUG2", AnsiColors.GREEN_INTENSIVE);
    }


    void info(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "INFO", AnsiColors.WHITE);
    }


    void major(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "MAJOR", AnsiColors.WHITE_INTENSIVE);
    }


    void signal() {
        System.out.println();
        print("Interrupt signal was caught.", new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "SIGNAL", AnsiColors.YELLOW);
    }


    void rmi(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "RMI", AnsiColors.PURPLE);
    }


    void warning(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "WARNING", AnsiColors.YELLOW);
    }


    void error(String entry) {
        print(entry, new Exception().getStackTrace()[1].getClassName(),
                new Exception().getStackTrace()[1].getMethodName(),
                "ERROR", AnsiColors.RED);
    }
}
