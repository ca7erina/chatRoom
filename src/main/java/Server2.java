import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
* Created by chenxiaoxue
* Multithread(threadpool)
*/
public class Server2 {


    static private int port;
    static private int poolSize=20;
    private static final String chatRooms[] = new String[100];

    public static void main(String[] args) {
        port=2222;

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ServerSocket serverSocket=null;
        try {
            serverSocket = new ServerSocket(port);
            while(true){
                Socket clientSocket = serverSocket.accept();
                ConnectionHandler ch = new ConnectionHandler(clientSocket,chatRooms);
                executor.execute(ch);
            }
        } catch(SocketException e) {
            executor.shutdown();
        } catch(IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished all threads");

    }

}

class ConnectionHandler implements Runnable, Comparable <ConnectionHandler>{


    private String clientName = null;
    private int clientPort=-1;
    private String clientIp = "";
    private DataInputStream is = null;
    public PrintStream os = null;
    public Socket clientSocket = null;
    private long joinId=-1;
    private String[] chatRooms; //all rooms
    private Set<Integer> joinedRoom = new HashSet<Integer>(); //current thread joined rooms
    private volatile static Set <ConnectionHandler>    allClients = new ConcurrentSkipListSet<ConnectionHandler>();



    public ConnectionHandler(Socket clientSocket, String chatRooms[]) {
        this.clientSocket = clientSocket;
        this.chatRooms=chatRooms;
        allClients.add(this);
    }

    public void run() {


        try {

            //set up
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            String line;
            while( (line = is.readLine()) != null) {

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
                    this.joinId=Thread.currentThread().getId();
                    // System.out.println(roomName + " " + clientIp + " " + clientPort + " " + clientName+" "+roomRef0);

                    //Welcome the new the client.
                    os.println("JOINED_CHATROOM: " + roomName);
                    os.println("SERVER_IP: " + Inet4Address.getLocalHost().getHostAddress());
                    os.println("PORT: " + "2223");
                    os.println("ROOM_REF: " + roomRef0);
                    os.println("JOIN_ID: " + joinId);

                    //tell all the threads , new user joined
                    synchronized(this) {

                        for(ConnectionHandler h : ConnectionHandler.allClients) {
                            if(h != null ) {
                                // thread that in the same room
                                for (Integer room:h.joinedRoom){
                                    if(room.equals(roomRef0)){
                                        h.os.println("CHAT:"+roomRef0);
                                        h.os.println("CLIENT_NAME:"+clientName);
                                        h.os.println("MESSAGE:" + this.clientName+" has joined this chatroom.\n");
                                    }
                                }
                            }
                        }

                    }
                    continue;

                }


                //kill service, close all sockets

                if(line.trim().equals("KILL_SERVICE") ) {

                    for(ConnectionHandler h : ConnectionHandler.allClients) {
                        if(h!=null&&h!=this){
                                h.is.close();
                                h.os.close();
                            h.clientSocket.close();
                        }
                    }

                    break;
                }


                // disconnect: but connection and leave all rooms.
                if(line.startsWith("DISCONNECT:")) {
                    int clientPort = Integer.parseInt(is.readLine().substring(5).trim()); //dont need?
                    String clientName = is.readLine().substring(12).trim(); // dont need?


                    //// inform all other users in those room
                    synchronized(this) {

                        for (Integer room0:this.joinedRoom){
                            for(ConnectionHandler h : ConnectionHandler.allClients) {
                                if(h!=null){
                                    for (Integer iroom:h.joinedRoom){
                                        if(iroom.equals(room0)){
                                            h.os.println("CHAT:"+iroom);
                                            h.os.println("CLIENT_NAME:"+clientName);
                                            h.os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
                                        }
                                    }
                                }


                            }

                        }
//                        //leave all rooms
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
                        for(ConnectionHandler h : ConnectionHandler.allClients) {
                            if(h != null  ) {
                                for (Integer room:h.joinedRoom){
                                    if(room.equals(roomRef0)){ //same room
                                        h.os.println("CHAT:"+roomRef0);
                                        h.os.println("CLIENT_NAME:"+clientName);
                                        h.os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
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
                        for(ConnectionHandler h : ConnectionHandler.allClients) {
                            if(h != null && h.clientName != null) {
                                for (Integer room:h.joinedRoom){
                                    if(room.equals(roomRef0)){ //same room
                                        h.os.println("CHAT:" + roomRef0);
                                        h.os.println("CLIENT_NAME:" + clientName);
                                        h.os.println("MESSAGE:" + msg+"\n");
                                    }
                                }
                            }
                        }
                    }
                }

            }





            //close connections
            is.close();
            os.close();
            clientSocket.close();


        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
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

    @Override
    public int compareTo(ConnectionHandler o) {
        return 1;
    }
}

