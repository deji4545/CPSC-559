import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//A udp client that takes user input using the command line interface
//Those input are add to a queue 
//That will be accessed by the PeerClient object to be sent to all peers in the system
class SnipUser extends Thread {

    private ArrayBlockingQueue<String> snipSendQueue;

    private AtomicBoolean endThread;

    public SnipUser(ArrayBlockingQueue<String> snipSendQueue, AtomicBoolean endThread) throws IOException {
        this.snipSendQueue = snipSendQueue;

        this.endThread = endThread;
    }

    public void run() {

        Scanner sc = new Scanner(System.in);

        while (true) {
            String inp = sc.nextLine();

            if (endThread.get()) {
                break;
            }

            snipSendQueue.add(inp);

        }

        sc.close();
    }

}
