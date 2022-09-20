package netty.RPC;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;
import nio.chat.Client;
import nio.chat.Server;
import org.junit.Test;
import sun.misc.Cache;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRPCTest {

    @Test
    public void startServer() throws Exception{
        Car car = new Car();
        Plane plane = new Plane();
        Dispatcher dis = new Dispatcher();


        dis.register(Car.class.getName(),car);
        dis.register(plane.getClass().getName(),plane);

        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap svb = new ServerBootstrap();

        ChannelFuture bind = svb.group(boss,worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ServerDecoder());
                        pipeline.addLast(new ServerRequestHandler(dis));
                    }
                })
                .bind(new InetSocketAddress(8888));
        bind.sync().channel().closeFuture().sync();//????
    }



    @Test
    public void startClients() throws Exception{
        //模拟client
        AtomicInteger num = new AtomicInteger(0);
        int size = 50;
        Thread[] threads = new Thread[size];

        for(int i = 0;i<size;i++){
            threads[i] = new Thread(()->{
                Car car = proxyGet(Car.class);
                String arg = "hello" + num.incrementAndGet();
                String res = car.drive(arg);
                System.out.println(res);
            });
        }

        for(Thread thread:threads){
            thread.start();
        }

        try {
            System.in.read();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }



    public static MyHeader createHeader(byte[] msg){
        MyHeader header = new MyHeader();
        header.setFlag(0x333333);
        header.setDataLength(msg.length);
        header.setRequestID(Math.abs(UUID.randomUUID().getLeastSignificantBits()));
        return header;
    }

    public static <T>T proxyGet(Class<T> interfaceInfo){
        //实现各个版本的动态代理
        ClassLoader loader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};
        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //1。调用服务，方法，参数，并且封装成message
                String name = interfaceInfo.getName();
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();
                MyContent myContent = new MyContent();

                myContent.setArgs(args);
                myContent.setMethodName(methodName);
                myContent.setName(name);
                myContent.setParameterTypes(parameterTypes);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                byte[] msgBody = out.toByteArray();

                //2。requestID+message 创建完整协议
                out.reset();
                MyHeader myHeader = createHeader(msgBody);
                oout = new ObjectOutputStream(out);
                oout.writeObject(myHeader);
                byte[] msgHeader = out.toByteArray();


                //3。连接池 取得连接

                ClientFactory factory = ClientFactory.getFactory();
                //4。发送

                //5.回来，将代码执行到这里


                return null;
            }
        });
    }
}

class ServerDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while(in.readableBytes()>=110){//??110是怎么来的？？？
            byte[] bytes  = new byte[110];
            //internalBuffer();

        }
    }
}

class ServerRequestHandler extends ChannelInboundHandlerAdapter{
    Dispatcher dis;

    public ServerRequestHandler(Dispatcher dis){
        this.dis = dis;
    }

    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception{
        //这边server收到请求后开始处理，执行方法
        PackMsg requestPkg = (PackMsg) msg;
        String ioThreadName = Thread.currentThread().getName();
        ctx.executor().execute(new Runnable() {
                    @Override
                    public void run() {
                        String serviceName = requestPkg.content.getName();
                        String method = requestPkg.content.getMethodName();
                        Object c = dis.get(serviceName);
                        Class<?> clazz = c.getClass();
                        Object res=null;

                        try {
                            Method m = clazz.getMethod(method,requestPkg.content.parameterTypes);
                            res = m.invoke(c,requestPkg.content.getArgs());
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                        MyContent myContent = new MyContent();
                        myContent.setRes((String)res);


                    }
            }

        );

    }

}

class ClientResponses extends ChannelInboundHandlerAdapter{
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackMsg responsePKG = (PackMsg) msg;
        ResponseMappingCallback.runCallBack(responsePKG);//运行回调函数
    }

}

class Dispatcher{
    public ConcurrentHashMap<String,Object> invokeMap = new ConcurrentHashMap<>();
    public void register(String k,Object obj) {
        invokeMap.put(k,obj);
    }
    public Object get(String k){
        return invokeMap.get(k);
    }
}


//spark源码
class ClientFactory{
    //一个consumer连接很多provider,每一个provider都有自己的pool
    ConcurrentHashMap<InetSocketAddress,ClientPool> outboxs = new ConcurrentHashMap<>();//每一个provider都有自己的poll
    int poolSize = 1;
    NioEventLoopGroup clientWorker;
    Random rand = new Random();
    private static final ClientFactory factory = new ClientFactory();

    private ClientFactory(){}

    public static ClientFactory getFactory() {
        return factory;
    }

    public synchronized NioSocketChannel getClient(InetSocketAddress address) throws Exception {
        ClientPool clientPool = outboxs.get(address);
        if(clientPool==null){
            outboxs.putIfAbsent(address,new ClientPool(poolSize));
            clientPool = outboxs.get(address);
        }
        int i = rand.nextInt(poolSize);
        if(clientPool.clients[i]!=null && clientPool.clients[i].isActive()){
            return clientPool.clients[i];
        }
        synchronized (clientPool.lock[i]){
            return clientPool.clients[i] = createClient(address);
        }
    }


    private NioSocketChannel createClient(InetSocketAddress address) throws Exception{
        clientWorker = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelFuture connect = bs.group(clientWorker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ServerDecoder());
                        pipeline.addLast(new ClientResponses());
                    }
                })
                .connect(address);
        NioSocketChannel client =(NioSocketChannel) connect.sync().channel();
        return client;

    }

}

class ResponseMappingCallback{
    static ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();

    public static void addCallback(long requestID,CompletableFuture cb) {
        mapping.putIfAbsent(requestID,cb);
    }

    public static void runCallBack(PackMsg msg){
        CompletableFuture cf = mapping.get(msg.header.getRequestID());
        cf.complete(msg.getContent().getRes());
        removeCB(msg.header.getRequestID());
    }

    public static void removeCB(long requestID){
        mapping.remove(requestID);
    }
}



//用来存储用户的连接
class ClientPool{
    NioSocketChannel[] clients;
    Object[] lock;
    ClientPool(int size){
        clients = new NioSocketChannel[size];//init 连接是空的
        lock = new Object[size];
        for(int i = 0;i<size;i++){
            lock[i] = new Object();
        }
    }
}

class MyHeader implements Serializable{
    int flag;
    long requestID;
    int dataLength;

    MyHeader() {
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }
}

class MyContent implements Serializable{
    String name;
    String methodName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    Class<?>[] parameterTypes;
    Object[] args;
    String res;

}

interface Movable{
    public String drive(String msg);
}

interface Flyable{
    public String fly(String msg);
}

class Car implements Movable{

    @Override
    public String drive(String msg) {
        return msg+"CAR";
    }
}

class Plane implements Flyable{

    @Override
    public String fly(String msg) {
        return msg+"PLANE";
    }
}



class PackMsg{
    MyHeader header;
    MyContent content;

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public MyContent getContent() {
        return content;
    }

    public void setContent(MyContent content) {
        this.content = content;
    }

    public PackMsg(MyHeader header, MyContent content) {
        this.header = header;
        this.content = content;
    }


}
