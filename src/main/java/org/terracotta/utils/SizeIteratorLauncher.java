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
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SizeIteratorLauncher {
	private static Logger log = LoggerFactory.getLogger(SizeIteratorLauncher.class);
	private static final boolean isDebug = log.isDebugEnabled();

	private static final int CACHE_POOLSIZEMAX = 10;
	private static final int CACHEGET_POOLSIZE = 20;

	private final CacheManager cacheManager;
	private final String[] myCacheNames;
	private final boolean serializedSizeCalculation;
	
	private final ExecutorService cacheFetchService;
	private final ExecutorService cacheGetService;
	
	private final SizeOf sizeOf = new net.sf.ehcache.pool.sizeof.AgentSizeOf(new PassThroughFilter(), true);
	//private final SizeOf sizeOf = new net.sf.ehcache.pool.sizeof.ReflectionSizeOf(new PassThroughFilter(), true);
	
	public SizeIteratorLauncher(final String cacheName) {
		this(cacheName, false);
	}
	
	public SizeIteratorLauncher(final String cacheName, final boolean serializedSizeCalculation) {
		this.cacheManager = CacheUtils.getCacheManager();
		this.serializedSizeCalculation = serializedSizeCalculation;
		if(cacheName == null || !cacheManager.cacheExists(cacheName)){
			this.myCacheNames = cacheManager.getCacheNames();
		} else {
			this.myCacheNames = new String[]{cacheName};
		}
		this.cacheFetchService = Executors.newFixedThreadPool((myCacheNames.length>CACHE_POOLSIZEMAX)?CACHE_POOLSIZEMAX:myCacheNames.length, new NamedThreadFactory("Cache Fetch Pool"));
		this.cacheGetService = Executors.newFixedThreadPool(CACHEGET_POOLSIZE, new NamedThreadFactory("Cache Fetch Pool"));
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

	/*
	 * mostly for testing...
	 */
	public void findSerialObjectSizesInCache(){
		CacheSizeStats[] stats = new CacheSizeStats[myCacheNames.length];
		for(int i=0; i<myCacheNames.length; i++){
			Cache myCache = cacheManager.getCache(myCacheNames[i]);
			stats[i] = new CacheSizeStats(myCache.getName());
			List<Object> keys = myCache.getKeysNoDuplicateCheck();
			for(Object key : keys) {
				Element e = myCache.getQuiet(key);
				if(e != null){
					long size = (serializedSizeCalculation)?e.getSerializedSize():sizeOf.deepSizeOf(Integer.MAX_VALUE, true, e.getObjectValue()).getCalculated();
					stats[i].add(size);
					if(isDebug)
						log.debug("Cache=" + myCache.getName() + ":\t" + "Key = " + e.getObjectKey() + " size of element = " + size);
				}
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
				long size = (serializedSizeCalculation)?e.getSerializedSize():sizeOf.deepSizeOf(Integer.MAX_VALUE, true, e.getObjectValue()).getCalculated();
				cacheStats.add(size);
				if(isDebug)
					log.debug("Cache=" + myCache.getName() + ":\t" + "Key = " + e.getKey() + " size of element = " + size);
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
		// pass in cachename to parse
		String cacheNameArg = (args.length > 0)?args[0]:null;
		boolean serializedSizeCalculation = (args.length > 1)?Boolean.parseBoolean(args[1]):false;
		
		SizeIteratorLauncher launcher = new SizeIteratorLauncher(cacheNameArg, serializedSizeCalculation);
		launcher.findObjectSizesInCache();
		
		System.exit(0);
	}
}