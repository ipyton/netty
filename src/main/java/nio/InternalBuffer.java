package nio;

import java.nio.IntBuffer;

public class InternalBuffer {//观察buffer的行为方式
    public static void main(String[] args) {
        IntBuffer buffer = IntBuffer.allocate(50);
        for(int i = 0;i<buffer.capacity();i++){
            buffer.put(i*2);
            System.out.println(buffer.mark());
        }
        IntBuffer intBuffer = buffer.asReadOnlyBuffer();//变成了只读的了，但是不是copy
        buffer.flip();
        //buffer.position(20);//更改position
        for(int i = 0;i< buffer.capacity();i++){
            System.out.println(buffer.get());
            System.out.println(buffer.mark());
        }

    }
}
