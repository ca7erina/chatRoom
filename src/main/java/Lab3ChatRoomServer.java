import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


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

    private static final HashMap<clientThread,Integer> threads = new HashMap<clientThread,Integer>();//[thread,chatroom]
   // private static final clientThread[]threads = new clientThread[maxClientsCount]; //[thread,chatroom]
    private static final String chatRooms[] = new String[maxClientsCount];

    public static void main(String args[]) {


        // set port number.
        int portNumber = 2223;
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

                if(threads.size()<maxClientsCount){
                        clientThread clientThread1=  new clientThread(clientSocket,threads,chatRooms);
                        clientThread1.start();
                        threads.put(clientThread1,-1);

                }else{
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
    private final HashMap<clientThread,Integer> threads;
    private String[] chatRooms;
    private int maxClientsCount;
    private int joinId;


    public clientThread(Socket clientSocket, HashMap<clientThread,Integer> threads, String chatRooms[]) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.chatRooms = chatRooms;
        maxClientsCount = threads.size();

    }

    private int getRoomRef(String roomName) {



        for(int i = 0; i <= maxClientsCount; i++) {

            if(chatRooms[i]==null||chatRooms[i].isEmpty() || chatRooms.equals("")) { //didn't find room, add one

                chatRooms[i] = roomName;

                return i;
            }
            if(chatRooms[i].equals(roomName)) { //found room, return ref number

                return i;
            }
        }

            return -1;
    }

    private void setRoofRef(int roomRef){
        for (Map.Entry<clientThread, Integer> entry : threads.entrySet()) {
            if(entry.getKey()==this){
                entry.setValue(roomRef);
            }
        }

    }



    public void run() {
        int maxClientsCount = this.maxClientsCount;
        HashMap<clientThread,Integer> threads = this.threads;

        try {

            //set up
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            // String name;

            //for client
            String roomName = "";
            boolean isUDP = false;
            String clientIp = "";
            int clientPort = -1;
            String clientName = "";
            long joinId = currentThread().getId();
            int roomRef = -1;



            //get info from client
            while(true) {
                roomName = is.readLine().trim().split(":")[1].trim();
                roomRef = getRoomRef(roomName);
                setRoofRef(roomRef);
                clientIp = is.readLine().trim().split(":")[1].trim();
                clientPort = Integer.parseInt(is.readLine().trim().split(":")[1].trim());
                clientName = is.readLine().trim().split(":")[1].trim();
                System.out.println(roomName + " " + clientIp + " " + clientPort + " " + clientName+" "+roomRef);


                break;
            }

            //Welcome the new the client.
            os.println("JOINED_CHATROOM: " + roomName);
            os.println("SERVER_IP: " + clientIp);
            os.println("SERVER_PORT: " + clientPort);
            os.println("ROOM_REF: " + roomRef);
            os.println("JOIN_ID: " + joinId);


            //tell all the threads , new user joined
            synchronized(this) {
                for (Map.Entry<clientThread, Integer> entry : threads.entrySet()) {
                    if(entry.getValue()==roomRef&&entry.getKey()!=this){
                        entry.getKey().os.println(clientName + " has joined this chatroom.");
                    }
                }

            }

            // get the msg from a client and broad cast it to all other clients
            while(true) {
                String line = is.readLine();
               // System.out.println(line);

                //leave protocol
                if(line.startsWith("LEAVE_CHATROOM:")) {

                    // inform all other users in this room
                    synchronized(this) {
                        for (Map.Entry<clientThread, Integer> entry : threads.entrySet()) {
                            if(entry.getValue()==roomRef && entry.getKey()!=this){
                                entry.getKey().os.println(clientName + "has left this chatroom.");
                            }
                        }
                    }
                    os.println("LEFT_CHATROOM:" + roomRef);
                    os.println("JOIN_ID:" + joinId);


                    //reset this thread in the map...?
                    synchronized(this) {
                        for (Map.Entry<clientThread, Integer> entry : threads.entrySet()) {
                            if(entry.getKey()==this){
                                entry.setValue(-1);
                            }
                        }
                    }

                    break;
                }



                if(line.indexOf("MESSAGE:")>=0){
                    String msg = parseClientMsg(line);
                    //broadcast it to all other clients.
                    synchronized(this) {
                        for (Map.Entry<clientThread, Integer> entry : threads.entrySet()) {
                            if(entry.getValue()==roomRef&&entry.getKey()!=this){
                                entry.getKey().os.println("CHAT:" + roomRef);
                                entry.getKey().os.println("CLIENT_NAME:" + clientName);
                                entry.getKey().os.println("MESSAGE:" + msg);
                            }
                        }

                    }
                }

            }


            //close connections
            is.close();
            os.close();
            clientSocket.close();
        } catch(IOException e) {
        }

    }
    private String parseClientMsg(String lines){
        return lines.split("MESSAGE:")[1].trim();
    }
}
