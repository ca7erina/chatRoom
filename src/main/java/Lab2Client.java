import java.io.*;
import java.net.Socket;

public class Lab2Client
{
    public static void main(String[] args) throws IOException {
        try {
            Socket clientSocket = new Socket("127.0.0.1",5656);

            // reader is the input stream from the server
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
            DataInputStream is= new DataInputStream(clientSocket.getInputStream());
            PrintStream os = new PrintStream(clientSocket.getOutputStream());


           // os.println("HELO text\n");
            os.println("KILL_SERVICE");
            String line;
            while ( (line = is.readLine()) != null){
                System.out.println(line);
            }
            os.close();
            is.close();
            clientSocket.close();

        }
        catch (IOException ioe) {
            System.err.println(ioe);
        }
    }
}