package nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NioServer {
    //问题 NIO事件种类中connect valid这些有什么用？

    public static void main(String[] args) throws IOException {
        ServerSocketChannel socketChannel = ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(8888));
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        int i = 0;

        while(true){
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            System.out.println(i++);
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();//不remove可以吗？也可以
                if(key.isReadable()){
                    SocketChannel channel = (SocketChannel)key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(20);
                    channel.read(buf);
                    buf.flip();
                    System.out.println("收到了"+new String(buf.array(), Charset.forName("utf-8")));
                    buf.clear();

                }
                if(key.isAcceptable()){
                    SocketChannel accept = socketChannel.accept();
                    accept.configureBlocking(false);
                    accept.register(selector,SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                }
                iterator.remove();//会修改容器中的内容


            }
        }

    }
}
