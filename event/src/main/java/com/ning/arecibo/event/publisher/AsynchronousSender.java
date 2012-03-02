/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.event.publisher;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mogwee.executors.NamedThreadFactory;
import org.skife.config.TimeSpan;

public class AsynchronousSender
{
	private ThreadPoolExecutor asyncSender;

	public AsynchronousSender(int numThreads, int bufferSize, final TimeSpan drainDelay)
	{
		this.asyncSender = new ThreadPoolExecutor(0, numThreads,
				60L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(bufferSize){
					public boolean offer(Runnable runnable)
					{
						boolean ok = super.offer(runnable);
						if ( !ok ) {
							try {
								boolean retryOk = super.offer(runnable,
								                              drainDelay.getPeriod(),
								                              drainDelay.getUnit());
								if ( ! retryOk ) {
									eventDiscarded(runnable);
								}
							}
							catch (InterruptedException e) {
								Thread.interrupted();
							}
						}
						return true;
					}
				},
				new NamedThreadFactory(getClass().getSimpleName()+":async"),
				new ThreadPoolExecutor.DiscardPolicy());
	}

	protected void eventDiscarded(Runnable runnable)
	{
	}

	public void execute(Runnable run)
	{
		this.asyncSender.execute(run);
	}

	public int getActiveCount()
	{
		return this.asyncSender.getActiveCount();
	}

	public int getQueueSize()
	{
		return this.asyncSender.getQueue().size();
	}

	public void shutdown()
	{
		this.asyncSender.shutdown();
	}

	public boolean isShutdown()
	{
		return asyncSender.isShutdown();
	}

	public boolean isTerminated()
	{
		return asyncSender.isTerminated();
	}

	public boolean isTerminating()
	{
		return asyncSender.isTerminating();
	}

	public List<Runnable> shutdownNow()
	{
		return asyncSender.shutdownNow();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException
	{
		return asyncSender.awaitTermination(timeout, unit);
	}
}
