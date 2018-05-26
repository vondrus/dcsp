package dcsp;


class TaskAssignmentWrapper {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private static final long MIN_NUMBER_TO_VERIFY = 100000;
    private static final long MIN_CURRENT_DIVISOR = 256;


    private static boolean isEligible(long l) {
        for (long i = 2; i < MIN_CURRENT_DIVISOR; i++) {
            if (l % i == 0) {
                return false;
            }
        }
        return true;
    }


    static TaskAssignment create(String s) {
        try {
            // 1) Is argument parsable number?
            long l = Long.parseLong(s);

            // 2) Is number to verify greater than or equal to MIN_NUMBER_TO_VERIFY?
            if (l >= MIN_NUMBER_TO_VERIFY) {

                // 3) Is number to verify eligible for testing?
                if (isEligible(l)) {
                    return new TaskAssignment(l, MIN_CURRENT_DIVISOR, Math.round(Math.sqrt((double) l)));
                }
                else {
                    logger.error(s + " is not prime number.");
                }
            }
            else {
                logger.error("Number to verify must be greater than or equal to " + MIN_NUMBER_TO_VERIFY + ".");
            }
        }
        catch (NumberFormatException e) {
            logger.error("Argument \'" + s + "\' is not a parsable number.");
        }
        return null;
    }

}
