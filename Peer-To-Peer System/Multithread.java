import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//REFERENCE -  Java code for creating threads
//https://www.geeksforgeeks.org/multithreading-in-java/

// Object that runs multiple threads at once
public class Multithread {

	// Threads safe object that will be shared among threads
	private int capacity = 1000;
	private ArrayBlockingQueue<String> peerLocationQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<String> snipQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<String> snipSendQueue = new ArrayBlockingQueue<String>(capacity);
	private AtomicInteger currentTimeStamp = new AtomicInteger(0);
	private AtomicBoolean endThread = new AtomicBoolean(false); // Ends loop using boolean flag
	private ConcurrentMap<String, LocalDateTime> peerInactiveTimeMap = new ConcurrentHashMap<String, LocalDateTime>();
	private ArrayBlockingQueue<String> ackQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<Peer> peerQueue = new ArrayBlockingQueue<Peer>(capacity);
	private ArrayBlockingQueue<Peer> ackPeerQueue = new ArrayBlockingQueue<Peer>(capacity);

	// Thread safe object shared with other threads that store data to be reported

	private ArrayBlockingQueue<String> snipReportQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<String> sendingReportQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<String> receivingReportQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<String[]> catchMessageQueue = new ArrayBlockingQueue<String[]>(capacity);;
	private ArrayBlockingQueue<String> ackReportQueue = new ArrayBlockingQueue<String>(capacity);
	private ArrayBlockingQueue<Peer> peerReportQueue = new ArrayBlockingQueue<Peer>(capacity);

	public void startThread(int portNumber, String teamName) throws IOException {
		// Object to receive messages from other peers
		PeerServer udpPeerServer = new PeerServer(portNumber, peerLocationQueue, snipQueue, endThread,
				receivingReportQueue, peerInactiveTimeMap, teamName, ackQueue, ackReportQueue, ackPeerQueue);

		// Object responsible for sending messages to all peers
		PeerClient udpPeerClient = new PeerClient(peerLocationQueue, portNumber, snipSendQueue, currentTimeStamp,
				endThread, sendingReportQueue, peerInactiveTimeMap, catchMessageQueue, teamName,
				peerReportQueue, peerQueue, ackPeerQueue);

		// Object to display snip messsage that are received
		Snip snip = new Snip(snipQueue, currentTimeStamp, endThread, snipReportQueue, catchMessageQueue);

		// Object to take user input using the command line interface
		SnipUser udpSnipUser = new SnipUser(snipSendQueue, endThread);

		SnipClient udpSnipClient = new SnipClient(snipSendQueue, currentTimeStamp, endThread, peerQueue, ackQueue);

		// All 4 threads are being run
		udpPeerServer.start();
		udpPeerClient.start();
		snip.start();
		udpSnipUser.start();
		udpSnipClient.start();

		while (true) {
			if (endThread.get() == true) {
				System.out.println("\nAll Thread Stopped");
				break;
			}
		}

	}

	// Iterates through queues shared with threads to generate report
	public String generateReport(List<String> initialPeerList, String registryInfo) {

		String report = "";
		report = report + peerReportQueue.size() + "\n";

		for (Peer peer : peerReportQueue) {
			String aliveness = "";
			if (peer.getMissingAck() == true) {
				aliveness = "missing_ack";
			} else if (peer.getAliveStatus() == true) {
				aliveness = "alive";
			} else {
				aliveness = "silent";
			}
			report = report + peer.getAddress() + ":" + peer.getPort() + " " + aliveness + "\n";
		}

		report = report + registryInfo + "\n" + initialPeerList.size() + "\n";

		for (String peer : initialPeerList) {
			report = report + peer + "\n";
		}

		report = report + receivingReportQueue.size() + "\n";
		for (String peer : receivingReportQueue) {
			report = report + peer + "\n";
		}

		report = report + sendingReportQueue.size() + "\n";

		for (String peer : sendingReportQueue) {
			report = report + peer + "\n";
		}

		report = report + snipReportQueue.size() + "\n";

		for (String snipMessage : snipReportQueue) {
			report = report + snipMessage + "\n";
		}

		report = report + ackReportQueue.size() + "\n";

		for (String ack : ackReportQueue) {
			report = report + ack + "\n";
		}

		return report;
	}

	// Adds a peer location to the peer location queue
	public void addToPeerQueue(String peerLocationMessage) {
		peerLocationQueue.add(peerLocationMessage);
	}

}
