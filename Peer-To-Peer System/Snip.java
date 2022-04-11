import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//Display snip message that are received either as a snip or catch up message
//Update the timestamp using lamport algorithm
public class Snip extends Thread {
    private ArrayBlockingQueue<String> snipQueue;
    private ArrayBlockingQueue<String> snipReportQueue;
    private AtomicInteger currentTimeStamp;
    private AtomicBoolean endThread;
    private boolean isFullQueue;
    private ArrayBlockingQueue<String[]> snipCatchQueue;
    private List<String> snipMessageList;

    // Initalizing variables using constructor
    public Snip(ArrayBlockingQueue<String> snipQueue, AtomicInteger currentTimeStamp, AtomicBoolean endThread,
            ArrayBlockingQueue<String> snipReportQueue,
            ArrayBlockingQueue<String[]> snipCatchQueue) {
        this.snipQueue = snipQueue;
        this.currentTimeStamp = currentTimeStamp;
        this.endThread = endThread;
        this.snipReportQueue = snipReportQueue;
        this.snipCatchQueue = snipCatchQueue;
        this.snipMessageList = new ArrayList<String>();
        this.isFullQueue = false;
    }

    // Start Thread
    public void run() {
        while (endThread.get() == false) {
            try {
                display();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Display Message in snipQueue that has been received by the Peer Server thread
    public void display() throws InterruptedException {

        while (!snipQueue.isEmpty() && endThread.get() == false) {

            // Removes the snippets sent by the peer from queue
            String message = snipQueue.remove();
            String[] catchMessageArr = {};

            // Continue to next loop if I already have the message
            if (snipMessageList.contains(message)) {
                continue;
            }

            // Parse message if it is a catchup message
            if (message.substring(0, 4).equals("ctch")) {

                message = message.substring(4);
                String catchMessage = message;
                String[] arrOfCatchMessage = catchMessage.split(" ", 3);
                String originalSender = arrOfCatchMessage[0];
                String timeStamp = arrOfCatchMessage[1];
                String content = arrOfCatchMessage[2];

                if (content.length() > 50) {
                    content = content.substring(0, 50);
                }

                message = timeStamp + " " + content + " " + originalSender;

                // Skips the loop if I already have the catch up message
                if (snipMessageList.contains(message)) {
                    continue;
                }

                // The catch up message has been splitted up and stored as an array
                String[] catchArr = { originalSender, timeStamp, content };
                catchMessageArr = catchArr;

            } else {
                // Split the snip message up into original sender, timestamp and content
                String timeStamp = message.split(" ", 2)[0];
                String originalSender = message.substring(message.lastIndexOf(" ") + 1);
                String contentString = message.split(" ", 2)[1];
                contentString = contentString.substring(0, contentString.lastIndexOf(" "));

                // The catch up message array will be used to send catchup message
                String[] catchArr = { originalSender, timeStamp, contentString };
                catchMessageArr = catchArr;
            }

            // Parse snip queue message
            String[] arrOfStr = message.split(" ", 2);
            int peerTimeStamp = Integer.parseInt(arrOfStr[0]);
            String content = arrOfStr[1];
            if (content.length() > 50) {
                content = content.substring(0, 50);
            }

            // Updates time stamp
            int myTimeStamp = currentTimeStamp.get();
            int newTimeStamp = Integer.max(myTimeStamp, peerTimeStamp);
            currentTimeStamp.set(newTimeStamp);

            // Print the snip message
            String snipMessage = newTimeStamp + " " + content;
            System.out.println(snipMessage);

            // Add snip message to a list
            snipMessageList.add(snipMessage);

            if (isFullQueue == false) {
                try {
                    // Add snip message to a queue that will be in report
                    snipReportQueue.add(snipMessage);
                } catch (IllegalStateException e) {
                    isFullQueue = true;
                }
            }

            try {
                // Catch Up Message Array is added to a queue
                // Another thread will send those catch up messages to new or reactivated peers
                snipCatchQueue.add(catchMessageArr);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

        }
    }

}
