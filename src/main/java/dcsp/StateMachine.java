package dcsp;

import java.rmi.RemoteException;
import java.util.concurrent.*;


class StateMachine {
    private static final ApplicationLogger logger = ApplicationLogManager.getLogger();
    private static final long QUEUE_BLOCKING_TIMEOUT = 100;
    private static final long TASK_BLOCKING_TIMEOUT = 1;
    private static final long AWAIT_TERMINATION_TIMEOUT = 1000;
    private enum Phases { ASK_FOR_JOB, START_JOB, WATCH_JOB, WAIT_FOR_MESSAGE, TERMINATION }
    private enum States { ACTIVE, PASSIVE }
    private enum Colors { WHITE, BLACK }
    private SharedObject sharedObject;


    StateMachine(SharedObject sharedObject) {
        this.sharedObject = sharedObject;
    }


    private void shutDownExecutorService(ExecutorService executorService) {
        logger.debug1("Shutting down task...");
        executorService.shutdownNow();
        try {
            if (! executorService.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                logger.error("Timeout elapsed before termination.");
            } else {
                logger.debug1("Task was orderly terminated.");
            }
        } catch (InterruptedException e) {
            // This catch block is intentionally left blank.
        }
    }


    private void rmiSendMessage(Message message, boolean forward) throws InterruptedException {
        try {
            sharedObject.getNextNodeRmiConnector().receive(message);

            String s1 = "null";
            String s2 = "Message sent";

            if (message.getBody() != null) {
                s1 = message.getBody().getClass().getName();
            }

            if (forward) {
                s2 = "Forwarded message";
            }
            logger.debug1(s2
                    + ": Subject = " + message.getSubject()
                    + ", Source = " + message.getSourceNodeName()
                    + ", Destination = " + message.getDestinationNodeName()
                    + ", Body = " + s1
            );
        }
        catch (RemoteException e) {
            logger.error("Remote exception was caught: " + e.getMessage());
        }
    }


    private void sendMessage(Message.Subjects subject, String destination, Object body) throws InterruptedException {
        rmiSendMessage(new Message(subject, sharedObject.getNodeIndex(), sharedObject.getNodeName(), destination, body), false);
    }


    private void forwardMessage(Message message) throws InterruptedException {
        rmiSendMessage(message, true);
    }


    void start() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Task task = null;
        Future taskResult = null;
        String resultNodeName = null;
        long result = -1;
        boolean mainLoop = true;
        boolean terminationDetected = false;
        boolean forwardMessage;
        Message message;
        Phases currentPhase = Phases.ASK_FOR_JOB;

        // Termination Detection (Dijkstra - Feijen - Van Gasteren algorithm)
        // Hereafter in the source code as DFG
        // [ DFG1 ]
        boolean tokenPresent = false;
        Colors tokenColor = Colors.WHITE;
        Colors processColor = Colors.WHITE;
        States currentState = States.ACTIVE;


