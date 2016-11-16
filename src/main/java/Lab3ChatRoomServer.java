import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;


/*
 * A chat server that delivers public and private messages.
 */
public class Lab3ChatRoomServer {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String args[]) {

        // set port number.
        int portNumber = 2222;
        if(args.length < 1) {
            System.out.println("Now using default configuration portNumber=" + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }


        try {
            serverSocket = new ServerSocket(portNumber);
        } catch(IOException e) {
            System.out.println(e);
        }


        //Create a client socket for each connection and pass it to a new client thread.
        while(true) {
            try {
                clientSocket = serverSocket.accept();
                int i = 0;
                for(i = 0; i < maxClientsCount; i++) {
                    if(threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if(i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch(IOException e) {
                System.out.println(e);
            }
        }
    }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class clientThread extends Thread {

    private String clientName = null;
    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;
    private int roomRef=-1;
    private int joinId=0;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;

    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {

            //set up
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            String name;
            while(true) {
                os.println("Enter your name.");
                name = is.readLine().trim();
                if(name.indexOf('@') == -1) {
                    break;
                } else {
                    os.println("The name should not contain '@' character.");
                }
                os.println("Enter chat room number [0-10]:");
                roomRef = is.readInt();
            }

            //Welcome the new the client.
            os.println("Welcome " + name + " to our chat room.\nTo leave enter /quit in a new line.");
            synchronized(this) {
                for(int i = 0; i < maxClientsCount; i++) {
                    if(threads[i] != null && threads[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }
                for(int i = 0; i < maxClientsCount; i++) {
                    if(threads[i] != null && threads[i] != this) {
                        threads[i].os.println("*** A new user " + name
                                + " entered the chat room !!! ***");
                    }
                }
            }

            // get the msg from a client and broad cast it to all other clients
            while(true) {
                String line = is.readLine();
                if(line.startsWith("/quit")) {
                    // leaving chat room
                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null && threads[i] != this && threads[i].clientName != null) {
                                threads[i].os.println("*** The user " + name
                                        + " is leaving the chat room !!! ***");
                            }
                        }
                    }
                    os.println("*** Bye " + name + " ***");

                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] == this) {
                                threads[i] = null;
                            }
                        }
                    }

                    break;
                }
                // msg sent it to the given client. */
//                if(line.startsWith("@")) {
//                    String[] words = line.split("\\s", 2);
//                    if(words.length > 1 && words[1] != null) {
//                        words[1] = words[1].trim();
//                        if(!words[1].isEmpty()) {
//                            synchronized(this) {
//                                for(int i = 0; i < maxClientsCount; i++) {
//                                    if(threads[i] != null && threads[i] != this
//                                            && threads[i].clientName != null
//                                            && threads[i].clientName.equals(words[0])) {
//                                        threads[i].os.println("<" + name + "> " + words[1]);
//                    /*
//                     * Echo this message to let the client know the private
//                     * message was sent.
//                     */
//                                        this.os.println(">" + name + "> " + words[1]);
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } else {
                    //broadcast it to all other clients.
                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null && threads[i].clientName != null) {
                                threads[i].os.println("<" + name + "> " + line);
                            }
                        }
                    }
//                }
            }



            //close connections
            is.close();
            os.close();
            clientSocket.close();
        } catch(IOException e) {
        }
    }
}
