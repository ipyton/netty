package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

public class LowerLevel {
    public static void main(String[] args) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(8, 20);
        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);

    }

    public static void print(ByteBuf buf){
        
        System.out.println(buf.isReadable());
        System.out.println(buf.readerIndex());
        System.out.println(buf.readableBytes());
        System.out.println(buf.isWritable());
        System.out.println(buf.isDirect());//堆外还是堆内

    }

    public void loopExecutor() throws Exception{
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup(1);//当成线程池

        eventExecutors.execute(()->{
            System.out.println("http");
        });
    }

    public void clientMode() throws InterruptedException {
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        NioSocketChannel client = new NioSocketChannel();
        thread.register(client);//epoll 注册监听事件

        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());

        //建立连接
        ChannelFuture connect = client.connect(new InetSocketAddress(9090));
        ChannelFuture sync = connect.sync();

        //发送数据
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server".getBytes(StandardCharsets.UTF_8));
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();

        //关闭连接
        sync.channel().closeFuture().sync();
    }

    public void serverMode() throws InterruptedException {
        //纯手写
        NioServerSocketChannel server = new NioServerSocketChannel();
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        thread.register(server);
        ChannelPipeline pipeline = server.pipeline();
        pipeline.addLast(new MyAcceptHandler(thread,new ChannelInit()));//需要注册到多路复用器，MyHandler不共享的话，就会创建大量的handler违背设计原则，因此要加sharable
        ChannelFuture bind = server.bind(new InetSocketAddress(8888));
        bind.sync().channel().closeFuture().sync();
        System.out.println("closed");
    }

    public void nettyClient() throws InterruptedException{
        NioEventLoopGroup client = new NioEventLoopGroup();
        Bootstrap bs = new Bootstrap();//自动监听连接
        ChannelFuture connect = bs.group(client)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MyInHandler());
                    }
                })
                .connect(new InetSocketAddress(8888));
        Channel channel = connect.sync().channel();
        ByteBuf buf = Unpooled.copiedBuffer("hello server".getBytes(StandardCharsets.UTF_8));
        ChannelFuture send = channel.writeAndFlush(buf);
        send.sync();
        channel.closeFuture().sync();
    }

    public void nettyServer() throws Exception{
        NioEventLoopGroup group = new NioEventLoopGroup();
        ServerBootstrap bs = new ServerBootstrap();
        ChannelFuture bind = bs.group(group,group)
                .channel(NioServerSocketChannel.class)//起什么作用？？？？
                .childHandler(new ChannelInitializer<NioSocketChannel>(){
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new MyInHandler());
                    }
                })
                .bind(new InetSocketAddress(8888));
        bind.sync().channel().closeFuture().sync();
    }
}

class MyAcceptHandler extends ChannelInboundHandlerAdapter{
    private final EventLoopGroup selector;
    private final ChannelHandler handler;
    MyAcceptHandler(EventLoopGroup grp,ChannelHandler handler){
        selector = grp;
        this.handler = handler;

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //在不同的通道读到的东西不同
        SocketChannel msg1 = (SocketChannel) msg;
        selector.register(msg1);
        ChannelPipeline pipeline = msg1.pipeline();
        pipeline.addLast(handler);//这个handler用来处理socket
    }
}

@ChannelHandler.Sharable
class ChannelInit extends ChannelInboundHandlerAdapter{
    //它会被加在新channel的最前端,它是复用的

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline p = client.pipeline();
        p.addLast(new MyInHandler());
        ctx.pipeline().remove(this);//只有第一次来的请求经过ChannelInit，当执行过一次之后就不再执行了
    }
}



//@ChannelHandler.Sharable 设计默认内部是要给不同的管道handler实例（同一个类的）如果说发现多个管道用的是同一个handler的话就会报错，因此需
// 要使用这个注解来使这个对象可以被复用，来做一些全局性的统计工作
class MyInHandler extends ChannelInboundHandlerAdapter{
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        CharSequence str = buf.getCharSequence(0,buf.readableBytes(), CharsetUtil.UTF_8);
        System.out.println(str);
        ctx.writeAndFlush(buf);//写回
    }
}


class ClientInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    }
}

class ServerInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("registered");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        SocketChannel msg1 = (SocketChannel) msg;

    }
}


