
//Student Name: Ayodeji Osho
//Course: CPSC559

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Iter1 {
    private Socket sock;
    private String sourceCode = "";
    private String teamName;
    private String udpPortNumber;
    private List<String> initialPeerList;
    private String report;

    // Initializing variables
    public Iter1(String firstName, String lastName, String host, String udpPortNum, String report,
            List<String> initialPeerList)
            throws IOException {
        // Connect to registry
        sock = new Socket(host, 12955);
        teamName = firstName + "_" + lastName;
        udpPortNumber = udpPortNum;
        this.initialPeerList = initialPeerList;
        this.report = report;
    }

    // REFERENCE - How to do socket programming
    // https://www.baeldung.com/a-guide-to-java-sockets

    // Send and Receive data through the socket connection
    public void start() throws IOException {

        readSourceCode();

        // Socket Reader
        BufferedReader bf = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        // Socket Writer
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

        Boolean bool = true;
        String peerList = "";

        // Loop for incoming request
        while (bool) {
            String getRequest = bf.readLine();

            // Switch case responds to different request
            switch (getRequest) {

                case "get team name":
                    System.out.println("Send Name\n");
                    // pr.println(teamName + "\n");
                    out.write(teamName + "\n");
                    break;

                case "get report":
                    System.out.println("Sending Report\n");
                    out.write(report);
                    break;

                case "get code":
                    System.out.println("Sending Code\n");
                    String theCode = "Java\n" + sourceCode + "...\n";
                    // pr.println(theCode);
                    out.write(theCode);
                    break;

                case "close":
                    System.out.println("Closing Connection\n");
                    bool = false; // end loop
                    break;

                case "receive peers":
                    System.out.println("Receiving peers ");
                    peerList = "";

                    getRequest = bf.readLine();
                    peerList = peerList + getRequest + "\n";
                    int numPeers = Integer.parseInt(getRequest);
                    // Loop until all peers are received
                    for (int i = 0; i < numPeers; i++) {
                        getRequest = bf.readLine();
                        initialPeerList.add(getRequest);
                        peerList = peerList + getRequest + "\n";
                    }
                    System.out.println(peerList);
                    break;

                case "get location":
                    System.out.println(getRequest + "\n");
                    InetAddress local = InetAddress.getLocalHost();
                    String localHost = local.toString().split("/")[1];
                    String myLocation = localHost + ":" + udpPortNumber + "\n";
                    System.out.println(myLocation);
                    out.write(myLocation);
                    break;
                default:
                    System.out.println("default = " + getRequest);
            }

            out.flush();

        }

        sock.close();
    }

    // Reference - Read Files
    // https://www.w3schools.com/java/java_files_read.asp

    // Read all java text files used for Iteration 2
    public void readSourceCode() {

        String[] fileNamesArr = { "Client.java", "Iter1.java", "Iter2.java", "Multithread.java",
                "Peer.java", "PeerClient.java", "PeerServer.java", "Snip.java", "SnipUser.java", "SnipClient.java" };

        for (String fileName : fileNamesArr) {
            try {
                File myObj = new File(fileName);
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    sourceCode = sourceCode + myReader.nextLine() + "\n";
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }

    }
}