import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

// Responsible for continously sending peerLocation and snippets message to other peers
public class PeerClient extends Thread {

    private boolean isFullQueue;
    private List<Peer> peerList; // list of pair location
    private List<String> peerStringList;
    private Client sender;
    private ArrayBlockingQueue<String> peerLocationQueue;
    private String inp;
    private AtomicBoolean endThread;
    private DateTimeFormatter dtf;
    private ArrayBlockingQueue<String> sendingReportQueue;
    private ConcurrentMap<String, LocalDateTime> peerInactiveTimeMap;
    private List<String[]> snipCatchList;
    private ArrayBlockingQueue<String[]> snipCatchQueue;
    private ArrayBlockingQueue<Peer> peerReportQueue;
    private ArrayBlockingQueue<Peer> peerQueue;
    private ArrayBlockingQueue<Peer> ackPeerQueue;

    public PeerClient(ArrayBlockingQueue<String> peerLocationQueue, int myPortNumber,
            ArrayBlockingQueue<String> snipSendQueue, AtomicInteger currentTimeStamp,
            AtomicBoolean endThread, ArrayBlockingQueue<String> sendingReportQueue,
            ConcurrentMap<String, LocalDateTime> peerInactiveTimeMap,
            ArrayBlockingQueue<String[]> snipCatchQueue, String teamName, ArrayBlockingQueue<Peer> peerReportQueue,
            ArrayBlockingQueue<Peer> peerQueue, ArrayBlockingQueue<Peer> ackPeerQueue)
            throws IOException {

        // Initializing variables
        sender = new Client();
        peerStringList = new ArrayList<String>();
        dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        peerList = new ArrayList<Peer>();
        peerStringList = new ArrayList<String>();
        this.isFullQueue = false;
        this.snipCatchList = new ArrayList<String[]>();
        this.snipCatchQueue = snipCatchQueue;

        this.peerLocationQueue = peerLocationQueue;
        this.endThread = endThread;
        this.sendingReportQueue = sendingReportQueue;
        this.peerInactiveTimeMap = peerInactiveTimeMap;
        this.peerReportQueue = peerReportQueue;
        this.peerQueue = peerQueue;
        this.ackPeerQueue = ackPeerQueue;

        // My peer location
        InetAddress local = InetAddress.getLocalHost();
        String localHost = local.toString().split("/")[1];
        inp = "peer" + localHost + ":" + Integer.toString(myPortNumber);

    }

    // Start threads - build list peer, send peer message
    public void run() {
        while (endThread.get() == false) {
            try {
                buildPeerLocationList();
                sendPeerMessages();
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Build peer location list
    public void buildPeerLocationList() throws IOException {
        int current_size_queue = peerLocationQueue.size();

        for (int i = 1; i < current_size_queue; i++) {

            String peerStringLocation = peerLocationQueue.remove();
            boolean isInPeerList = peerStringList.contains(peerStringLocation);

            if (isInPeerList == false) {

                String[] arrOfStr;
                String ip_address;
                int portNumber;

                try {
                    // Split the peer locations string to extract both the IP and Port Number
                    arrOfStr = peerStringLocation.split(":");
                    ip_address = arrOfStr[0];
                    portNumber = Integer.parseInt(arrOfStr[1]);

                    // Catch error for wrongly informatted ip address and port number
                    sender.sendPacket(inp, ip_address, portNumber);
                } catch (Exception e) {
                    // e.printStackTrace();
                    continue;
                }

                // Add peer location to a Peer List
                Peer peerLocation = new Peer(ip_address, portNumber);
                peerList.add(peerLocation);
                peerStringList.add(peerStringLocation);
                peerReportQueue.add(peerLocation);
                peerQueue.add(peerLocation);
                ackPeerQueue.add(peerLocation);
                System.out.println("Peer Added: " + peerStringLocation);
                buildCatchSnipMessageList();
                sendCatchSnipMessages(peerStringList.size() - 1);
            } else {
                // Re-activate the peer in the peer list
                int peerIndex = peerStringList.indexOf(peerStringLocation);

                if (peerList.get(peerIndex).getAliveStatus() == false
                        && peerList.get(peerIndex).getMissingAck() == false) {
                    buildCatchSnipMessageList();
                    sendCatchSnipMessages(peerIndex);
                    peerList.get(peerIndex).setAliveStatus(true);
                    System.out.println(peerStringList.get(peerIndex) + " is alive");
                }

            }
        }
    }

    // Pick a random peer and send that location to every peer in the peer list
    public void sendPeerMessages() throws IOException {

        Random rand = new Random();
        long duration = 0;

        // Pick a random peer
        int rand_int = rand.nextInt(peerStringList.size());

        Peer thePeer = peerList.get(rand_int);

        peerList.get(0).setAliveStatus(true);

        // Continously picks random peers until one of them is active
        // Stops after a couple of turns
        for (int i = 0; i < 5; i++) {
            if (thePeer.getAliveStatus() == false) {
                rand_int = rand.nextInt(peerStringList.size());
                thePeer = peerList.get(rand_int);
            } else {
                break;
            }
        }

        String peerString = peerStringList.get(rand_int);

        String peerInp = "peer" + peerString;

        if (thePeer.getAliveStatus()) {

            // Checks if peer is still active
            LocalDateTime peerInactiveTime = peerInactiveTimeMap.get(peerString);
            if (peerInactiveTime == null) {
                peerInactiveTimeMap.put(peerString, LocalDateTime.now());
                duration = 0;
            } else {
                LocalDateTime timeNow = LocalDateTime.now();
                duration = ChronoUnit.SECONDS.between(peerInactiveTime, timeNow);
            }
            // If we exceed the duration we do not send a peer message to that peer
            if (duration < 300 || rand_int == 0) {

                // If the random peer location is still alive send peer messages
                for (int j = 1; j < peerStringList.size(); j++) {

                    String ip_Address_String = peerList.get(j).getAddress();
                    int portNum = peerList.get(j).getPort();

                    // Sender object used to send a peer locations
                    sender.sendPacket(peerInp, ip_Address_String, portNum);

                    // Time that peer was sent
                    String currentTime = dtf.format(LocalDateTime.now());

                    String sendingPeerMessage = peerStringList.get(j) + " " + peerStringList.get(rand_int) + " "
                            + currentTime;

                    if (isFullQueue == false) {
                        try {
                            sendingReportQueue.add(sendingPeerMessage);
                        } catch (IllegalStateException e) {
                            isFullQueue = true;
                        }
                    }
                }
            } else {
                System.out.println(peerString + " is not alive");
                thePeer.setAliveStatus(false);
            }

        }

    }

    // Resend catchup snip messages to reactivated peers
    public void sendCatchSnipMessages(int index) throws IOException {

        String ip_Address_String = peerList.get(index).getAddress();
        int portNum = peerList.get(index).getPort();

        for (int i = 0; i < snipCatchList.size(); i++) {
            String[] catchMessage = snipCatchList.get(i);
            String originalSender = catchMessage[0];
            String timeStamp = catchMessage[1];
            String content = catchMessage[2];

            String snipInp = "ctch" + originalSender + " " + timeStamp + " " + content;

            sender.sendPacket(snipInp, ip_Address_String, portNum);

        }

    }

    // Build a catchup message list that will be sent to reactivate peers
    public void buildCatchSnipMessageList() {
        int snipCatchQueueSize = snipCatchQueue.size();

        for (int i = 0; i < snipCatchQueueSize; i++) {
            String[] thePeerMessage = snipCatchQueue.remove();
            snipCatchList.add(thePeerMessage);
        }
    }

}
