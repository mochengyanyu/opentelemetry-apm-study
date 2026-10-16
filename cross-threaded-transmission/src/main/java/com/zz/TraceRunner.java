package com.zz;

public class TraceRunner<T> implements Runnable {

    private final T snapshot;

    private final ThreadLocal<T> threadLocal;

    private final Runnable runnable;

    public TraceRunner(Runnable runnable, ThreadLocal<T> threadLocal) {
        this.runnable = runnable;
        this.snapshot = threadLocal.get();
        this.threadLocal = threadLocal;
    }

    public static <T> TraceRunner<T> wrap(Runnable runnable, ThreadLocal<T> threadLocal) {
        return new TraceRunner<>(runnable, threadLocal);
    }

    @Override
    public void run() {
        //先进行备份
        T backup = threadLocal.get();
        if (snapshot != null) {
            threadLocal.set(snapshot);
        } else {
            threadLocal.remove();
        }
        try {
            runnable.run();
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
