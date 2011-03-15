package com.ning.arecibo.dashboard.table;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class CachedTableBeanWrapper
{
	private final String cacheKey;
	private final long createdTimeMillis;
	private final CountDownLatch latch = new CountDownLatch(1);
	private DashboardTableBean tableBean;
	
	CachedTableBeanWrapper(String cacheKey)
	{
		this.cacheKey = cacheKey;
		this.createdTimeMillis = System.currentTimeMillis();
	}

	boolean waitFor(long time, TimeUnit unit) throws InterruptedException
	{
		return latch.await(time, unit);
	}

	public String getCacheKey()
	{
		return cacheKey;
	}
	
	public boolean testAge(long maxAge, TimeUnit unit) {
		long maxAgeMillis = unit.toMillis(maxAge);
		if(createdTimeMillis + maxAgeMillis < System.currentTimeMillis())
			return true;
		else
			return false;
	}

	public DashboardTableBean getTableBean()
	{
		return this.tableBean;
	}

	public void setTableBean(DashboardTableBean tableBean)
	{
		this.tableBean = tableBean;
		latch.countDown();
	}
}
