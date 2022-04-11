import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

// REFRENCE - Java code for udp Server side
// https://www.geeksforgeeks.org/working-udp-datagramsockets-java/

// udp server to receives messages like stop, peer or snip
public class PeerServer extends Thread {

    private int portNumber;
    private ArrayBlockingQueue<String> peerLocationQueue;
    private ArrayBlockingQueue<String> snipQueue;
    private AtomicBoolean endThread;
    private ArrayBlockingQueue<String> receivingReportQueue;
    private ArrayBlockingQueue<String> ackQueue;
    private DateTimeFormatter dtf;
    private boolean isFullQueue;
    private ConcurrentMap<String, LocalDateTime> peerInactiveTimeMap;
    private String teamName;
    private Client client;
    private ArrayBlockingQueue<String> ackReportQueue;
    private ArrayBlockingQueue<Peer> peerQueue;
    private List<Peer> peerList;
    private boolean isAckReportFullQueue;

    public PeerServer(int portNum, ArrayBlockingQueue<String> peerLocationQueue, ArrayBlockingQueue<String> snipQueue,
            AtomicBoolean endThread, ArrayBlockingQueue<String> receivingReportQueue,
            ConcurrentMap<String, LocalDateTime> peerInactiveTimeMap, String teamName,
            ArrayBlockingQueue<String> ackQueue, ArrayBlockingQueue<String> ackReportQueue,
            ArrayBlockingQueue<Peer> peerQueue) throws IOException {
        this.portNumber = portNum;
        this.peerLocationQueue = peerLocationQueue;
        this.snipQueue = snipQueue;
        this.endThread = endThread;
        this.receivingReportQueue = receivingReportQueue;
        this.isFullQueue = false;
        this.dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.peerInactiveTimeMap = peerInactiveTimeMap;
        this.client = new Client();
        this.teamName = teamName;
        this.ackQueue = ackQueue;
        this.ackReportQueue = ackReportQueue;
        this.peerList = new ArrayList<Peer>();
        this.peerQueue = peerQueue;
        this.isAckReportFullQueue = false;
    }

    // Start Thread
    public void run() {
        try {
            startServer();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // REFERENCE - How To Create UDP Server
    // https://www.geeksforgeeks.org/working-udp-datagramsockets-java/

    // Start UDP Server to receive messages
    public void startServer() throws IOException, InterruptedException {

        // Step 1 : Create a socket to listen at port number
        DatagramSocket ds = new DatagramSocket(portNumber);

        byte[] receive = new byte[65535];

        DatagramPacket DpReceive = null;
        // System.out.println("Start Server: " + ds.getPort());

        String localHost = InetAddress.getLocalHost().toString().split("/")[1];

        while (true) {

            // Step 2 : create a DatgramPacket to receive the data.
            DpReceive = new DatagramPacket(receive, receive.length);

            // Step 3 : revieve the data in byte buffer.
            ds.receive(DpReceive);

            // Exit the server if the client sends "stop"
            if (data(receive).toString().equals("stop")) {
                String exitAddress = DpReceive.getAddress().toString().substring(1);
                if (exitAddress.equals("136.159.5.22") || exitAddress.equals(localHost)) {

                    // Send ack message
                    String inp = "ack" + teamName;
                    client.sendPacket(inp, exitAddress, DpReceive.getPort());
                    endThread.set(true);
                    break;
                }
            }

            // Build our peer list
            int peerQueueSize = peerQueue.size();
            for (int i = 0; i < peerQueueSize; i++) {
                peerList.add(peerQueue.remove());
            }

            // Parsing the message
            String message = data(receive).toString();
            String firstFourChars = message.substring(0, 4);
            message = message.substring(4);

            // Address and Port of who sends any udp messages
            String recevingAddress = DpReceive.getAddress().toString().substring(1);
            int recevingPortNumber = DpReceive.getPort();

            // Decides what do with message based on first 4 characters
            switch (firstFourChars) {

                case "snip":

                    message = message + " " + recevingAddress + ":" + recevingPortNumber;

                    try {
                        snipQueue.add(message);
                    } catch (IllegalStateException e) {
                        while (!snipQueue.isEmpty()) {
                        }
                        snipQueue.add(message);
                    }

                    // Send ack to indicate message received
                    String inp = "ack " + message.split(" ", 2)[0];
                    for (Peer peer : peerList) {
                        if (recevingAddress.equals(peer.getAddress()) && peer.getAliveStatus()) {
                            client.sendPacket(inp, recevingAddress, peer.getPort());
                        }
                    }
                    break;

                case "peer":

                    String currentTime = dtf.format(LocalDateTime.now());

                    String receivingPeerMessage = recevingAddress + ":" + recevingPortNumber + " "
                            + message + " " + currentTime;

                    if (isFullQueue == false) {
                        try {
                            receivingReportQueue.add(receivingPeerMessage);
                        } catch (IllegalStateException e) {
                            isFullQueue = true;
                        }
                    }

                    try {
                        peerInactiveTimeMap.put(message, LocalDateTime.now());
                        peerLocationQueue.add(message);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        while (!peerLocationQueue.isEmpty()) {
                        }
                        peerLocationQueue.add(message);
                    }

                    // System.out.println("Peer Added: " + message);
                    break;

                case "ctch":
                    try {
                        snipQueue.add("ctch" + message);
                    } catch (IllegalStateException e) {
                        while (!snipQueue.isEmpty()) {
                        }
                        snipQueue.add("ctch" + message);
                    }
                    break;

                case "ack ":
                    ackQueue.add(message);

                    if (isAckReportFullQueue == false) {
                        try {
                            ackReportQueue.add(message + " " + recevingAddress + ":" + recevingPortNumber);
                        } catch (IllegalStateException e) {
                            isAckReportFullQueue = true;
                        }
                    }

                default:
                    break;
            }

            // Clear the buffer after every message.
            receive = new byte[65535];
        }

        ds.close();

    }

    // A utility method to convert the byte array
    // data into a string representation.
    public static StringBuilder data(byte[] a) {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0) {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

}
