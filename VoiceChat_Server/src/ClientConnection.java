
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
/**
 * this thread manages a connection with a client. it does a lot of stuff: -read
 * messages from client -add said messages to the server's broadcast queue
 * -receive messages from the server thread -add said messages to a send queue
 * -send messages from said queue to the client (or throw them away if too old)
 *
 * @author dosse
 */
public class ClientConnection extends Thread {

    private Server serv; //instance of server, needed to put messages in the server's broadcast queue
    private Socket s; //connection to client 
    private ObjectInputStream in; //object streams to/from client
    private ObjectOutputStream out;
    private long chId; //unique id of this client, generated in the costructor
    private ArrayList<Message> toSend = new ArrayList<Message>(); //queue of messages to be sent to the client

    public InetAddress getInetAddress() { //returns this client's ip address
        return s.getInetAddress();
    }

    public int getPort() { //returns this client's tcp port
        return s.getPort();
    }

    public long getChId() { //return this client's unique id
        return chId;
    }

    public ClientConnection(Server serv, Socket s) {
        this.serv = serv;
        this.s = s;
        byte[] addr = s.getInetAddress().getAddress();
        chId = (addr[0] << 48 | addr[1] << 32 | addr[2] << 24 | addr[3] << 16) + s.getPort(); //generate unique chId from client's IP and port
    }

    public void addToQueue(Message m) { //add a message to send to the client
        try {
            toSend.add(m);
        } catch (Throwable t) {
            //mutex error, ignore because the server must be as fast as possible
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(s.getOutputStream()); //create object streams to/from client
            in = new ObjectInputStream(s.getInputStream());
        } catch (IOException ex) { //connection error, close connection
            try {
                s.close();
                Log.add("ERROR " + getInetAddress() + ":" + getPort() + " " + ex);
            } catch (IOException ex1) {
            }
            stop();
        }
        for (;;) {
            try {
                if (s.getInputStream().available() > 0) { //we got something from the client
                    Message toBroadcast = (Message) in.readObject(); //read data from client
                    if (toBroadcast.getChId() == -1) { //set its chId and timestamp and pass it to the server
                        toBroadcast.setChId(chId);
                        toBroadcast.setTimestamp(System.nanoTime() / 1000000L);
                        serv.addToBroadcastQueue(toBroadcast);
                    } else {
                        continue; //invalid message
                    }
                }
                try {
                    if (!toSend.isEmpty()) {
                        Message toClient = toSend.get(0); //we got something to send to the client
                        if (!(toClient.getData() instanceof SoundPacket) || toClient.getTimestamp() + toClient.getTtl() < System.nanoTime() / 1000000L) { //is the message too old or of an unknown type?
                            Log.add("dropping packet from " + toClient.getChId() + " to " + chId);
                            continue;
                        }
                        out.writeObject(toClient); //send the message
                        toSend.remove(toClient); //and remove it from the queue
                    } else {
                        Utils.sleep(10); //avoid busy wait
                    }
                } catch (Throwable t) {
                    if (t instanceof IOException) {//connection closed or connection error
                        throw (Exception) t;
                    } else {//mutex error, try again
                        System.out.println("cc fixmutex");
                        continue;
                    }
                }
            } catch (Exception ex) { //connection closed or connection error, kill thread
                try {
                    s.close();
                } catch (IOException ex1) {
                }
                stop();
            }
        }

    }
}
