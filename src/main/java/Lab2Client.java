import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Lab2Client
{
    public static void main(String[] args) throws IOException {
        try {
            Socket sock = new Socket("127.0.0.1",5656);

            // reader is the input stream from the server
            BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            String line;
            while ( (line = reader.readLine()) != null){
                System.out.println(line);
            }


            sock.close();
        }
        catch (IOException ioe) {
            System.err.println(ioe);
        }
    }
}