        while (mainLoop) {

            try {
                // Try to get message from queue
                message = sharedObject.pollFromMessageQueue(QUEUE_BLOCKING_TIMEOUT);

                // Some message exists, process it
                if (message != null) {

                    forwardMessage = true;

                    String s = "null";

                    if (message.getBody() != null) {
                        s = message.getBody().getClass().getName();
                    }

                    logger.debug1("Message received"
                            + ": Subject = " + message.getSubject()
                            + ", Source = " + message.getSourceNodeName()
                            + ", Destination = " + message.getDestinationNodeName()
                            + ", Body = " + s
                    );


                    switch (message.getSubject()) { // ===============================[ Received message ]====

                        // ------------------------------------------------------------------------[ JOB ]----
                        case JOB: {

                            // Request for a job
                            if (! message.getDestinationNodeName().equals(sharedObject.getNodeName())) {

                                // Nobody responded yet
                                if (message.getBody() == null) {

                                    // We have enough job
                                    if (task != null) {
                                        TaskAssignment taskAssignment = task.getNewTaskAssignment();

                                        if (taskAssignment.getNumberToVerify() != 0) {
                                            message.setBody(taskAssignment);

                                            if (message.getSourceNodeIndex() > sharedObject.getNodeIndex()) {  // [ DFG5 ]
                                                processColor = Colors.BLACK;
                                            }

                                            logger.debug2("New task sent: numberToVerify = " + taskAssignment.getNumberToVerify()
                                                    + ", currentDivisor = " + taskAssignment.getCurrentDivisor()
                                                    + ", lastDivisor = " + taskAssignment.getLastDivisor()
                                            );
                                        }
                                        else {
                                            logger.debug2("Response sent: Local node has no more job. (Task is not running.)");
                                        }
                                    }
                                }
                            }

                            // Response to request
                            else {

                                // New job was received
                                if (message.getBody() != null) {

                                    TaskAssignment taskAssignment = (TaskAssignment) message.getBody();

                                    if (taskAssignment.getNumberToVerify() != 0) {
                                        logger.debug2("New task received from node " + message.getSourceNodeName()
                                                + " (numberToVerify = " + taskAssignment.getNumberToVerify()
                                                + ", currentDivisor = " + taskAssignment.getCurrentDivisor()
                                                + ", lastDivisor = " + taskAssignment.getLastDivisor()
                                                + ")."
                                        );

                                        if (!sharedObject.putIntoTaskAssignmentQueue(taskAssignment)) {
                                            logger.error("Received task cannot be added to the queue.");
                                        }
                                        else {  // [ DFG3 ]
                                            currentPhase = Phases.START_JOB;
                                            currentState = States.ACTIVE;
                                        }
                                    }
                                }

                                // No node has a job
                                else {
                                    logger.debug2("No node has a job.");
                                    currentPhase = Phases.WAIT_FOR_MESSAGE;
                                }

                                forwardMessage = false;
                            }

                            break;
                        }   // case JOB


                        // ------------------------------------------------------------------[ TERMINATE ]----
                        case TERMINATE: {
                            Result finalResult = (Result) message.getBody();

							// A divisor was found on this node
                            if ((resultNodeName != null) && (result > 0)) {
                                finalResult.setResultNodeName(resultNodeName);
                                finalResult.setResult(result);
                            }
							// A divisor was not found on this node
                            else {
                                resultNodeName = finalResult.getResultNodeName();
                                result = finalResult.getResult();
                            }

                            currentPhase = Phases.TERMINATION;

                            if (message.getDestinationNodeName().equals(sharedObject.getNodeName())) {
                                forwardMessage = false;
                            }

                            break;
                        }   // case TERMINATE


                        // ----------------------------------------------------------------------[ TOKEN ]----
                        case TOKEN: {  // [ DFG6 ]
                            Colors ct = (Colors) message.getBody();

                            logger.trace(ct.name() + " token was received.");

                            tokenPresent = true;
                            tokenColor = ct;

                            if (sharedObject.isMasterNode()) {
                                if ((processColor == Colors.WHITE) &&
                                        (tokenColor == Colors.WHITE)) {

                                    terminationDetected = true;

                                    logger.major("Termination detected!");

                                    sendMessage(
                                            Message.Subjects.TERMINATE,
                                            sharedObject.getNodeName(),
                                            new Result(resultNodeName, result)
                                    );
                                }
                                else {
                                    tokenColor = Colors.WHITE;
                                }
                            }

                            forwardMessage = false;

                            break;
                        }   // case TOKEN


                        // --------------------------------------------------------------------[ DISJOIN ]----
                        case DISJOIN: {

                            // Circular has been sent by successor of this node
                            if ((message.getDestinationNodeName().equals(sharedObject.getNextNodeName()))) {
                                forwardMessage(message);
                                forwardMessage = false;

                                NodeProperties nodeProperties = (NodeProperties) message.getBody();
                                String nextNodeName = nodeProperties.getNextNodeName();

                                // Set legacy tasks
                                for (TaskAssignment taskAssignment : nodeProperties.getTaskAssignmentQueue()) {
                                    sharedObject.putIntoTaskAssignmentQueue(taskAssignment);
                                    logger.debug2("Legacy task received from node " + message.getSourceNodeName()
                                            + " (numberToVerify = " + taskAssignment.getNumberToVerify()
                                            + ", currentDivisor = " + taskAssignment.getCurrentDivisor()
                                            + ", lastDivisor = " + taskAssignment.getLastDivisor()
                                            + ")."
                                    );
                                }

                                // Set "Master node" attribute and nodeSeqNumber
                                if (! sharedObject.isMasterNode()) {
                                    sharedObject.setMasterNode(nodeProperties.getMasterNode());
                                    sharedObject.setNodeSeqNumber(nodeProperties.getNodeSeqNumber());
                                }

								// We have received legacy divisor - set it
								if ((nodeProperties.getResultNodeName() != null) && (nodeProperties.getResult() > 0)) {
                                    resultNodeName = nodeProperties.getResultNodeName();
                                    result = nodeProperties.getResult();
								}

                                // Successor is the penultimate node (the last except me)
                                if (nextNodeName.equals(sharedObject.getNodeName())) {
                                    sharedObject.setNextNodeName(null);
                                    sharedObject.setNextNodeRmiConnector(null);
                                }
                                else {
                                    sharedObject.setNextNodeName(nextNodeName);
                                    RmiConnector rmiConnector = ApplicationUtils.getRmiConnector(sharedObject, nextNodeName);

                                    if (rmiConnector != null) {
                                        sharedObject.setNextNodeRmiConnector(rmiConnector);
                                        logger.rmi("The ring is repaired. This: "
                                                + sharedObject.getNodeName()
                                                + " --> Next: "
                                                + nextNodeName);
                                    }
                                    else {
                                        logger.error("getRmiConnector(): Null response from node " + nextNodeName + ".");
                                    }
                                }
                            }

                            // Circular was sent by this node
                            else if ((message.getDestinationNodeName().equals(sharedObject.getNodeName()))) {
                                forwardMessage = false;

                                // Break main loop -> end
                                mainLoop = false;
                            }

                            break;
                        }   // case DISJOIN

                    }   // switch ()


                    // ------------------------------------------[ Forward message for someone else node ]----
                    if (forwardMessage) {
                        forwardMessage(message);
                    }

                }   // if (null)


                switch (currentPhase) { // ===============================================[ currentPhase ]====

                    // --------------------------------------------------------------[ CASE: ASK_FOR_JOB ]----
                    case ASK_FOR_JOB: {

                        // There is no job in the stack
                        if (sharedObject.peekAtTaskAssignmentQueue() == null) {

                            // There are more nodes (I've a neighbor) -> ask neighbor for job
                            if (sharedObject.getNextNodeRmiConnector() != null) {
                                sendMessage(
                                        Message.Subjects.JOB,
                                        sharedObject.getNodeName(),
                                        null
                                );
                                currentPhase = Phases.WAIT_FOR_MESSAGE;
                            }

                            // There is only one node (I'm alone) -> The end...
                            else {
                                currentPhase = Phases.TERMINATION;
                            }

                            break;
                        }

                        // There is some job in the stack
                        else {
                            currentPhase = Phases.START_JOB;
                            currentState = States.ACTIVE;       // DFG
                        }
                    }   // case ASK_FOR_JOB


                    // ----------------------------------------------------------------[ CASE: START_JOB ]----
                    case START_JOB: {
                        task = new Task(sharedObject.pollFromTaskAssignmentQueue(), sharedObject.getTaskSleep());
                        taskResult = executorService.submit(task);
                        currentPhase = Phases.WATCH_JOB;
                        break;
                    }   // case START_JOB


                    // ----------------------------------------------------------------[ CASE: WATCH_JOB ]----
                    case WATCH_JOB: {
                        if (taskResult != null) {

                            long r = (long) taskResult.get(TASK_BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS);

                            // A result wasn't found yet
                            if (r == 0) {
                                logger.info("Local task result: A divisor was not found.");

								//  We already have a result (probably as legacy)
								if (result > 0) {
									currentPhase = Phases.WAIT_FOR_MESSAGE;
								}
								// We still don't have a result
								else {
									result = r;
									currentPhase = Phases.ASK_FOR_JOB;	
								}
                            }

                            // A result was found
                            else {
                                logger.info("Local task result: Divisor was found (" + r + ").");

								//  We already have a result (probably as legacy)
								if (result > 0) {
									logger.error("Some error happened - two divisors exist! (" + result + ", " + r + ")");
								}
								// We still don't have a result
								else {
									result = r;
									resultNodeName = sharedObject.getNodeName();

									// There are more nodes (I've a neighbor)
									if (sharedObject.getNextNodeRmiConnector() != null) {
										currentPhase = Phases.WAIT_FOR_MESSAGE;
									}
									// There is only one node (I'm alone)
									else {
										currentPhase = Phases.TERMINATION;
									}
								}
                            }
						
						}   // if (null)

                        break;
                    }   // case WATCH_JOB


                    // ---------------------------------------------------------[ CASE: WAIT_FOR_MESSAGE ]----
                    case WAIT_FOR_MESSAGE: {

                        if (! terminationDetected) {    // Prevents sending tokens after termination is detected

                            if (currentState == States.ACTIVE) {    // [ DFG4 ]
                                currentState = States.PASSIVE;

                                if (sharedObject.isMasterNode()) {  // [ DFG2 ]
                                    tokenPresent = true;
                                    tokenColor = Colors.WHITE;
                                }
                            }

                            if ((tokenPresent) &&                   // [ DFG7 ]
                                    (currentState == States.PASSIVE)) {

                                if (processColor == Colors.BLACK) {
                                    logger.trace("Change token color: " + tokenColor.name() + " --> BLACK.");
                                    tokenColor = Colors.BLACK;
                                }

                                tokenPresent = false;
                                sendMessage(
                                        Message.Subjects.TOKEN,
                                        sharedObject.getNextNodeName(),
                                        tokenColor
                                );
                                processColor = Colors.WHITE;

                                logger.trace(tokenColor.name() + " token was sent.");
                            }
                        }

                        break;
                    }   // WAIT_FOR_MESSAGE


                    // --------------------------------------------------------------[ CASE: TERMINATION ]----
                    case TERMINATION: {

                        shutDownExecutorService(executorService);

                        if (result > 0) {
                            logger.major("Final taskResult: A result was found (" + result + ") on " + resultNodeName + ".");
                        }
                        else if (result == 0) {
                            logger.major("Final taskResult: A result was not found.");
                        }
                        else {
                            logger.error("Final taskResult: Some error happened (result = " + result + ")!");
                        }

                        // Break main loop -> end
                        mainLoop = false;

                        break;
                    }   // TERMINATION


                }   // switch()

            }   // try


            // Interrupt was caught ---------------------------------------------------------[ INTERRUPT ]----
            catch (InterruptedException e) {
                logger.signal();

                // There are more nodes (I've a neighbor)
                if (sharedObject.getNextNodeRmiConnector() != null) {

                    TaskAssignment taskAssignment = task.getLegacyTaskAssignment();
                    if (taskAssignment != null) {
                        sharedObject.putIntoTaskAssignmentQueue(taskAssignment);
                    }

                    shutDownExecutorService(executorService);

                    sendMessage(
                            Message.Subjects.DISJOIN,
                            sharedObject.getNodeName(),
                            new NodeProperties(
                                    sharedObject.getNextNodeName(),
                                    sharedObject.getTaskAssignmentQueue(),
                                    sharedObject.isMasterNode(),
                                    resultNodeName,
                                    result,
                                    sharedObject.getNodeSeqNumber()
                            )
                    );

                    currentPhase = Phases.WAIT_FOR_MESSAGE;
                }

                // There is only one node (I'm alone)
                else {
                    shutDownExecutorService(executorService);

                    // Break main loop -> end
                    mainLoop = false;
                }
            }

            // We swallow TimeoutException
            catch (TimeoutException e) {
                // This catch block is intentionally left blank.
            }

            // If the computation threw an exception
            catch (ExecutionException e) {
                logger.info("ExecutionException: " + e.getMessage());
            }

        }   // while (mainLoop)

    }   // start()

}   // class StateMachine
