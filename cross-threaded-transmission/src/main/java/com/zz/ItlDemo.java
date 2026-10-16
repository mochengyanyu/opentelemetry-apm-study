package com.zz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 验证 README 第3 章的论断：InheritableThreadLocal 为什么不能用于 APM 跨线程透传。
 * 每段输出都是真实运行结果，可直接贴进文章。
 */
public class ItlDemo {

    static final InheritableThreadLocal<String> ITL = new InheritableThreadLocal<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=== 论断 3.1：继承发生在 new Thread() 瞬间，而非 start() ===");
        demo3_1();

        System.out.println();
        System.out.println("=== 论断 3.2：线程池复用线程，值被定格在创建那一刻 ===");
        demo3_2();

        System.out.println();
        System.out.println("=== 论断 3.3：单向、一次性，父线程后续 set 不会同步 ===");
        demo3_3();
    }

    /** 3.1 new 之后、start 之前设置 -> 拿不到 */
    private static void demo3_1() throws Exception {
        ITL.remove();
        Thread t = new Thread(() ->
                System.out.println("  子线程读到: " + ITL.get() + "   <- 期望是 traceId-123，实际 null"));

        ITL.set("traceId-123");
        System.out.println("  父线程 set(\"traceId-123\") 完成，时机在 new Thread() 之后");
        t.start();
        t.join();
    }

    /** 3.2 单线程池：第二个任务应拿到 B，实际拿到 A（脏数据） */
    private static void demo3_2() throws Exception {
        ITL.remove();
        ExecutorService pool = Executors.newSingleThreadExecutor();

        ITL.set("traceId-A");
        pool.submit(() -> System.out.println("  task1 读到: " + ITL.get() + "   <- 期望 A"));

        ITL.set("traceId-B");
        pool.submit(() -> System.out.println("  task2 读到: " + ITL.get() + "   <- 期望 B，实际 A，链路串扰"));

        pool.shutdown();
        pool.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);
    }

    /** 3.3 继承是单向的：子线程 set 不会回写父线程 */
    private static void demo3_3() throws Exception {
        ITL.remove();
        ITL.set("parent-value");

        Thread t = new Thread(() -> {
            ITL.set("child-modified");
            System.out.println("  子线程内set 后读到: " + ITL.get());
        });
        t.start();
        t.join();

        System.out.println("  子线程结束，父线程读到: " + ITL.get() + "   <- 子线程的修改未回写");
    }
}