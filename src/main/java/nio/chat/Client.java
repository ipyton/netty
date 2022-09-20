package nio.chat;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) throws IOException {
        Runnable runnable = new Runnable() {
            int i = 0;
            @Override
            public void run() {
                try {
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(8888));
                    ByteBuffer bf = ByteBuffer.allocate(1024);
                    int p = 0;
                    synchronized (Client.class){
                        p = i++;

                    }
                    while(true){
                        channel.write(ByteBuffer.wrap(("hello I am"+ (new Integer(p))).getBytes(StandardCharsets.UTF_8)));
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        Thread.currentThread().sleep(1000);
                        System.out.println(p+"A");
                        channel.read(buffer);
                        System.out.println(p+"B");
                        System.out.println((new String(buffer.array())).trim());
                        Thread.currentThread().sleep(1000);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        ExecutorService executorService = Executors.newCachedThreadPool();
        for(int i = 0;i<10;i++) {
            executorService.execute(runnable);
        }

    }
}
