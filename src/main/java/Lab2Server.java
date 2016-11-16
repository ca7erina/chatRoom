import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by chenxiaoxue
 * Multithread(threadpool)
 */
public class Lab2Server {


    static private int port;
    static private int poolSize=5;

    public static void main(String[] args) {
        port=5656;

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ServerSocket serverSocket=null;
        try {
            serverSocket = new ServerSocket(port);
            while(true){
                Socket socket = serverSocket.accept();
//                socket.setSoTimeout(10000); // inputstream's read times out if no data came after 10 seconds
                executor.execute(new ConnectionHandler2(socket));
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished all threads");

    }

}

class ConnectionHandler2 implements Runnable {

    private Socket client;

    public ConnectionHandler2(Socket client) {
        this.client = client;
    }

    public void run() {
        System.out.println(Thread.currentThread().getName()+" Start");
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new PrintWriter(client.getOutputStream(), true);

            writer.println("Hello! \nIP:"+client.getRemoteSocketAddress()+"\nStudent ID:"+"16302007");
//                client.close();

            while(true) {// The read loop. Code only exits this loop if connection is lost / client disconnects
                String line = reader.readLine();
                if(line == null) break;
                writer.println("Echo: " + line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(reader != null) reader.close();
                if(writer != null) writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.out.println(Thread.currentThread().getName()+" End.");
    }

}

