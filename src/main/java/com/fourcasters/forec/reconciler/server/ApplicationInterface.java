package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.*;

public interface ApplicationInterface {

    int select();
    boolean enqueue(SelectorTask task);
	ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long delay, long period, TimeUnit tu);
    boolean submit(Runnable r);
	int taskSize();
    void registerEventHandler(EventHandler handler);
    int handleEvents();
    void close();
}
