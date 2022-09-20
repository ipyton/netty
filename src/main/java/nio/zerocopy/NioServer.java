package nio.zerocopy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class NioServer {
    public static void main(String[] args) throws Exception{
        ServerSocketChannel channel = ServerSocketChannel.open();
        FileChannel fileChannel = new FileOutputStream("abc.txt").getChannel();
        channel.bind(new InetSocketAddress(8888));
        SocketChannel inputChannel= channel.accept();
        fileChannel.transferFrom(inputChannel,0,fileChannel.size());

    }
}
