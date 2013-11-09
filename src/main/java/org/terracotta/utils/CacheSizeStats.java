package org.terracotta.utils;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Fabien Sanglier
 * 
 */
public class CacheSizeStats implements Cloneable {
	private static final NumberFormat nf = NumberFormat.getInstance();
	private static final Logger log = LoggerFactory.getLogger(CacheSizeStats.class);

	private final String cacheName;
	private long objCount;
	private long totalSize;
	private long minSize, maxSize;

	public CacheSizeStats(String cacheName) {
		this.cacheName = cacheName;
		reset();
	}

	public CacheSizeStats(CacheSizeStats stat) {
		if (stat != null) {
			this.cacheName = stat.cacheName;
			init(stat.getObjCount(), stat.getTotalSize(), stat.getMinSize(), stat.getMaxSize());
		} else {
			throw new IllegalArgumentException("Stat may not be null");
		}
	}

	/**
	 * resets the stats to {@link init()}
	 */
	public void reset() {
		init(0, 0, Long.MAX_VALUE, Long.MIN_VALUE);
	}

	private void init(long objCount, long totalSize, long minSize, long maxSize) {
		this.objCount = objCount;
		this.totalSize = totalSize;
		this.minSize = minSize;
		this.maxSize = maxSize;
	}

	/**
	 * Add {@link CacheSizeStats} to the current.
	 *
	 * @param stat
	 * @return {@link this}
	 */
	public CacheSizeStats add(CacheSizeStats stat) {
		if(null != stat){
			if(!this.cacheName.equals(stat.cacheName)){
				throw new IllegalArgumentException("Cannot add a stat object with different cache name");
			}

			this.objCount+= stat.objCount;
			this.totalSize += stat.totalSize;

			if (stat.minSize < this.minSize)
				this.minSize = stat.minSize;

			if (stat.maxSize > this.maxSize)
				this.maxSize = stat.maxSize;
		}
		return this;
	}

	/**
	 * Add transaction length
	 *
	 * @param txLength
	 *            transaction length
	 */
	public void add(long size) {
		objCount += 1;
		totalSize += size;

		if (size < minSize) {
			minSize = size;
		}
		if (size > maxSize) {
			maxSize = size;
		}
	}

	/**
	 * @return cachename
	 */
	public String getCacheName() {
		return cacheName;
	}

	/**
	 * @return average size
	 */
	public double getAvgSize() {
		if (objCount > 0)
			return (double) totalSize / objCount;
		return 0;
	}

	/**
	 *
	 * @return total size
	 */
	public long getTotalSize() {
		return totalSize;
	}

	/**
	 *
	 * @return max object size
	 */
	public long getMaxSize() {
		return maxSize;
	}

	/**
	 *
	 * @return min object size
	 */
	public long getMinSize() {
		return minSize;
	}

	/**
	 * @return total count
	 */
	public long getObjCount() {
		return objCount;
	}

	@Override
	public String toString() {
		return String
				.format("Cache: %s, Cached Objects=>[Total Count: %s, Avg Size: %s, Min Size: %s, Max Size: %s]",
						getCacheName(),
						nf.format(this.getObjCount()),
						nf.format(this.getAvgSize()),
						nf.format(this.getMinSize()),
						nf.format(this.getMaxSize()));
	}
}
