import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * socket HTTP request
 */
public class Lab1Client {

    public static void main(String[] args) throws IOException, URISyntaxException {
        String urlStr = "http://www.scss.tcd.ie/~ebarrett/lectures/cs4032/echo.php";
        URI uri = new URI( urlStr);
        String host = uri.getHost( );
        String path = uri.getRawPath( )+"?message=yay!!!";
        String auth = "chenx6:Rainbow7787";

        String encodedAuth = Base64.encodeBase64String(auth.getBytes());

        try {
          //  Socket socket = new Socket(InetAddress.getByName("www.scss.tcd.ie"),80);

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket)factory.createSocket(host, 443);


            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.print("GET " + path +" HTTP1.1\r\n");
            pw.write("Host: "+host+"\r\n");
            pw.write("Authorization: Basic " + encodedAuth + "\r\n" );
            pw.write("\r\n");
            pw.flush();


            // bin is the input stream from the server
            BufferedReader bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ( (line = bin.readLine()) != null){
                System.out.println(line);
            }

            socket.close();
            bin.close();
            pw.close();
        }
        catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

}
