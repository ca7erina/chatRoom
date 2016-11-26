import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.TreeSet;


/*
 * A  server that delivers public and private messages.
 */
public class Server {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;
    private static int joinId = 0;
    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 30;
    private static final clientThread0[] threads = new clientThread0[maxClientsCount];
    private static  TreeSet<ChatRoom> chatRooms = new TreeSet<ChatRoom>();
    private static boolean stop=false;


    public static void main(String args[]) throws IOException {

        // set port number.
        int portNumber = 2222;

        try {
            serverSocket = new ServerSocket(portNumber);
        } catch(IOException e) {
            System.out.println(e);
        }


        //Create a client socket for each connection and pass it to a new client thread.
        while(!stop) {
            try {
                clientSocket = serverSocket.accept();

                int i=0;
                for(i = 0; i < maxClientsCount; i++) {
                    if(threads[i] == null) {
                        (threads[i] = new clientThread0(clientSocket,threads,chatRooms,serverSocket,joinId++)).start();
                        break;
                    }
                }
                while(i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch(SocketException e) {
               e.printStackTrace();
                //stop all services
                for(int i = 0; i < maxClientsCount; i++) {
                    if(threads[i]!=null){

                        if(threads[i].isAlive()){

                                threads[i].clientSocket.close();

                            threads[i].is.close();
                            threads[i].os.close();
                            while(threads[i].isAlive()){}
                        }


                            System.out.println(threads[i].joinId+" "+threads[i].isAlive());

                        threads[i]=null;

                    }

                }

                break;

            } catch(Exception e){
                e.printStackTrace();
            }
        }

        try {

            serverSocket.close();
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
   // private DataInputStream is = null;
    public BufferedReader is=null;
    public PrintWriter os = null;
    public Socket clientSocket = null;
    private  clientThread0[] threads;
    private int maxClientsCount;
    public long joinId=-1;
    private  TreeSet<ChatRoom> chatRooms; //all rooms
    private TreeSet<ChatRoom> joinedRoom = new TreeSet<ChatRoom>(); //current thread joined rooms


    public clientThread0(Socket clientSocket, clientThread0[] threads, TreeSet<ChatRoom> chatRooms,ServerSocket serverSocket, int joinId) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.chatRooms=chatRooms;
        maxClientsCount = threads.length;
        this.joinId=joinId;
        this.serverSocket = serverSocket;


    }

    private int getRoomRef(String roomName) {
        if(chatRooms.size()==0){ //no room , this is the first
            chatRooms.add(new ChatRoom(0,roomName));
            return 0;
        }
        for (ChatRoom room:chatRooms){ //find room
            if(room.getName().equals(roomName)){
                    return room.getId();
                }
            }

        int id=chatRooms.size();// add room
        chatRooms.add(new ChatRoom(id,roomName));
        return id;


    }

    synchronized private void addJoinedRoom(ChatRoom room){
        joinedRoom.add(room);

    }

    synchronized private boolean isAlreadyIn(int roomRef){
        for (Iterator<ChatRoom> iterator = this.joinedRoom.iterator(); iterator.hasNext(); ) {
            ChatRoom room = iterator.next();
            if (room.getId()==(roomRef)){
                return true;
            }
        }

        return false;


    }

    synchronized private void removeJoinedRoom(int roomRef){
       for (Iterator<ChatRoom> iterator = this.joinedRoom.iterator(); iterator.hasNext(); ) {
           ChatRoom room = iterator.next();
           if(room.getId()==roomRef){
               iterator.remove();
           }
       }


    }


    public void run() {

        clientThread0[] threads = this.threads;

        try {

            //set up
            is= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            os = new PrintWriter(clientSocket.getOutputStream(),true);
            String line="";
            while((line = is.readLine())!=null) {
//                String line = is.readLine();
//                if(null==line||line.isEmpty()){
//                   // os.println("ERROR_CODE:00");
//                   // os.println("ERROR_DESCRIPTION: message is empty.\n");
//                    continue;
//                }
                System.out.println("thread"+this.joinId+" receive msg:"+line);

                // service

                if(line.startsWith("KILL_SERV") ) {




//                    this.clientSocket.close();
//                    this.serverSocket.close();

                    //new Socket("localhost", 2222);
                    for(int i = 0; i < maxClientsCount; i++) {
                        if(threads[i] != null ) {
                            threads[i].clientSocket.close();
                            threads[i].os.close();
                            threads[i].is.close();
                            threads[i]=null;
                        }
                    }

                    break;
                }



             //base test
                if(line.trim().indexOf("BASE_TEST") > 0) {
                    String address =Inet4Address.getLocalHost().toString();
                    os.println(line + "\n" + "IP:" + address.split("/")[1] + "\n" + "Port: " + 2222 + "\nStudentID: 16302007");
                    os.flush();
                    continue;
                }

                //client join
                if(line.startsWith("JOIN_CHATROOM:")) {
                    String roomName = line.split(":")[1].trim();
                    int roomRef0 = getRoomRef(roomName);

                    //already joined
                    if(isAlreadyIn(roomRef0)){
                        os.println("ERROR_CODE:00");
                        os.println("ERROR_DESCRIPTION: already joined this room.\n");
                        os.flush();
                        continue;
                    }

                    addJoinedRoom(new ChatRoom(roomRef0,roomName));
//                    this.clientIp = is.readLine().trim().split(":")[1].trim();
//                    this.clientPort = Integer.parseInt(is.readLine().trim().split(":")[1].trim());
                    this.clientIp="0";is.readLine();
                    this.clientPort=0;is.readLine();
                    this.clientName = is.readLine().trim().split(":")[1].trim();

                   // System.out.println(roomName + " " + clientIp + " " + clientPort + " " + clientName+" "+roomRef0);

                    //Welcome the new the client.
                    os.println("JOINED_CHATROOM: " + roomName);
                    os.println("SERVER_IP: " + Inet4Address.getLocalHost().getHostAddress());
                    os.println("PORT: " + "2223");
                    os.println("ROOM_REF: " + roomRef0);
                    os.println("JOIN_ID: " + joinId);
                    os.flush();

                    //tell all the threads , new user joined
                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null ) {
                                // thread that in the same room
                                for (ChatRoom room:threads[i].joinedRoom){

                                    if(room.getId()==(roomRef0)){
                                        threads[i].os.println("CHAT:"+roomRef0);
                                        threads[i].os.println("CLIENT_NAME:"+clientName);
                                        threads[i].os.println("MESSAGE:" + this.clientName+" has joined this chatroom.\n");
                                        threads[i].os.flush();
                                    }
                                }
                            }
                        }
                    }
                    continue;
                }





               // disconnect
                if(line.startsWith("DISCONNECT:")) {
                    int clientPort = Integer.parseInt(is.readLine().substring(5).trim()); //dont need?
                    System.out.println("port"+clientPort);
                    String clientName = is.readLine().substring(12).trim(); // dont need?
                    System.out.println("clientname:"+clientName);

                    //// inform all other users in those room
                    synchronized(this) {
                        for (ChatRoom room0:this.joinedRoom){
                            for(int i = 0; i < maxClientsCount; i++) {
                                if(threads[i]!=null){
                                    for (ChatRoom iroom:threads[i].joinedRoom){
                                        if(iroom.getId()==(room0.getId())){
                                            threads[i].os.println("CHAT:"+iroom.getId());
                                            threads[i].os.println("CLIENT_NAME:"+clientName);
                                            threads[i].os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
                                            threads[i].os.flush();
                                        }
                                    }
                                }


                            }

                        }
                        //remove all rooms
                        this.joinedRoom=new TreeSet<ChatRoom>();

                    }

                  //  break;
                   continue;
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
                                for (ChatRoom room:threads[i].joinedRoom){
                                    if(room.getId()==roomRef0){ //same room
                                        threads[i].os.println("CHAT:"+roomRef0);
                                        threads[i].os.println("CLIENT_NAME:"+clientName);
                                        threads[i].os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
                                        threads[i].os.flush();
                                    }
                                }
                            }
                        }
                    }
                    if(isAlreadyIn(roomRef0)){
                        removeJoinedRoom(roomRef0);

                    }else{
                        os.println("ERROR_CODE:00");
                        os.println("ERROR_DESCRIPTION: already left this room.\n");
                        os.flush();
                    }
                    continue;

                }




