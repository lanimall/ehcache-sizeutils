package org.terracotta.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SizeIteratorLauncher {
	private static Logger log = LoggerFactory.getLogger(SizeIteratorLauncher.class);
	private static final boolean isDebug = log.isDebugEnabled();

	private static final int CACHE_POOLSIZEMAX = 10;
	private static final int CACHEGET_POOLSIZE = 20;

	private final CacheManager cacheManager;
	private final String[] myCacheNames;

	private final ExecutorService cacheFetchService;
	private final ExecutorService cacheGetService;
	
	public SizeIteratorLauncher(final String cacheName) {
		cacheManager = CacheUtils.getCacheManager();
		if(cacheName == null || !cacheManager.cacheExists(cacheName)){
			myCacheNames = cacheManager.getCacheNames();
		} else {
			myCacheNames = new String[]{cacheName};
		}
		cacheFetchService = Executors.newFixedThreadPool((myCacheNames.length>CACHE_POOLSIZEMAX)?CACHE_POOLSIZEMAX:myCacheNames.length, new NamedThreadFactory("Cache Fetch Pool"));
		cacheGetService = Executors.newFixedThreadPool(CACHEGET_POOLSIZE, new NamedThreadFactory("Cache Fetch Pool"));
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		shutdownAndAwaitTermination(cacheFetchService);
	}

	public void findObjectSizesInCache(){
		Future<CacheSizeStats> futs[] = new Future[myCacheNames.length];
		int count = 0;
		for(String cacheName : myCacheNames){
			Cache myCache = cacheManager.getCache(cacheName);
			futs[count++] = cacheFetchService.submit(new CacheFetchOp(myCache));
		}

		CacheSizeStats[] stats = new CacheSizeStats[count];
		for(int i = 0; i < count; i++) {
			try {
				while(!futs[i].isDone()){
					System.out.print(".");
					Thread.sleep(5000);
				}
				stats[i] = futs[i].get();
			} catch (InterruptedException e) {
				log.error("", e);
			} catch (ExecutionException e) {
				log.error("", e);
			}
		}

		System.out.println("");
		for(CacheSizeStats stat : stats){
			System.out.println(stat.toString());
		}
	}

	private class CacheFetchOp implements Callable<CacheSizeStats> {
		private final Cache myCache;

		public CacheFetchOp(Cache myCache) {
			this.myCache = myCache;
		}

		@Override
		public CacheSizeStats call() throws Exception {
			CacheSizeStats cacheStats = new CacheSizeStats(myCache.getName());
			List<Object> keys = myCache.getKeysNoDuplicateCheck();
			long objSize;
			for(Object key : keys) {
				cacheGetService.submit(new CacheGetOp(myCache, key, cacheStats));
			}
			return cacheStats;
		}
	}
	
	private class CacheGetOp implements Runnable {
		private final Cache myCache;
		private final Object key;
		private final CacheSizeStats cacheStats;

		public CacheGetOp(Cache myCache, Object key, CacheSizeStats cacheStats) {
			this.myCache = myCache;
			this.cacheStats = cacheStats;
			this.key = key;
		}

		@Override
		public void run() {
			Element e = myCache.getQuiet(key);
			if(e != null & e.getValue() != null){
				cacheStats.add(e.getSerializedSize());
				if(isDebug)
					log.debug("Cache=" + myCache.getName() + ":\t" + "Key = " + e.getKey() + " size of element = " + e.getSerializedSize());
			}
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
		// pass in the number of object you want to generate, default is 100
		String cacheNameArg = (args.length > 0)?args[0]:null;

		SizeIteratorLauncher launcher = new SizeIteratorLauncher(cacheNameArg);
		launcher.findObjectSizesInCache();

		System.exit(0);
	}
}