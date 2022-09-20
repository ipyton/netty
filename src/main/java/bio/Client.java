package bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost",8888);
        //s.bind(new InetSocketAddress(4444));

        InputStream inputStream = s.getInputStream();
        OutputStream outputStream = s.getOutputStream();
        byte[] buffer = new byte[20];
        int len = 1;
        while(len>0){
            outputStream.write("hello".getBytes(StandardCharsets.UTF_8));
            len = inputStream.read(buffer);
            System.out.println(new String(buffer,0,len, Charset.forName("utf-8")));

        }
        inputStream.close();
        outputStream.close();
        s.close();

    }
}
