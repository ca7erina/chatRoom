//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintStream;
//import java.net.Inet4Address;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.net.UnknownHostException;
//import java.util.Iterator;
//import java.util.Set;
//import java.util.TreeSet;
//import java.util.concurrent.ConcurrentSkipListSet;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//
///*
// * A  server that delivers public and private messages.
// */
//public class Server3 {
//
//    // The server socket.
//    private static ServerSocket serverSocket = null;
//
//    private static int joinId = 0;
//
//    private static TreeSet<ChatRoom3> chatRooms = new TreeSet<ChatRoom3>();
//    static boolean isOn = true;
//
//
//
//
//    public static void main(String args[]) throws IOException {
//
//
//        int port = 2222;
//
//        ExecutorService executor = Executors.newFixedThreadPool(20);
//        ServerSocket serverSocket = null;
//
//        try {
//            serverSocket = new ServerSocket(port);
//
//            while(isOn) {
//                Socket clientSocket = serverSocket.accept();
//                ClientThread3 ch = new ClientThread3(clientSocket, serverSocket, chatRooms, joinId++);
//                executor.execute(ch);
//            }
//
//
//
//
//        }catch(Exception e){
//            e.printStackTrace();
//
//        }
//        executor.shutdown();
//        serverSocket.close();
//        System.out.println("The server is shut down!");
//    }
//}
//
///*
// * The chat client thread. This client thread opens the input and the output
// * streams for a particular client, ask the client's name, informs all the
// * clients connected to the server about the fact that a new client has joined
// * the chat room, and as long as it receive data, echos that data back to all
// * other clients. The thread broadcast the incoming messages to all clients and
// * routes the private message to the particular client. When a client leaves the
// * chat room this thread informs also all the clients about that and terminates.
// */
//class ClientThread3 extends Thread implements Comparable <ClientThread3>{
//    private ServerSocket serverSocket;
//    private String clientName = null;
//    private int clientPort=-1;
//    private String clientIp = "";
//    // private DataInputStream is = null;
//    public BufferedReader is=null;
//    public PrintStream os = null;
//    public Socket clientSocket = null;
//    public long joinId=-1;
//    private  TreeSet<ChatRoom3> chatRooms; //all rooms
//    private TreeSet<ChatRoom3> joinedRoom = new TreeSet<ChatRoom3>(); //current thread joined rooms
//    static Set<ClientThread3> allClients = new ConcurrentSkipListSet<ClientThread3>();
//
//    public ClientThread3(Socket clientSocket,ServerSocket serverSocket, TreeSet<ChatRoom3> chatRooms, int joinId) {
//        this.clientSocket = clientSocket;
//        this.chatRooms=chatRooms;
//        this.joinId=joinId;
//        this.serverSocket = serverSocket;
//        allClients.add(this);
//
//
//    }
//
//    private int getRoomRef(String roomName) {
//        if(chatRooms.size()==0){ //no room , this is the first
//            chatRooms.add(new ChatRoom3(0,roomName));
//            return 0;
//        }
//        for (ChatRoom3 room:chatRooms){ //find room
//            if(room.getName().equals(roomName)){
//                return room.getId();
//            }
//        }
//
//        int id=chatRooms.size();// add room
//        chatRooms.add(new ChatRoom3(id,roomName));
//        return id;
//
//
//    }
//
//    synchronized private void addJoinedRoom(ChatRoom3 room){
//        joinedRoom.add(room);
//
//    }
//
//    synchronized private boolean isAlreadyIn(int roomRef){
//        for (Iterator<ChatRoom3> iterator = this.joinedRoom.iterator(); iterator.hasNext(); ) {
//            ChatRoom3 room = iterator.next();
//            if (room.getId()==(roomRef)){
//                return true;
//            }
//        }
//
//        return false;
//
//
//    }
//
//    synchronized private void removeJoinedRoom(int roomRef){
//        for (Iterator<ChatRoom3> iterator = this.joinedRoom.iterator(); iterator.hasNext(); ) {
//            ChatRoom3 room = iterator.next();
//            if(room.getId()==roomRef){
//                iterator.remove();
//            }
//        }
//
//
//    }
//
//
//
//    public void run() {
//
//
//        try {
//
//            //set up
//            is= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//            os = new PrintStream(clientSocket.getOutputStream());
//            String line="";
//            while((line = is.readLine())!=null) {
//
//                System.out.println("thread"+this.joinId+" receive msg:"+line);
//
//                // service
//
//                if(line.startsWith("KILL_SERV") ) {
//
//                    for(ClientThread3 h : ClientThread3.allClients) {
//                        if(h!=this){
//
//                            h.clientSocket.close();
//                            if(is != null)is.close();
//                            if(os != null)os.close();
//                        }
//
//                    }
//
//
//                    break;
//                }
//
//
//
//                //base test
//                if(line.trim().indexOf("BASE_TEST") > 0) {
//                    String address =Inet4Address.getLocalHost().toString();
//                    os.println(line + "\n" + "IP:" + address.split("/")[1] + "\n" + "Port: " + 2222 + "\nStudentID: 16302007");
//                    os.flush();
//
//                }
//
//                //client join
//                if(line.startsWith("JOIN_CHATROOM:")) {
//                    String roomName = line.split(":")[1].trim();
//                    int roomRef0 = getRoomRef(roomName);
//
//                    //already joined
//                    if(isAlreadyIn(roomRef0)){
//                        os.println("ERROR_CODE:00");
//                        os.println("ERROR_DESCRIPTION: already joined this room.\n");
//                        os.flush();
//                        continue;
//                    }
//
//                    addJoinedRoom(new ChatRoom3(roomRef0,roomName));
////                    this.clientIp = is.readLine().trim().split(":")[1].trim();
////                    this.clientPort = Integer.parseInt(is.readLine().trim().split(":")[1].trim());
//                    this.clientIp="0";is.readLine();
//                    this.clientPort=0;is.readLine();
//                    this.clientName = is.readLine().trim().split(":")[1].trim();
//
//                     System.out.println(roomName + " " + clientIp + " " + clientPort + " " + clientName+" "+roomRef0);
//
//                    //Welcome the new the client.
//                    os.println("JOINED_CHATROOM: " + roomName);
//                    os.println("SERVER_IP: " + Inet4Address.getLocalHost().getHostAddress());
//                    os.println("PORT: " + "2222");
//                    os.println("ROOM_REF: " + roomRef0);
//                    os.println("JOIN_ID: " + joinId);
//
//
//
//                    //tell all the threads , new user joined
//                    synchronized(this) {
//                        for(ClientThread3 h : ClientThread3.allClients) {
//                            if(h != null ) {
//                                // thread that in the same room
//                                for (ChatRoom3 room:h.joinedRoom){
//                                    if(room.getId()==(roomRef0)){
//                                        h.os.println("CHAT:"+roomRef0);
//                                        h.os.println("CLIENT_NAME:"+clientName);
//                                        h.os.println("MESSAGE:" + this.clientName + " has joined this chatroom.\n");
//                                        h.os.flush();
//                                    }
//                                }
//                            }
//                        }
//
//                    }
//                    continue;
//
//                }
//
//
//
//
//
//                // disconnect
//                if(line.startsWith("DISCONNECT:")) {
//                    int clientPort = Integer.parseInt(is.readLine().substring(5).trim()); //dont need?
//                    System.out.println("port"+clientPort);
//                    String clientName = is.readLine().substring(12).trim(); // dont need?
//                    System.out.println("clientname:"+clientName);
//
//
//                    //// inform all other users in those room
//                    synchronized(this) {
//                        for (ChatRoom3 room0:this.joinedRoom){
//                            for(ClientThread3 h : ClientThread3.allClients) {
//                                if(h!=null){
//                                    for (ChatRoom3 iroom:h.joinedRoom){
//                                        if(iroom.getId()==(room0.getId())){
//                                            h.os.println("CHAT:"+iroom.getId());
//                                            h.os.println("CLIENT_NAME:"+clientName);
//                                            h.os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
//                                            h.os.flush();
//                                        }
//                                    }
//                                }
//
//
//                            }
//
//                        }
//                        //leave all rooms
//                        this.joinedRoom=new TreeSet<ChatRoom3>();
//                    }
//
//                    //  break;
//                    continue;
//                }
//
//                //leave room, but still connected , wont receive any message cuz not in this room.
//                if(line.startsWith("LEAVE_CHATROOM:")) {
//                    int roomRef0 = Integer.parseInt(line.substring(15).trim());
//                    int joinId = Integer.parseInt(is.readLine().substring(8).trim());
//                    String clientName = is.readLine().substring(12).trim();
//
//                    os.println("LEFT_CHATROOM:" + roomRef0);
//                    os.println("JOIN_ID:" + joinId);
//                    //// inform all other users in this room
//
//                    synchronized(this) {
//                        for(ClientThread3 h : ClientThread3.allClients) {
//                            if(h != null  ) {
//                                for (ChatRoom3 room:h.joinedRoom){
//                                    if(room.getId()==roomRef0){ //same room
//                                        h.os.println("CHAT:"+roomRef0);
//                                        h.os.println("CLIENT_NAME:"+clientName);
//                                        h.os.println("MESSAGE:" + clientName+" has left this chatroom.\n");
//                                        h.os.flush();
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    if(isAlreadyIn(roomRef0)){
//                        removeJoinedRoom(roomRef0);
//
//                    }else{
//                        os.println("ERROR_CODE:00");
//                        os.println("ERROR_DESCRIPTION: already left this room.\n");
//                        os.flush();
//                    }
//                    continue;
//
//                }
//
//
//
//                //msg
//                if(line.startsWith("CHAT:") ) {
//                    int roomRef0 = Integer.parseInt(line.substring(5).trim());
//                    String temp =is.readLine();
//                    System.out.println(temp);
//                    if (temp.startsWith("JOIN_ID")){
//                        int joinId = Integer.parseInt(temp.substring(8).trim());
//                    }
//
//                    String clientName = is.readLine().substring(12).trim();
//                    String msg = is.readLine().substring(8).trim();
//                    is.readLine();// /n
//
//                    //broadcast it to all other clients in this room
//
//                    synchronized(this) {
//                        for(ClientThread3 h : ClientThread3.allClients) {
//                            if(h != null && h.clientName != null) {
//                                for (ChatRoom3 room:h.joinedRoom){
//                                    if(room.getId()==roomRef0){ //same room
//                                        h.os.println("CHAT:" + roomRef0);
//                                        h.os.println("CLIENT_NAME:" + clientName);
//                                        h.os.println("MESSAGE:" + msg+"\n");
//                                        h.os.flush();
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//            }
//
//
//
//        } catch(UnknownHostException e) {
//            e.printStackTrace();
//        } catch(IOException e) {
//            e.printStackTrace();
//
//        }finally {
//            //close connections
//            try {
//
//                clientSocket.close();
//                if(is != null)is.close();
//                if(os != null)os.close();
//                // System.out.println("clientsocket close");
//
//
//
//            } catch(IOException e) {
//                e.printStackTrace();
//
//            }
//
//        }
//
//
//
//    }
//
//
//
//    @Override
//    public int compareTo(ClientThread3 o) {
//        return 1;
//    }
//}
//
//class ChatRoom3 implements Comparable{
//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    private int id;
//    private String name;
//
//    public ChatRoom3(int id, String name) {
//        this.id = id;
//        this.name = name;
//    }
//
//
//    @Override
//    public int compareTo(Object o) {
//        ChatRoom3 other=(ChatRoom3)o;
//        return this.getId()-((ChatRoom3) o).getId();
//    }
//
//
//}
