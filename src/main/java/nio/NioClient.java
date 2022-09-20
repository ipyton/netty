package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        //向服务器异步发送消息
        SocketChannel socketChannel = SocketChannel.open();
        InetSocketAddress localhost = new InetSocketAddress("localhost", 6666);
        socketChannel.configureBlocking(false);
        socketChannel.connect(localhost);
        while(!socketChannel.finishConnect()){
            System.out.println("未完成连接");
        }
            socketChannel.write(ByteBuffer.wrap("你好啊！".getBytes(StandardCharsets.UTF_8)));
            //Thread.currentThread().wait(2000);
        System.in.read();
    }
}
