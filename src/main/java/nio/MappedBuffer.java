package nio;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedBuffer {
    //在堆外内存进行修改,避免复制进操作系统
    public static void main(String[] args) throws Exception{
        RandomAccessFile raf = new RandomAccessFile("a.txt","rw");
        FileChannel channel = raf.getChannel();

        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, 20);

//        ByteBuffer bf = ByteBuffer.allocate(20);
        map.put(0,(byte)'H');
        map.put(1,(byte)'G');
        for(int i = 0;i<10;i++) map.put(i,(byte)('a'+i));
//        channel.write(bf);
        raf.close();


    }
}
