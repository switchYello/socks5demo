package com.start;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * @author xiaoming
 * 只有监听器成功才执行
 */
public abstract class SuccessFutureListener<V> implements FutureListener<V> {

    @Override
    public void operationComplete(Future<V> future) throws Exception {
        if (future.isSuccess()) {
            operationComplete0(future.getNow());
        }
    }

    abstract void operationComplete0(V future) throws Exception;

}