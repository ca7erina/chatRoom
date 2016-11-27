import java.io.*;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * chat room client for testing ChatRoom server
 */
public class Lab3ChatRoomClient implements Runnable {

    private static Socket clientSocket = null;
    private static PrintStream os = null;
    private static DataInputStream is = null;
    private static BufferedReader inputReader = null;
    private static boolean closed = false;


    public static void main(String[] args) {

        //set host and port number.
        int portNumber = 2222;
        String clientName = "xiaoxue";
        String chatRoomName = "room1";
        String host = "localhost";

        //set up steam
        try {
            clientSocket = new Socket(host, portNumber);
            inputReader = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
        } catch(UnknownHostException e) {
            System.err.println("Unknown host " + host);
        } catch(IOException e) {
            System.err.println("Cannot connect to the host:  " + host);
        }

        // start client and get user input.
        if(clientSocket != null && os != null && is != null) {
            try {
                new Thread(new Lab3ChatRoomClient()).start();
                // send join msg to the server
                os.println("JOIN_CHATROOM:" + chatRoomName + "\nCLIENT_IP:" + Inet4Address.getLocalHost().getHostAddress() + "\nPORT:" + portNumber + "\nCLIENT_NAME:" + clientName);
//                os.println("JOIN_CHATROOM:" + "room2" + "\nCLIENT_IP:" + Inet4Address.getLocalHost().getHostAddress() + "\nPORT:" + portNumber + "\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName());

                //base test
//                os.println("HELO BASE_TEST");
                //random string test
//                os.println("sadfniwoqnfkw");
                //test send leave msg
//                os.println("LEAVE_CHATROOM:" + 0 + "\nJOIN_ID:" + "11" + "\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName());
                //test disconnect
//                os.println("DISCONNECT:" + 0 + "\nPORT:" + "11" + "\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName());
                //test kill service
//                os.println("KILL_SERVICE\n");//closed=true;
                while(!closed) {
                    String message = inputReader.readLine().trim();
                    os.println("CHAT:" + 0 + "\nJOIN_ID:" + "02" + "\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName() + "\nMESSAGE:" + message + "\n\n");
                }
                os.close();
                is.close();
                clientSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    //read from server
    public void run() {
        String responseLine;
        try {
            while((responseLine = is.readLine()) != null) {
                System.out.println("-" + responseLine);
            }
            closed = true;
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
