package com.start;

import com.brige.BrigeInit;
import com.httpservice.ProxyServiceInit;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表示整个程序上下文
 */
public class Context {
    private static Logger log = LoggerFactory.getLogger(Context.class);
    private static final String DEFAULT_ENVIRONMENT_PARAM = "param.properties";
    private Environment environment;
    private static Context contextHolder;
    private static boolean init;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private static Bootstrap b = new Bootstrap();

    static {
        b.channel(NioSocketChannel.class).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
    }

    public Context() {
        this(DEFAULT_ENVIRONMENT_PARAM, 5);
    }

    public Context(String param) {
        this(param, 5);
    }

    public Context(int threadCount) {
        this(DEFAULT_ENVIRONMENT_PARAM, threadCount);
    }

    public Context(String param, int threadCount) {
        if (init) {
            throw new RuntimeException("alread existence Context");
        }
        log.info("param fileName:{}", param);
        this.environment = new Environment(param);
        contextHolder = this;
        bossGroup = new NioEventLoopGroup(1);
        workGroup = new NioEventLoopGroup(threadCount);
        init = true;
    }

    public static Context getNow() {
        if (!init) {
            throw new RuntimeException("not find contxt");
        }
        return contextHolder;
    }

    public static Environment getEnvironment() {
        if (!init) {
            throw new RuntimeException("not find contxt");
        }
        return contextHolder.environment;
    }

    public void start() {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 256)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .childHandler(getInitializer());
            log.info("start at:{}", environment.getLocalPort());
            ChannelFuture f = b.bind(environment.getLocalPort()).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void stop() {
        workGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    public Bootstrap createBootStrap() {
        return b.clone(workGroup);
    }

    public Bootstrap createBootStrap(EventLoopGroup group) {
        return b.clone(group);
    }

    private ChannelInitializer<Channel> getInitializer() {
        log.info("proxy Type:{}", environment.getProxyType());
        if (environment.getProxyType() == Environment.ProxyType.proxy) {
            return new ProxyServiceInit();
        }
        if (environment.getProxyType() == Environment.ProxyType.brige) {
            return new BrigeInit();
        }
        throw new RuntimeException("未知配置");
    }


}
