import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
* Chat Server
*/
public class Lab3ChatRoomServer {
    private static ServerSocket serverSocket = null;
    private static final int PORT_NUMBER = 2222;
    private static final int MAX_CLIENT_NUMBER = 3;
    private static ClientThread[] threads = new ClientThread[MAX_CLIENT_NUMBER];
    private static TreeSet<ChatRoom> allChatRooms = new TreeSet<ChatRoom>();

    public static void main(String args[]) {
        int joinId = 0;
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CLIENT_NUMBER);
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
        } catch(IOException e) {
            e.printStackTrace();
        }
        /* Create a client socket for each connection and pass it to a new client thread. */
        while(true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientThread ct=new ClientThread(clientSocket, threads, allChatRooms, joinId++);
                /* put thread in the list */
                for(int i = 0; i < MAX_CLIENT_NUMBER; i++) {
                    if(threads[i] == null) {
                        threads[i] = ct;
                        break;
                    }
                }
                executor.execute(ct);
            } catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            executor.shutdown();
            serverSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
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
class ClientThread extends Thread {
    //    private String clientName;
//    private final int clientPort = 0; //for TCP
//    private final String clientIp = "0"; //for TCP
    private BufferedReader is = null;
    private PrintWriter os = null;
    private Socket clientSocket = null;
    private ClientThread[] threads;
    private int maxClientsCount;
    private long joinId = -1;
    private TreeSet<ChatRoom> chatRooms; //all rooms set
    private TreeSet<ChatRoom> joinedRoom = new TreeSet<ChatRoom>(); //client's current thread joined rooms

