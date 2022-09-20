package bio;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    //复习内容1.线程池的种类
    //2.线程安全容器
    //3.创建线程的三种方法以及future
    public static void main(String[] args) throws Exception {
        //在此处就可以发现粘包问题
        ExecutorService executorService = Executors.newCachedThreadPool();
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动了");
        while(true){
            Socket accept = serverSocket.accept();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler(accept);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        }

    }
    private static void handler(Socket socket) {
        byte[] buffer = new byte[30];
        try {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            int k = inputStream.read(buffer);
            System.out.println(k);
            while (k>=0) {
                String s = new String(buffer,0,k, Charset.forName("utf-8"));
                s = "我已经收到了" + s;
                outputStream.write(s.getBytes(StandardCharsets.UTF_8));
                System.out.println(s);
                k = inputStream.read(buffer);
                System.out.println(buffer[0]);//使用telnet连接，前两个为13和10
                System.out.println(buffer[1]);
            }
            inputStream.close();
            outputStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            System.out.println("已关闭连接");
        }

    }
}
