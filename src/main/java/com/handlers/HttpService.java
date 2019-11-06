package com.handlers;

import com.start.PromiseProvide;
import com.start.SuccessFutureListener;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 处理http(s)代理请求
 * 解析请求ip 端口
 */
@ChannelHandler.Sharable
public class HttpService extends ChannelInboundHandlerAdapter {

    private static Logger log = LoggerFactory.getLogger(HttpService.class);
    private PromiseProvide promiseProvide;

    public HttpService(PromiseProvide promiseProvide) {
        this.promiseProvide = promiseProvide;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        final FullHttpRequest req = (FullHttpRequest) msg;
        final ChannelPipeline p = ctx.pipeline();
        InetSocketAddress inetSocketAddress = resolveHostPort(req.headers().get("Host"));
        //创建远程连接，等待连接完成,,下面添加的回掉只有连接成功才会触发
        Promise<Channel> promise = promiseProvide.createPromise(inetSocketAddress, ctx);
        //https代理
        if (HttpMethod.CONNECT.equals(req.method())) {
            ReferenceCountUtil.release(msg);
            //https连接完成后，开始通传，删除http相关的handler
            promise.addListener(new SuccessFutureListener<Channel>() {
                @Override
                public void operationComplete0(Channel value) {
                    ctx.pipeline().addLast(new TransferHandler(value));
                    FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "OK"));
                    ctx.writeAndFlush(resp).addListener(new SuccessFutureListener<Void>() {
                        @Override
                        public void operationComplete0(Void future) {
                            removeHttpHandler(p);
                        }
                    });
                }
            });
        } else {
            req.headers().remove("Proxy-Authorization").remove("Proxy-Connection").add("Connection", "keep-alive");
            //http代理，代理后需要将原始报文继续发出去
            //这里调用channel的write方法，会从tail向head查找outHandler
            promise.addListener(new SuccessFutureListener<Channel>() {
                @Override
                public void operationComplete0(final Channel value) {
                    ctx.pipeline().addLast(new TransferHandler(value));
                    removeHttpHandler(p);
                    value.pipeline().addLast(new HttpRequestEncoder());
                    value.writeAndFlush(req).addListener(new SuccessFutureListener<Void>() {
                        @Override
                        public void operationComplete0(Void future) {
                            value.pipeline().remove(HttpRequestEncoder.class);
                        }
                    });
                }
            });
        }
    }


    private InetSocketAddress resolveHostPort(String headerHost) {
        String[] split = headerHost.split(":");
        String host = split[0];
        int port = 80;
        if (split.length > 1) {
            port = Integer.valueOf(split[1]);
        }
        return InetSocketAddress.createUnresolved(host, port);
    }


    private void removeHttpHandler(ChannelPipeline p) {
        p.remove("httpcode");
        p.remove("objectAggregator");
        p.remove("httpservice");
    }

}
