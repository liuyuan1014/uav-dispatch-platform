package com.uav.iot.protocol;

import lombok.Data;

/**
 * 无人机自定义通信协议包
 * <p>
 * 协议结构:
 * [0-3]   Magic (4字节): 0xCAFEBABE (防非法连接)
 * [4]     Version (1字节): 协议版本
 * [5]     Command (1字节): 业务指令 (1=登录, 2=心跳, 3=GPS数据...)
 * [6-9]   Length (4字节): 数据体长度
 * [10-N]  Body (N字节): Protobuf 序列化数据
 * </p>
 */
@Data
public class UavPacket {
    // 魔数校验，用于快速拒绝非法连接
    public static final int MAGIC_NUMBER = 0xCAFEBABE;

    // 消息头
    private int magic;
    private byte version;   // 版本号
    private byte command;  // 指令类型
    private int len;      // 数据长度

    // 消息体 (具体的数据，后续我们会放 Protobuf 序列化后的字节)
    private byte[] content;
}
