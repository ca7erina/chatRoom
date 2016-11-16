import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
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
            String host = "localhost";
            if (args.length < 2) {
                System.out.println("Now using default configuration host=" + host + ", portNumber=" + portNumber);
            } else {
                host = args[0];
                portNumber = Integer.valueOf(args[1]).intValue();
            }

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
                    while (!closed) {
                        os.println(inputReader.readLine().trim());
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
            System.out.println("why multi thread client?");
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
