package nio.chat;

import io.netty.channel.ServerChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private Selector selector;
    private ServerSocketChannel channel;
    Server() throws IOException {
        try {
            selector = Selector.open();
            channel = ServerSocketChannel.open();
            channel.socket().bind(new InetSocketAddress(8888));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (Exception e){
            System.err.println("初始化阶段出错！！");
            e.printStackTrace();
        }
        System.out.println("initialize successfully!");
        dispatch();
    }

    public void dispatch() throws IOException {
        while(selector.select()>0){
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey next = iterator.next();
                if(next.isAcceptable()){
                    accept(next);
                }
                if(next.isReadable()){
                    readAndWrite(next);
                }
                iterator.remove();
            }

        }
    }

    public void readAndWrite(SelectionKey next){
        SocketChannel channel = (SocketChannel) next.channel();
        try{
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            System.out.println("正在读取");
            int length = channel.read(buffer);
            if(length<0){
                System.out.println("已经断开连接");
                channel.close();
                next.cancel();
                return;
            }
            System.out.println("读取成功:"+new String(buffer.array(),0,length, Charset.forName("utf-8")));
            System.out.print("正在发送:");

            for(SelectionKey key:selector.keys()) {

                if(key.channel() instanceof SocketChannel && key != next){
                    SocketChannel channel1 = (SocketChannel) key.channel();
                    String prepare = new String(buffer.array(),0,length).trim();
                    channel1.write(ByteBuffer.wrap(prepare.getBytes()));
                    System.out.print(prepare);
                }
                System.out.println();
            }
            System.out.println("发送成功");

        }
        catch (IOException ie){
            ie.printStackTrace();
        }
    }




    public void accept(SelectionKey key){
        try{
            SocketChannel accept = channel.accept();
            accept.configureBlocking(false);
            accept.register(selector,SelectionKey.OP_READ);
            System.out.println("绑定写成功！");
            System.out.println(selector.keys().size());
        }
        catch (Exception e){
            System.out.println("关闭了连接，绑定失败");
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws IOException {
        Server s = new Server();//声明即开始运行
    }
}
