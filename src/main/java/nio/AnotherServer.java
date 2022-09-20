package nio;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class AnotherServer {
    public static void main(String[] args) throws IOException, IOException {
        // 1.获取通道
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        // 2.切换非阻塞模式
        ssChannel.configureBlocking(false);
        // 3.绑定连接
        ssChannel.bind(new InetSocketAddress(9999));
        // 4.获取选择器
        Selector selector = Selector.open();
        // 5.将通道注册到选择器上，并且指定监听接收事件
        ssChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 6.使用Selector选择器轮询已经就绪好的事件
        while (selector.select() > 0) {
            // 7.获取选择器中的所有注册的通道中已经就绪好的事件
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            // 8.开始遍历这些准备好的事件
            while (it.hasNext()) {
                SelectionKey sk = it.next();
                // 9.判断这个事件具体是什么
                if (sk.isAcceptable()) {
                    // 10.直接获取当前接入的客户端通道
                    SocketChannel channel = ssChannel.accept();
                    // 11.切换非阻塞模式
                    channel.configureBlocking(false);
                    // 12.将该通道注册到选择器上
                    channel.register(selector, SelectionKey.OP_READ);
                } else if (sk.isReadable()) {
                    // 13.获取当前选择器上读就绪状态的通道
                    SocketChannel sChannel = (SocketChannel) sk.channel();
                    System.out.println("sdfsfsdfds");
                    // 14.读取数据
                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    int len = 0;
                    while ((len = sChannel.read(buf)) > 0) {
                        buf.flip();
                        System.out.println(new String(buf.array(), 0, buf.remaining())+"hello");
                        buf.clear();
                    }
                }
                // 15.取消选择键SelectionKey
                it.remove();
            }
        }
    }

}