                //msg
                if(line.startsWith("CHAT:") ) {
                    int roomRef0 = Integer.parseInt(line.substring(5).trim());
                    String temp =is.readLine();
                    System.out.println(temp);
                    if (temp.startsWith("JOIN_ID")){
                        int joinId = Integer.parseInt(temp.substring(8).trim());
                    }

                    String clientName = is.readLine().substring(12).trim();
                    String msg = is.readLine().substring(8).trim();
                    is.readLine();// /n

                    //broadcast it to all other clients in this room
                    synchronized(this) {
                        for(int i = 0; i < maxClientsCount; i++) {
                            if(threads[i] != null && threads[i].clientName != null) {
                                for (ChatRoom room:threads[i].joinedRoom){
                                    if(room.getId() ==(roomRef0)){ //same room
                                        threads[i].os.println("CHAT:" + roomRef0);
                                        threads[i].os.println("CLIENT_NAME:" + clientName);
                                        threads[i].os.println("MESSAGE:" + msg+"\n");
                                        threads[i].os.flush();
                                    }
                                    }
                                }
                        }
                    }
                }

            }
            os.flush();

        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }finally {
            //close connections
            try {

                clientSocket.close();
                if(is != null)is.close();
                if(os != null)os.close();
               // System.out.println("clientsocket close");



            } catch(IOException e) {
                e.printStackTrace();

            }

        }



    }


}

class ChatRoom implements Comparable{
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private int id;
        private String name;

    public ChatRoom(int id, String name) {
        this.id = id;
        this.name = name;
    }


    @Override
    public int compareTo(Object o) {
        ChatRoom other=(ChatRoom)o;
        return this.getId()-((ChatRoom) o).getId();
    }
}
