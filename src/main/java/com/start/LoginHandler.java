package com.start;

import com.utils.PasswordChecker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCountUtil;

/**
 * @author xiaoming
 */
@ChannelHandler.Sharable
public class LoginHandler extends ChannelInboundHandlerAdapter {

    public static LoginHandler INSTANCE = new LoginHandler();

    private LoginHandler() {
    }

    //验证通过则移除this，fire消息
    //否则，发送验证指令
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf b = null;
        b.forEachByte(ByteProcessor.FIND_CR);
        FullHttpRequest request = (FullHttpRequest) msg;
        if (PasswordChecker.digestLogin(request)) {
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        } else {
            ReferenceCountUtil.release(msg);
            ctx.writeAndFlush(PasswordChecker.getDigestNotLoginResponse()).addListener(ChannelFutureListener.CLOSE);
        }
    }

}