    public ClientThread(Socket clientSocket, ClientThread[] threads, TreeSet<ChatRoom> chatRooms, int joinId) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.chatRooms = chatRooms;
        this.maxClientsCount = threads.length;
        this.joinId = joinId;
    }

    public void run() {
        ClientThread[] threads = this.threads;
        try {
            /* set up in/out streams */
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            os = new PrintWriter(clientSocket.getOutputStream(), true);
            String line;
            while((line = is.readLine()) != null) {
                System.out.println("thread" + this.joinId + " receive msg:" + line); //debug

                /* kill service */
                if(line.startsWith("KILL_SERV")) {
                    for(int i = 0; i < maxClientsCount; i++) {
                        if(threads[i]==this){
                            threads[i].clientSocket.close();
                            threads[i].os.close();
                            threads[i].is.close();
                            threads[i] = null;
                        }
                    }
                    break;
                }

                /* base test */
                if(line.trim().indexOf("BASE_TEST") > 0) {
                    String address = Inet4Address.getLocalHost().toString();
                    os.println(line + "\n" + "IP:" + address.split("/")[1] + "\n" + "Port: " + 2222 + "\nStudentID: 16302007");
                    os.flush();
                    continue;
                }

                /* client join */
                if(line.startsWith("JOIN_CHATROOM:")) {
                    /* read content from socket. */
                    String roomName = line.split(":")[1].trim();
                    int roomRef0 = getRoomRef(roomName);
                    is.readLine(); //for TCP
                    is.readLine(); //for TCP
                    String clientName = is.readLine().trim().split(":")[1].trim();
                    /* for client already joined this certain room. */
                    if(isAlreadyIn(roomRef0)) {
                        os.println("ERROR_CODE:00");
                        os.println("ERROR_DESCRIPTION: already joined this room.\n");
                        os.flush();
                        continue;
                    }
                    /* add this new room to room set. */
                    addJoinedRoom(new ChatRoom(roomRef0, roomName));
                    /* feedback to the new joined client. */
                    os.println("JOINED_CHATROOM: " + roomName);
                    os.println("SERVER_IP: " + Inet4Address.getLocalHost().getHostAddress());
                    os.println("PORT: " + "2223");
                    os.println("ROOM_REF: " + roomRef0);
                    os.println("JOIN_ID: " + joinId);
                    os.flush();
                    /* broadcast it to all other clients in this room. */
                    String msg = "CHAT:" + roomRef0+"\nCLIENT_NAME:"+clientName+"\nMESSAGE:"+clientName + " has joined this chatroom.\n";
                    broadcastToOthersInTheSameRoom(roomRef0,msg);
                    continue;
                }

                /* Disconnect a client */
                if(line.startsWith("DISCONNECT:")) {
                    /* read content from socket. */
//                    int clientPort = Integer.parseInt(is.readLine().substring(5).trim());
                    is.readLine();//read client port, but wont use it in TCP mode;
                    String clientName = is.readLine().substring(12).trim();
                    /* broadcast it to all other clients in this room. */
                    synchronized(this) {
                        for(ChatRoom room0 : this.joinedRoom) {
                            String msg = "CHAT:" + room0.getId()+"\nCLIENT_NAME:" + clientName+"\nMESSAGE:" + clientName + " has left this chatroom.\n";
                            broadcastToOthersInTheSameRoom(room0.getId(),msg);
                        }
                        /* remove the client from all associated rooms. */
                        this.joinedRoom = new TreeSet<ChatRoom>();
                    }
                    continue;
                }

                /* leave certain room, wont receive any message cuz not in this room. */
                if(line.startsWith("LEAVE_CHATROOM:")) {
                    /* read content from socket. */
                    int roomRef0 = Integer.parseInt(line.substring(15).trim());
                    int joinId = Integer.parseInt(is.readLine().substring(8).trim());
                    String clientName = is.readLine().substring(12).trim();
                    /* feadback to client. */
                    os.println("LEFT_CHATROOM:" + roomRef0);
                    os.println("JOIN_ID:" + joinId);
                    /* broadcast it to all other clients in this room. */
                    String msg = "CHAT:" + roomRef0+"\nCLIENT_NAME:" + clientName+"\nMESSAGE:" + clientName + " has left this chatroom.\n";
                    broadcastToOthersInTheSameRoom(roomRef0,msg);
                    /* remove the room from this client. */
                    if(isAlreadyIn(roomRef0)) {
                        removeJoinedRoom(roomRef0);
                    } else {
                        /* client try to leave a room which never joined at the beginning */
                        os.println("ERROR_CODE:00");
                        os.println("ERROR_DESCRIPTION: You never joined in this room\n");
                        os.flush();
                    }
                    continue;
                }

                /* Chat */
                if(line.startsWith("CHAT:")) {
                    /* read content from socket */
                    int roomRef0 = Integer.parseInt(line.substring(5).trim());
                    is.readLine(); //read joinid, in this protocol, never used here tho.
                    String clientName = is.readLine().substring(12).trim();
                    String message = is.readLine().substring(8).trim();
                    is.readLine();// extra line
                     /* broadcast it to all other clients in this room */
                    String msg = "CHAT:" + roomRef0+"\nCLIENT_NAME:" + clientName+"\nMESSAGE:" + message + "\n";
                    broadcastToOthersInTheSameRoom(roomRef0,msg);
                }
            }
            os.flush();

        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            /* close connections */
            try {
                if(clientSocket != null)
                    clientSocket.close();
                if(is != null)
                    is.close();
                if(os != null)
                    os.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * broadcast to others
     */
    synchronized private void broadcastToOthersInTheSameRoom(int roomRef0, String msg){
        for(ClientThread c: this.threads) {
            if(c != null) {
                for(ChatRoom room : c.joinedRoom) {
                    if(room.getId() == (roomRef0)) {
                        c.os.println(msg);
                        c.os.flush();
                    }
                }
            }
        }
    }

    /**
     * Get the reference id of a room from the room set.
     */
    private int getRoomRef(String roomName) {
        if(chatRooms.size() == 0) { //no room , this is the first
            chatRooms.add(new ChatRoom(0, roomName));
            return 0;
        }
        for(ChatRoom room : chatRooms) { //find room
            if(room.getName().equals(roomName)) {
                return room.getId();
            }
        }
        int id = chatRooms.size();// add room
        chatRooms.add(new ChatRoom(id, roomName));
        return id;
    }

    /**
     * Add the room in the client's joined room set.
     */
    synchronized private void addJoinedRoom(ChatRoom room) {
        joinedRoom.add(room);
    }

    /**
     * Check if the room appear in the joined set.
     */
    synchronized private boolean isAlreadyIn(int roomRef) {
        for(ChatRoom room : this.joinedRoom) {
            if(room.getId() == (roomRef)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a room from joined set.
     */
    synchronized private void removeJoinedRoom(int roomRef) {
        for(Iterator<ChatRoom> iterator = this.joinedRoom.iterator(); iterator.hasNext(); ) {
            ChatRoom room = iterator.next();
            if(room.getId() == roomRef) {
                iterator.remove();
            }
        }
    }
}

class ChatRoom implements Comparable {
    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChatRoom(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int compareTo(Object o) {
        return this.getId() - ((ChatRoom) o).getId();
    }

}
