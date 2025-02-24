package org.pikaqiu.jraft.basic.bytebuf;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.pikaqiu.jraft.basic.utils.JvmUtil;
import org.pikaqiu.jraft.basic.utils.Logger;

@Slf4j
public class AllocatorTest {

    @Test
    public void showUnpooledByteBufAllocator() {
        UnpooledByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        Logger.tcfo(JvmUtil.getMxMemory());

        for (int i = 0; i < 1000; i++) {
            ByteBuf buffer = allocator.directBuffer(20 * 1024 * 1024);
            Logger.tcfo(buffer);
            System.out.println("分配了 " + 20 * (i + 1) + " MB");
        }

    }
}
