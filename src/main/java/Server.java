import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.HashSet;
import java.util.Set;


/*
 * A chat server that delivers public and private messages.
 */
public class Server {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 30;
    private static final clientThread0[] threads = new clientThread0[maxClientsCount];
    private static final String chatRooms[] = new String[1000];


    public static void main(String args[]) {

        // set port number.
        int portNumber = 2222;

        try {
            serverSocket = new ServerSocket(portNumber);
        } catch(IOException e) {
            System.out.println(e);
        }


        //Create a client socket for each connection and pass it to a new client thread.

        while(true) {
            try {
                clientSocket = serverSocket.accept();


                int i=0;
                for(i = 0; i < maxClientsCount; i++) {
                    if(threads[i] == null) {
                        (threads[i] = new clientThread0(clientSocket,threads,chatRooms,serverSocket)).start();
                        break;
                    }
                }
                if(i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch(SocketException e) {
             // e.printStackTrace();
                break;
            } catch(Exception e){
                e.printStackTrace();
            }
        }

            try {
                clientSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }


            System.out.println("The server is shut down!");

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
class clientThread0 extends Thread {
    private ServerSocket serverSocket;
    private String clientName = null;
    private int clientPort=-1;
    private String clientIp = "";
    private DataInputStream is = null;
    public PrintStream os = null;
    public Socket clientSocket = null;
    private final clientThread0[] threads;
    private int maxClientsCount;
    private long joinId=-1;
    private String[] chatRooms; //all rooms
    private Set<Integer> joinedRoom = new HashSet<Integer>(); //current thread joined rooms

    public clientThread0(Socket clientSocket, clientThread0[] threads, String chatRooms[],ServerSocket serverSocket) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.chatRooms=chatRooms;
        maxClientsCount = threads.length;
        this.serverSocket = serverSocket;


    }

    private int getRoomRef(String roomName) {
        for(int i = 0; i < 1000; i++) {
            if(chatRooms[i] == null || chatRooms[i].isEmpty() || chatRooms.equals("")) { //didn't find room, add one
                chatRooms[i] = roomName;
                return i;
            }
            if(chatRooms[i].equals(roomName)) { //found room, return ref number
                return i;
            }
        }
        return -1;
    }

    private void addJoinedRoom(int roomRef){
        joinedRoom.add(roomRef);

    }

    private void removeJoinedRoom(int roomRef){
        this.joinedRoom.remove(roomRef);

    }


    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread0[] threads = this.threads;

        try {

            //set up
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());

            while(true) {
                String line = is.readLine();
                if(null==line){
                    System.out.println("error: line is null");
                    break;
                }
                System.out.println("receive msg:"+line);


             //base test
                if(line.trim().indexOf("BASE_TEST") > 0) {
                    os.println("HELO BASE_TEST");
                    os.println("IP:127.0.0.1");
                    os.println("Port:8888");
                    os.println("StudentID:TESTSERVER1234");
                    continue;
                }

                //client join
                if(line.startsWith("JOIN_CHATROOM:")) {
                    String roomName = line.split(":")[1].trim();
                    int roomRef0 = getRoomRef(roomName);
                    addJoinedRoom(roomRef0);
                    this.clientIp = is.readLine().trim().split(":")[1].trim();
                    this.clientPort = Integer.parseInt(is.readLine().trim().split(":")[1].trim());
                    this.clientName = is.readLine().trim().split(":")[1].trim();
                    this.joinId=currentThread().getId();
                   // System.out.println(roomName + " " + clientIp + " " + clientPort + " " + clientName+" "+roomRef0);

                    //Welcome the new the client.
                    os.println("JOINED_CHATROOM: " + roomName);
                    os.println("SERVER_IP: " + Inet4Address.getLocalHost().getHostAddress());
                    os.println("PORT: " + "2223");
                    os.println("ROOM_REF: " + roomRef0);
                    os.println("JOIN_ID: " + joinId);

                    //tell all the threads , new user joined
                    synchronized(this) {

                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null ) {
                                // thread that in the same room
                                for (Integer room:threads[i].joinedRoom){
                                    if(room.equals(roomRef0)){
                                        threads[i].os.println("CHAT:"+roomRef0);
                                        threads[i].os.println("CLIENT_NAME:"+clientName);
                                        threads[i].os.println("MESSAGE:" + this.clientName+" has joined this chatroom.\n");
                                    }
                                }
                            }
                        }

                    }
                    continue;

                }


               // service

                if(line.trim().equals("KILL_SERVICE") ) {
                    //stop all services
                    for(int i = 0; i < maxClientsCount; i++) {
                        if(threads[i] != null && threads[i]!=this){
                                threads[i].is.close();
                                threads[i].os.close();
                                threads[i].clientSocket.close();
                                threads[i].interrupt();

                        }
                        threads[i] = null;
                    }

                    break;
                }


               // disconnect
                if(line.startsWith("DISCONNECT:")) {
                    int clientPort = Integer.parseInt(is.readLine().substring(5).trim()); //dont need?
                    String clientName = is.readLine().substring(12).trim(); // dont need?


                    //// inform all other users in those room
                    synchronized(this) {
                        for (Integer room0:this.joinedRoom){
                            for(int i = 0; i < maxClientsCount; i++) {
                                if(threads[i]!=null){
                                    for (Integer iroom:threads[i].joinedRoom){
                                        if(iroom.equals(room0)){
                                            threads[i].os.println("CHAT:"+iroom);
                                            threads[i].os.println("CLIENT_NAME:"+clientName);
                                            threads[i].os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
                                        }
                                    }
                                }


                            }

                        }
//                        //remove all rooms
//                        this.joinedRoom=new HashSet<Integer>();

                    }


                    break;
                }

                //leave room, but still connected , wont receive any message cuz not in this room.
                if(line.startsWith("LEAVE_CHATROOM:")) {
                    int roomRef0 = Integer.parseInt(line.substring(15).trim());
                    int joinId = Integer.parseInt(is.readLine().substring(8).trim());
                    String clientName = is.readLine().substring(12).trim();

                    os.println("LEFT_CHATROOM:" + roomRef0);
                    os.println("JOIN_ID:" + joinId);
                    //// inform all other users in this room
                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null  ) {
                                for (Integer room:threads[i].joinedRoom){
                                    if(room.equals(roomRef0)){ //same room
                                        threads[i].os.println("CHAT:"+roomRef0);
                                        threads[i].os.println("CLIENT_NAME:"+clientName);
                                        threads[i].os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
                                    }
                                }
                            }
                        }
                    }
                    removeJoinedRoom(roomRef0);
                    continue;

                }




                //msg
                if(line.startsWith("CHAT:") ) {
                    int roomRef0 = Integer.parseInt(line.substring(5).trim());
                    String temp =is.readLine();
                    if (temp.startsWith("JOIN_ID")){
                        int joinId = Integer.parseInt(temp.substring(8).trim());
                    }

                    String clientName = is.readLine().substring(12).trim();
                    String msg = is.readLine().substring(8).trim();

                    //broadcast it to all other clients in this room
                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null && threads[i].clientName != null) {
                                for (Integer room:threads[i].joinedRoom){
                                    if(room.equals(roomRef0)){ //same room
                                        threads[i].os.println("CHAT:" + roomRef0);
                                        threads[i].os.println("CLIENT_NAME:" + clientName);
                                        threads[i].os.println("MESSAGE:" + msg+"\n");
                                    }
                                    }
                                }
                        }
                    }
                }

            }

           // Thread.currentThread().interrupt();
        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }finally {
            //close connections
            try {
                if(is != null)is.close();
                if(os != null)os.close();
                clientSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }

        }
    }
}
