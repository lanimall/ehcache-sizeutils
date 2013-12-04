package org.terracotta.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheClearerLauncher {
	private static Logger log = LoggerFactory.getLogger(CacheClearerLauncher.class);
	private static final boolean isDebug = log.isDebugEnabled();

	private final CacheManager cacheManager;
	private final String[] myCacheNames;

	private static final int CACHE_POOLSIZEMAX = 10;

	private final ExecutorService cacheOpsService;

	public CacheClearerLauncher(final String cacheName) {
		this.cacheManager = CacheUtils.getCacheManager();
		if(cacheName == null || !cacheManager.cacheExists(cacheName)){
			this.myCacheNames = new String[]{}; //will do nothing
		} else {
			this.myCacheNames = new String[]{cacheName};
		}
		
		if(myCacheNames.length > 0){
			this.cacheOpsService = Executors.newFixedThreadPool((myCacheNames.length>CACHE_POOLSIZEMAX)?CACHE_POOLSIZEMAX:myCacheNames.length, new NamedThreadFactory("Cache Ops Pool"));
		} else {
			this.cacheOpsService = null;
		}
	}

	public void clearObjectInCache(){
		for(String cacheName : myCacheNames){
			if(cacheManager.cacheExists(cacheName)){
				Cache myCache = cacheManager.getCache(cacheName);
				if(null != myCache){
					cacheOpsService.submit(new CacheClearOp(myCache));
				}
			}
		}
	}

	public void clearObjectInCacheSerial() throws InterruptedException{
		for(String cacheName : myCacheNames){
			if(cacheManager.cacheExists(cacheName)){
				Cache myCache = cacheManager.getCache(cacheName);
				if(null != myCache){
					System.out.print(String.format("Before Clear - Cache Name: %s - Cache Size: %d", myCache.getName(), myCache.getSize()));

					myCache.removeAll();

					System.out.print("Waiting a bit to let the clustered cache clear its entries...");
					Thread.sleep(5000);

					System.out.print(String.format("After Clear - Cache Name: %s - Cache Size: %d", myCache.getName(), myCache.getSize()));
				}
			}
		}
	}

	private class CacheClearOp implements Runnable {
		private final Cache myCache;

		public CacheClearOp(Cache myCache) {
			this.myCache = myCache;
		}

		@Override
		public void run() {
			myCache.removeAll();
		}
	}

	/*
	 * thread executor shutdown
	 */
	private void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown(); // Disable new tasks from being submitted

		try {
			// Wait until existing tasks to terminate
			while(!pool.awaitTermination(5, TimeUnit.SECONDS));

			pool.shutdownNow(); // Cancel currently executing tasks

			// Wait a while for tasks to respond to being canceled
			if (!pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS))
				log.error("Pool did not terminate");
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();

			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	public static void main(String[] args) throws Exception {
		// pass in cachename to parse
		String cacheNameArg = (args.length > 0)?args[0]:null;

		CacheClearerLauncher launcher = new CacheClearerLauncher(cacheNameArg);
		launcher.clearObjectInCacheSerial();

		System.exit(0);
	}
}