package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class ScatteringAndGathering {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress add = new InetSocketAddress("localhost",8888);
        serverSocketChannel.bind(add);
        ByteBuffer[] bfs = new ByteBuffer[2];
        bfs[0] = ByteBuffer.allocate(2);
        bfs[1] = ByteBuffer.allocate(3);
        SocketChannel channel = serverSocketChannel.accept();
        channel.read(bfs);
        Arrays.asList(bfs).stream().map(buf->
                buf.flip()).map(buffer ->buffer.mark()+" ").forEach(System.out::println);
        channel.write(bfs);
        channel.close();
        serverSocketChannel.close();







    }
}
