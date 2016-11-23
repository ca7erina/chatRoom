import java.io.*;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by chenxiaoxue on 11/12/16.
 */
public class Lab3ChatRoomClient  implements Runnable {

        private static Socket clientSocket = null;
        private static PrintStream os = null;
        private static DataInputStream is = null;
        private static BufferedReader inputReader = null;
        private static boolean closed = false;
        int roomRef=0;
        int joinId=0;

        public static void main(String[] args) {

            //set host and port number.
            int portNumber = 2222;
            String clientName="";
            String chatRoomName="chat4";
            String host = "localhost";


            //set up
            try {
                clientSocket = new Socket(host, portNumber);
                inputReader = new BufferedReader(new InputStreamReader(System.in));
                os = new PrintStream(clientSocket.getOutputStream());
                is = new DataInputStream(clientSocket.getInputStream());
            } catch (UnknownHostException e) {
                System.err.println("Unknown host " + host);
            } catch (IOException e) {
                System.err.println("Cannot connect to the host:  " + host);
            }

            // start client and get user input.
            if (clientSocket != null && os != null && is != null) {
                try {
                    new Thread(new Lab3ChatRoomClient()).start();
                    // send initialize msg to the server
                   os.println("JOIN_CHATROOM:"+"room1"+"\nCLIENT_IP:"+ Inet4Address.getLocalHost().getHostAddress()+"\nPORT:"+portNumber+"\nCLIENT_NAME:"+Inet4Address.getLocalHost().getHostName());
                   //test
                   //os.println("HELO BASE_TEST");

                    //test send leave msg
                   // os.println("LEAVE_CHATROOM:" + 0 +"\nJOIN_ID:" + "11"+"\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName());

                    //test disconnect
                    // os.println("DISCONNECT:" + 0 +"\nPORT:" + "11"+"\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName());

                   //test kill
                   // os.println("KILL_SERVICE");closed=true;

                    while (!closed) {
                      //  String message=inputReader.readLine().trim();
                        //send msg
                       //os.println("CHAT:" + 0+"\nJOIN_ID:" + "02"+"\nCLIENT_NAME:" + Inet4Address.getLocalHost().getHostName()+"\nMESSAGE:" + message);


                    }
                    os.close();
                    is.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("IOException:  " + e);
                }
            }
        }

        //get chat updated from the server
        public void run() {
            String responseLine;
            try {
                while ((responseLine = is.readLine()) != null) {
                    System.out.println(responseLine);
                    if (responseLine.indexOf("*** Bye") != -1){
                        break;
                    }
                }
                closed = true;
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }
