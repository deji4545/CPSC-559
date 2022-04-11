import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

// REFERENCE - Client side of udp. Send message to destination
// https://www.geeksforgeeks.org/working-udp-datagramsockets-java/

class Client {
    private DatagramSocket ds;

    public Client() throws IOException {
        ds = new DatagramSocket();
    }

    public void sendPacket(String inp, String ip_Address_String, int portNum) throws IOException {

        // Step 1:Create the socket object for
        // carrying the data.
        InetAddress ip = InetAddress.getByName(ip_Address_String);
        // InetAddress ip = InetAddress.getLocalHost();
        byte buf[] = null;

        // convert the String input into the byte array.
        buf = inp.getBytes();

        // Step 2 : Create the datagramPacket for sending
        // the data.
        DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, portNum);

        // Step 3 : invoke the send call to actually send
        // the data.
        ds.send(DpSend);

    }

}