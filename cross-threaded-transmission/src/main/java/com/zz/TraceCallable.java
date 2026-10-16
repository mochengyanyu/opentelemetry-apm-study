package com.zz;

import java.util.concurrent.Callable;

public class TraceCallable<T> implements Callable<T> {

    private final T snapshot;

    private final ThreadLocal<T> threadLocal;

    private final Callable<T> callable;

    public TraceCallable(Callable<T> callable, ThreadLocal<T> threadLocal) {
        this.callable = callable;
        this.snapshot = threadLocal.get();
        this.threadLocal = threadLocal;
    }

    public static <T> TraceCallable<T> wrap(Callable<T> callable, ThreadLocal<T> threadLocal) {
        return new TraceCallable<>(callable, threadLocal);
    }

    @Override
    public T call() throws Exception {
        //先进行备份
        T backup = threadLocal.get();
        if (snapshot != null) {
            threadLocal.set(snapshot);
        } else {
            threadLocal.remove();
        }
        try {
            return callable.call();
        } finally {
            if (backup != null) {
                //这里进行还原
                threadLocal.set(backup);
            } else {
                threadLocal.remove();
            }
        }
    }
}
