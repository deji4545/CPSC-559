import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//A udp client that takes user input using the command line interface
//Those input are add to a queue by the Snip Class
//That will be accessed by this SnipClient class to be sent to all peers in the system
class SnipClient extends Thread {

    private ArrayBlockingQueue<String> snipSendQueue;
    private AtomicBoolean endThread;
    private AtomicInteger currentTimeStamp;
    private List<Peer> peerList;
    private ArrayBlockingQueue<Peer> peerQueue;
    private ArrayBlockingQueue<String> ackQueue;
    private Client sender;

    public SnipClient(ArrayBlockingQueue<String> snipSendQueue, AtomicInteger currentTimeStamp,
            AtomicBoolean endThread, ArrayBlockingQueue<Peer> peerQueue, ArrayBlockingQueue<String> ackQueue)
            throws IOException {
        this.snipSendQueue = snipSendQueue;
        this.currentTimeStamp = currentTimeStamp;
        this.endThread = endThread;
        this.peerQueue = peerQueue;
        this.peerList = new ArrayList<Peer>();
        this.sender = new Client();
        this.ackQueue = ackQueue;
    }

    // Iterates through peer list and send snip messages from the user
    public void run() {
        while (endThread.get() == false) {
            try {
                sendUserSnipMessage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void sendUserSnipMessage() throws IOException {
        while (endThread.get() == false) {

            int peerQueueSize = peerQueue.size();

            for (int i = 0; i < peerQueueSize; i++) {
                peerList.add(peerQueue.remove());
            }

            if (!snipSendQueue.isEmpty()) {

                String mySnipMessage = snipSendQueue.remove();
                int timeStamp = currentTimeStamp.getAndAdd(1) + 1;
                String snipInp = "snip" + timeStamp + " " + mySnipMessage;

                for (int i = 0; i < peerList.size(); i++) {

                    if (peerList.get(i).getAliveStatus() == true && peerList.get(i).getMissingAck() == false) {
                        Peer thePeer = peerList.get(i);
                        String ip_Address_String = thePeer.getAddress();
                        int portNum = thePeer.getPort();
                        ackQueue.clear();

                        // Waits for an ack for maximum of 30 seconds
                        // Then the peer is marked as inactive and missing_ack
                        for (int j = 0; j < 3; j++) {

                            // Sender object sends user entered snippets to a peer in the system
                            // Resend snippets if ack has not been received
                            sender.sendPacket(snipInp, ip_Address_String, portNum);

                            Integer ackTimeStamp = timeStamp;

                            // Wait for an ack after a snippet is sent for 10 seconds
                            if (waitForAck(ackTimeStamp.toString()) == true) {
                                // System.out.println(thePeer.getAddress() + ":" + thePeer.getPort() + " ack
                                // received");
                                break;
                            }

                            // After three times of no ack received, peer is labelled inactive
                            if (j == 2) {
                                System.out.println(thePeer.getAddress() + ":" + thePeer.getPort() + " is missing ack");
                                thePeer.setMissingAck(true);
                            }

                        }
                    }
                }

                System.out.println(mySnipMessage + " has been delivered to all peers in the list");

            }

        }
    }

    // Continously checks ackQueue for 10 seconds for the correct timestamp
    public boolean waitForAck(String timeStamp) {

        LocalDateTime initialTimeNow = LocalDateTime.now();
        String ackTimeStamp;
        long duration = 0;

        while (duration < 10) {

            if (ackQueue.size() > 0) {
                ackTimeStamp = ackQueue.remove();

                if (ackTimeStamp.equals(timeStamp)) {
                    return true;
                }
            }

            duration = ChronoUnit.SECONDS.between(initialTimeNow, LocalDateTime.now());

        }

        return false;
    }

}
