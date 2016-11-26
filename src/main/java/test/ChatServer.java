package test;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChatServer {
    private static ExecutorService executorService = null;
    public static ArrayList<ChatRoom> chatRooms = new ArrayList<ChatRoom>();
    public static HashMap<String, ServerThread> userMap = new HashMap<String, ServerThread>();

    final int POOL_SIZE=10;

    private ServerSocket serverSocket = null;
    private String localIp = "";

    static int id = 0;
    static int mapId = 0;
    static boolean setDown = false;

    public ChatServer() throws IOException {

        try {
            executorService=Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*POOL_SIZE);
            serverSocket = new ServerSocket(2222);
            localIp = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Server start");
            while (true) {
                Socket cs = serverSocket.accept();
                if(setDown == true) {
                    break;
                }
                ServerThread thread = new ServerThread(cs,id);
                id++;
                executorService.execute(thread);

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            if(serverSocket != null){
                serverSocket.close();
            }
        }
        executorService.shutdown();
        serverSocket.close();


    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        try {
            new ChatServer();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }



    public PrintWriter getWriter(Socket socket) throws IOException {
        OutputStream socketOut = socket.getOutputStream();
        return new PrintWriter(socketOut, true);
    }

    public BufferedReader getReader(Socket socket) throws IOException {
        InputStream socketIn = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(socketIn));
    }

    class ServerThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private int join_id;
        private String client_name = "";

        public ServerThread(Socket socket,int join_id) throws IOException {
            this.socket = socket;
            reader = getReader(socket);
            writer = getWriter(socket);
            this.join_id = join_id;
        }



        @SuppressWarnings("deprecation")
        public void run() {

            String info;
            try {
                while ((info = reader.readLine()) != null) {

                    if (info.startsWith(Utility.JOIN_CHATROOM)) {
                        String[] mString = addToString(4, info);

                        if(this.getClient_name().equals("")){
                            this.setClient_name(mString[3].split(":")[1]);
                        }

                        respondJoin(mString, this, writer);
                    } else if (info.startsWith(Utility.LEAVE_CHATROOM)) {
                        String[] mString = addToString(3, info);
                        try {
                            int roomR = Integer.parseInt(info.trim().substring(info.indexOf(" ") + 1));
                            String leave = chatRooms.get(roomR).getChatRoomName();
                            boolean check = false;
                            Iterator iter = userMap.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                String key = (String) entry.getKey();
                                ServerThread value = (ServerThread) entry.getValue();
                                if (key.substring(0, key.indexOf(":")).equals(leave) && value.equals(this)) {
                                    System.out.println(respondLeave(mString));
                                    writer.println(respondLeave(mString));
                                    writer.flush();
                                    check = true;
                                    String leaveMsg = leaveMsgFormate(mString, roomR);
                                    pushToAll(leave, leaveMsg, this, writer);
                                    synchronized (userMap) {
                                        userMap.remove(key);
                                    }

                                    break;
                                }
                            }

                            if (check == false) {
                                writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
                                        + ":" + "You didn't join that chatroom");

                                writer.flush();
                            }
                        } catch (NumberFormatException e) {
                            // TODO: handle exception
                            writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
                                    + ":" + "Invalid room reference!");
                            writer.flush();
                        }

                    } else if (info.startsWith(Utility.CHAT)) {
                        String[] mString = addToString(4, info);
                        try {
                            int index = Integer.parseInt(info.split(":")[1].trim());
                            for(ChatRoom chatRoom: chatRooms){
                                System.out.println(chatRoom.toString());
                            }
                            String chatRoomName = chatRooms.get(index).getChatRoomName();

                            pushToAll(chatRoomName,
                                    info + Utility.SEGEMENT + mString[2] + Utility.SEGEMENT + mString[3] + "\n", this, writer);

                        } catch (NullPointerException e) {
                            writer.println(Utility.ERROR_CODE + ":2" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
                                    + ":" + "Invalid room reference!");
                            writer.flush();
                        } catch (NumberFormatException e) {
                            // TODO: handle exception
                            writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
                                    + ":" + "Invalid room reference!");
                            writer.flush();
                        }

                    } else if (info.startsWith(Utility.DISCONNECT)) {

                        String[] mStrings = addToString(3, info);
                        HashMap<String, ServerThread> removeMap = new HashMap<String, ServerThread>();


                        Iterator iter = userMap.entrySet().iterator();
                        while (iter.hasNext()) {
                            Map.Entry entry = (Map.Entry) iter.next();
                            String key = (String) entry.getKey();
                            ServerThread value = (ServerThread) entry.getValue();
                            if(mStrings[2].split(":")[1].equals(value.getClient_name())){
                                removeMap.put(key,value);
                                pushToAll(key.split(":")[0], leaveMsgFormate(mStrings, Integer.parseInt(key.split(":")[2])), this, writer);
                            }
                        }
                        synchronized (userMap) {
                            Iterator iter2 = removeMap.entrySet().iterator();
                            while (iter2.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter2.next();
                                String key = (String) entry.getKey();
                                ServerThread value = (ServerThread) entry.getValue();
                                userMap.remove(key);

                            }
                        }


                    } else if(info.startsWith("HELO BASE_TEST")){
                        writer.println(
                                info + "\n" + "IP:" + localIp + "\n" + "Port: " + 54321 + "\nStudentID: 16308222");
                        writer.flush();
                    }else if(info.startsWith("KILL_SERVICE")){

                        setDown = true;
                        new Socket("localhost", 54321);

                        break;

                    }
                }
                socket.close();
                reader.close();
                writer.close();
                System.out.println("client closed");

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        }



        private String leaveMsgFormate(String[] mString, int roomR) {
            String leaveMsg = Utility.CHAT + ":" + roomR + Utility.SEGEMENT + mString[2]
                    + Utility.SEGEMENT + Utility.MESSAGE + ":"
                    + mString[2].split(":")[1]
                    + " has left this chatroom.\n";
            return leaveMsg;
        }

        private String[] addToString(int size, String start) throws IOException {
            String[] mString = new String[size];
            mString[0] = start;
            System.out.println(start);
            for (int i = 1; i < size; i++) {
                mString[i] = reader.readLine();

                System.out.println(mString[i]);

            }

            return mString;
        }

        public void respondJoin(String[] mString, ServerThread s, PrintWriter writer) throws IOException {

            String respond = "";
            String joinRoom = mString[0].split(":")[1];

            System.out.println(checkJoin(joinRoom.trim(), s));
            if (checkJoin(joinRoom.trim(), s) == false) {
                int roomRef = 0;

                ChatRoom chatRoom = null;

                if (checkChatRoom(joinRoom) == null) {
                    chatRoom = new ChatRoom(chatRooms.size(), joinRoom);

                    synchronized (chatRooms) {
                        chatRooms.add(chatRoom);
                    }

                }else{
                    chatRoom = checkChatRoom(joinRoom);
                }

                roomRef = chatRoom.getChatRoomId();

                mapId++;
                respond = Utility.JOINED_CHATROOM + ":" + joinRoom + Utility.SEGEMENT + Utility.SERVER_IP + ":" + localIp
                        + Utility.SEGEMENT + Utility.PORT + ":" + 54321 + Utility.SEGEMENT + Utility.ROOM_REF + ":"
                        + roomRef + Utility.SEGEMENT + Utility.JOIN_ID + ":" + mapId;


                synchronized (userMap) {

                    userMap.put(joinRoom + ":" + mapId + ":"+roomRef, s);
                    System.out.println("join room: " + joinRoom);
                    System.out.println("user map size: " + userMap.size());
                }

                writer.println(respond);

                String joinInform = Utility.CHAT + ":" + roomRef + Utility.SEGEMENT + mString[3] + Utility.SEGEMENT
                        + Utility.MESSAGE + ":" + mString[3].split(":")[1]
                        + " has joined this chatroom.\n";
                pushToAll(joinRoom, joinInform, s, writer);

            } else {
                respond = Utility.ERROR_CODE + ":0" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION + ":"
                        + "You have joined the chatroom!";
                writer.println(respond);
            }

            writer.flush();

        }

        private synchronized ChatRoom checkChatRoom(String room) {
            for (ChatRoom chatRoom : chatRooms) {
                if (chatRoom.getChatRoomName().equals(room)) {
                    return chatRoom;
                }
            }
            return null;
        }

        private synchronized boolean checkJoin(String room, ServerThread server) {
            Iterator iter = userMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                System.out.println("key: " + key);
                ServerThread serverThread = (ServerThread) entry.getValue();
                if (room.equals(key.split(":")[0]) && serverThread.equals(server)) {
                    return true;
                }
            }

            return false;

        }

        private synchronized void pushToAll(String room, String msg, ServerThread s, PrintWriter writer) throws IOException {
            Iterator iter = userMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                ServerThread serverThread = (ServerThread) entry.getValue();

                if (key.split(":")[0].equals(room)) {
                    System.out.println(key);
                    writer = getWriter(serverThread.getSocket());
                    writer.println(msg);
                }
            }


        }

        private String respondLeave(String[] mString) {

            return Utility.LEFT_CHATROOM + ":" + mString[0].split(":")[1] + Utility.SEGEMENT
                    + Utility.JOIN_ID + ":" + mString[1].split(":")[1];

        }

        public Socket getSocket() {
            return socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }



        public int getJoin_id() {
            return join_id;
        }



        public void setJoin_id(int join_id) {
            this.join_id = join_id;
        }



        public String getClient_name() {
            return client_name;
        }



        public void setClient_name(String client_name) {
            this.client_name = client_name;
        }



    }

}
 class ChatRoom {
    private int chatRoomId;
    private String chatRoomName;

    public ChatRoom(int chatRoomId, String chatRoomName) {
        super();
        this.chatRoomId = chatRoomId;
        this.chatRoomName = chatRoomName;
    }
    public int getChatRoomId() {
        return chatRoomId;
    }
    public void setChatRoomId(int chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
    public String getChatRoomName() {
        return chatRoomName;
    }
    public void setChatRoomName(String chatRoomName) {
        this.chatRoomName = chatRoomName;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + chatRoomId;
        result = prime * result + ((chatRoomName == null) ? 0 : chatRoomName.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChatRoom other = (ChatRoom) obj;
        if (chatRoomId != other.chatRoomId)
            return false;
        if (chatRoomName == null) {
            if (other.chatRoomName != null)
                return false;
        } else if (!chatRoomName.equals(other.chatRoomName))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ChatRoom [chatRoomId=" + chatRoomId + ", chatRoomName=" + chatRoomName + "]";
    }




}

class User{
    private String name;
    private int ip;

    public User(String name, int ip) {
        this.name = name;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIp() {
        return ip;
    }

    public void setIp(int ip) {
        this.ip = ip;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ip;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (ip != other.ip)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }



}

 class Utility {
    public static String JOIN_CHATROOM = "JOIN_CHATROOM";
    public static String SERVER_IP = "SERVER_IP";
    public static String PORT = "PORT";
    public static String ROOM_REF = "ROOM_REF";
    public static String JOIN_ID = "JOIN_ID";

    public static String JOINED_CHATROOM = "JOINED_CHATROOM";

    public static String LEAVE_CHATROOM = "LEAVE_CHATROOM";
    public static String LEFT_CHATROOM = "LEFT_CHATROOM";

    public static String DISCONNECT = "DISCONNECT";
    public static String CLIENT_NAME = "CLIENT_NAME";

    public static String CHAT = "CHAT";
    public static String MESSAGE = "MESSAGE";

    public static String ERROR_CODE = "ERROR_CODE";
    public static String ERROR_DESCRIPTION = "ERROR_DESCRIPTION";

    public static String SEGEMENT = "\n";


    public static String dispatchMessage(String message){
        String[] mStrings = message.split("@");
        String m = "";
        for(String string: mStrings){
            m += string + "\n";
        }

        return m;
    }
}