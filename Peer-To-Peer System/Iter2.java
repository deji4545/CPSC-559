
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Iter2 {

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Invalid Arguments");
            System.exit(0);
        }

        String report = "";

        List<String> initialPeerList = new ArrayList<String>();

        // Creating Iteration 1 object that connects to registry
        Iter1 registryConnect = new Iter1(args[0], args[1], args[2], args[3], report, initialPeerList);
        String teamName = args[0] + "_" + args[1];
        System.out.println(teamName);
        // Start the first connection with the registry
        registryConnect.start();

        // Time of connection with registry
        // Reference - //https://www.javatpoint.com/java-get-current-date
        // Date Formatting
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = dtf.format(LocalDateTime.now());
        String registryInfo = "1\n136.159.5.22:12955\n" + currentTime;

        // Creating the UDP Multithread Object
        Multithread udpPeerThread = new Multithread();

        // Retrieve localhost address and port number
        int portNumber = Integer.parseInt(args[3]);
        InetAddress local = InetAddress.getLocalHost();
        String localHost = local.toString().split("/")[1];
        udpPeerThread.addToPeerQueue(localHost + ":" + portNumber);

        // Adding Initial Peer List from Registry to the UDP Peer Queue
        for (int i = 0; i < initialPeerList.size(); i++) {
            udpPeerThread.addToPeerQueue(initialPeerList.get(i));
        }

        // Run all threads
        udpPeerThread.startThread(portNumber, teamName);

        System.out.println("Restarting Connection with the registry:\n");

        // Generate reports
        report = udpPeerThread.generateReport(initialPeerList, registryInfo);

        // Second connection with registry
        Iter1 registryConnect2 = new Iter1(args[0], args[1], args[2], args[3],
                report, initialPeerList);

        registryConnect2.start();

        System.exit(0);

    }

}
