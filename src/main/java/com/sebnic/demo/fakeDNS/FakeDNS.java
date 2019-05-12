package com.sebnic.demo.fakeDNS;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class FakeDNS extends SimpleChannelInboundHandler<DatagramPacket> {

    private final String host;

    private final int port;

    public void run() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    protected void initChannel(NioDatagramChannel channel) {
                        channel.pipeline().addLast(FakeDNS.this);
                    }
                });
        bootstrap.bind(port).sync().channel().closeFuture().await();
    }

    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws UnsupportedEncodingException {
        // String request = packet.content().toString(Charset.defaultCharset());

        // log.warn(getDomainName(request));

        ByteBuf content = packet.content();
        short transactionId = content.readShort();
        log.info("TransactionId: {}", transactionId);
        log.info("Flags: {}", content.readShort());
        log.info("Questions: {}", content.readShort());
        log.info("Answer: {}", content.readShort());
        log.info("Authority RRs: {}", content.readShort());
        log.info("additional RRs: {}", content.readShort());
        log.info("end DNS headers: {}", content.readByte());
        StringBuffer domainName = new StringBuffer();
        byte currentByte = content.readByte();
        while(currentByte != '\0') {
            domainName.append((char)currentByte);
            currentByte = content.readByte();
        }
        log.info("Domain name: {}", domainName);
        log.info("Type resource: {}", content.readShort());
        log.info("Class: {}", content.readShort());

        byte[] hostBytes = "test.orange.com".getBytes();
        ByteBuffer response = ByteBuffer.allocate(200);
        // ByteBuffer response = ByteBuffer.allocate(13 + domainName.toString().getBytes().length + 15 + 5 + hostBytes.length + 2);
        response.putShort(transactionId);
        response.putShort((short)33152);
        response.putShort((short)1);
        response.putShort((short)1);
        response.putShort((short)0);
        response.putShort((short)0);
        response.put((byte)4);
        response.put(domainName.toString().getBytes());
        response.put((byte)0);
        response.putShort((short)15);
        response.putShort((short)1);
        response.putShort((short)49164);
        response.putShort((short)15);
        response.putShort((short)1);
        response.putInt(300);

        response.putShort((short)(hostBytes.length+4));
        response.putShort((short)50);
        response.put((byte)4);
        response.put(hostBytes);
        response.put((byte)0);

        ctx.write(new DatagramPacket(Unpooled.copiedBuffer(response.array()), packet.sender()));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private String getDomainName(String request) {
        byte[] noFormattedDomainName = request.substring(13).getBytes();
        StringBuffer stringBuffer = new StringBuffer();
        for(int index = 0 ; index < noFormattedDomainName.length ; index++) {
            byte currentByte = noFormattedDomainName[index];
            if (currentByte < 32) {
                currentByte = 46;
            }
            stringBuffer.append((char)currentByte);
        }
        return stringBuffer.toString();
    }
}