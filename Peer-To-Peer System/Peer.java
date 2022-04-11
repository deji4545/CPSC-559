import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Create a Peer object to represent IP, Port location and aliveness of the peer
public class Peer {

    String address;
    private AtomicInteger port;
    private AtomicBoolean isAlive;
    private AtomicBoolean isMissingAck;

    public Peer(String address, int port) {
        this.address = address;
        this.port = new AtomicInteger(port);
        this.isAlive = new AtomicBoolean(true);
        this.isMissingAck = new AtomicBoolean(false);
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port.get();
    }

    public boolean getAliveStatus() {
        return isAlive.get();
    }

    public void setAliveStatus(boolean isAlive) {
        this.isAlive.set(isAlive);
    }

    public boolean getMissingAck() {
        return isMissingAck.get();
    }

    public void setMissingAck(boolean MissingAck) {
        this.isMissingAck.set(MissingAck);
        if (MissingAck == true) {
            isAlive.set(false);
        }
    }

}