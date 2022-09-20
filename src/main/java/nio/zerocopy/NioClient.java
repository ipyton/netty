package nio.zerocopy;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;

import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class NioClient {
    public static void main(String[] args) throws Exception {
        SocketChannel channel= SocketChannel.open();
        FileChannel fileChannel = new FileInputStream("a.txt").getChannel();
        channel.connect(new InetSocketAddress(8888));
        fileChannel.transferTo(0,fileChannel.size(), (WritableByteChannel) channel);

    }
}
