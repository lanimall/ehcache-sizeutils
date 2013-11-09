package org.terracotta.utils;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory{

	private String poolName;
	private int threadCounter;
	public NamedThreadFactory(String poolName) {
		this.poolName = poolName;
	}
	
    public Thread newThread(Runnable r) {
    	return new Thread(r, poolName + threadCounter++);
    }
}
