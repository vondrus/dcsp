package dcsp;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;


public class Task implements Callable<Long> {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private static final long TASK_ACTIVITY_LOGGER_INTERVAL = 500;

    private long numberToVerify;
    private long currentDivisor;
    private long lastDivisor;
    private long newCurrentDivisor;
    private long newLastDivisor;
    private long taskSleep;
    private long taskActivityTimestamp;
    private AtomicBoolean flag = new AtomicBoolean();
    private AtomicBoolean isRunning = new AtomicBoolean();


    Task(TaskAssignment taskAssignment, long taskSleep) {
        this.numberToVerify = taskAssignment.getNumberToVerify();
        this.currentDivisor = taskAssignment.getCurrentDivisor();
        this.lastDivisor = taskAssignment.getLastDivisor();
        this.newCurrentDivisor = 0;
        this.newLastDivisor = 0;
        this.taskSleep = taskSleep;
    }


    TaskAssignment getLegacyTaskAssignment() {
        if (isRunning.get()) {
            return new TaskAssignment(numberToVerify, currentDivisor, lastDivisor);
        }
        else {
            return null;
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    TaskAssignment getNewTaskAssignment() {
        if (isRunning.get()) {

            flag.set(true);

            while (flag.get());

            if ((newCurrentDivisor != 0) && (newLastDivisor != 0)) {
                return new TaskAssignment(numberToVerify, newCurrentDivisor, newLastDivisor);
            }
        }
        return new TaskAssignment();
    }


    @Override
    public Long call() throws InterruptedException {

        isRunning.set(true);

        while (currentDivisor <= lastDivisor) {

            // PAUSE: Prepare new task assignment
            if (flag.get()) {
                long remain = lastDivisor - currentDivisor;
                if (remain > 100) {
                    newLastDivisor = lastDivisor;
                    lastDivisor = currentDivisor + (remain / 2);
                    newCurrentDivisor = lastDivisor + 1;
                }
                else {
                    newCurrentDivisor = 0;
                    newLastDivisor = 0;
                }
                flag.set(false);
            }

            if (numberToVerify % currentDivisor == 0) {
                isRunning.set(false);
                return currentDivisor;
            }

            // Print Log message
            if (System.currentTimeMillis() > taskActivityTimestamp) {
                logger.taskActivity(currentDivisor, lastDivisor);
                taskActivityTimestamp = System.currentTimeMillis() + TASK_ACTIVITY_LOGGER_INTERVAL;
            }

            currentDivisor++;

            // Sleep for stated time
            if (taskSleep > 0) {
                try {
                    sleep(taskSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        isRunning.set(false);

        return (long) 0;

    }
}
