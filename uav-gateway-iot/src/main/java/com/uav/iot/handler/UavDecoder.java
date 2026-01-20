package com.uav.iot.handler;

import com.uav.iot.protocol.UavPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;


/**
 * 自定义协议解码器
 * 职责：
 * 1. 处理 TCP 粘包/拆包
 * 2. 校验协议魔数
 * 3. 将字节流转换为 UavPacket 对象
 */
@Slf4j
public class UavDecoder extends LengthFieldBasedFrameDecoder {
    public UavDecoder() {
        /**
         * maxFrameLength: 10MB (单包最大限制，防止内存溢出)
         * lengthFieldOffset: 6 (魔数4 + 版本1 + 指令1 = 6，长度字段从第6个字节开始)
         * lengthFieldLength: 4 (长度字段本身占4个字节)
         * lengthAdjustment: 0 (长度字段的值即为 Body 的长度，无需修正)
         * initialBytesToStrip: 0 (不跳过头部，我们需要在 decode 里校验魔数)
         */
        super(10 * 1024 * 1024, 6, 4, 0, 0);
    }
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 1.利用父类逻辑解决粘包问题，frame 是一个完整的、切分好的数据包
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        try {
            // 2.校验魔数（安全检查）
            int magic = frame.readInt();
            if (magic != UavPacket.MAGIC_NUMBER) {
                log.warn("检测到非法连接，魔数不匹配：{}，关闭连接：{}", magic, ctx.channel());
                ctx.close();
                return null;
            }

            // 3.封装对象
            UavPacket packet = new UavPacket();
            packet.setMagic(magic);
            packet.setVersion(frame.readByte());
            packet.setCommand(frame.readByte());

            int len = frame.readInt();
            packet.setLen(len);

            if (len > 0) {
                byte[] bytes = new byte[len];
                frame.readBytes(bytes);
                packet.setContent(bytes);
            }
            return packet;
        }catch (Exception e) {
            log.error("数据包解析异常：{}", e.getMessage());
            ctx.close();
            return null;
        }finally {
            // 4.极其重要：释放内存，防止Direct Memory Leak
            if (frame != null) {
                frame.release();
            }
        }
    }
}
