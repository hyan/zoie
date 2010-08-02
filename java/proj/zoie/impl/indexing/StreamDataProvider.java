package proj.zoie.impl.indexing;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import proj.zoie.api.DataConsumer;
import proj.zoie.api.ZoieVersion;
import proj.zoie.api.DataProvider;
import proj.zoie.api.ZoieException;
import proj.zoie.api.DataConsumer.DataEvent;
import proj.zoie.mbean.DataProviderAdminMBean;


public abstract class StreamDataProvider<D,V extends ZoieVersion> implements DataProvider<D>,DataProviderAdminMBean{
	private static final Logger log = Logger.getLogger(StreamDataProvider.class);
	
	private int _batchSize;
	private DataConsumer<D,V> _consumer;
	private DataThread<D,V> _thread;
	
	public StreamDataProvider()
	{
		_batchSize=1;
		_consumer=null;
	}
	
	public void setDataConsumer(DataConsumer<D,V> consumer)
	{
	  _consumer=consumer;	
	}
	
	public DataConsumer<D,V> getDataConsumer()
	{
	  return _consumer;
	}

	public abstract DataEvent<D,V> next();
	
	public abstract void reset();
	
	public int getBatchSize() {
		return _batchSize;
	}
	
	public long getEventsPerMinute() {
	  DataThread<D,V> thread = _thread;
	  if (thread==null) return 0;
	  return thread.getEventsPerMinute();
	}
	
	public long getMaxEventsPerMinute() {
      DataThread<D,V> thread = _thread;
      if (thread==null) return 0;
      return thread.getMaxEventsPerMinute();
	}

	public void setMaxEventsPerMinute(long maxEventsPerMinute) {
      DataThread<D,V> thread = _thread;
      if (thread==null) return;
      thread.setMaxEventsPerMinute(maxEventsPerMinute);
	}
	
	public String getStatus() {
      DataThread<D,V> thread = _thread;
      if (thread==null) return "dead";
      return thread.getStatus() + " : " + thread.getState();
	}

	public void pause() {
		if (_thread != null)
		{
			_thread.pauseDataFeed();
		}
	}

	public void resume() {
		if (_thread != null)
		{
			_thread.resumeDataFeed();
		}
	}

	public void setBatchSize(int batchSize) {
		_batchSize=Math.max(1, batchSize);
	}
	
	public long getEventCount()
	{
	  DataThread<D,V> thread = _thread;
	  if (thread != null)
	    return _thread.getEventCount();
	  else
	    return 0;
	}
	
	public void stop()
	{
		if (_thread!=null && _thread.isAlive())
		{
			_thread.terminate();
			try {
				_thread.join();
			} catch (InterruptedException e) {
				log.warn("stopping interrupted");
			}
		}
	}

	public void start() {
		if (_thread==null || !_thread.isAlive())
		{
			reset();
			_thread = new DataThread<D,V>(this);
			_thread.start();
		}
	}
	
	public void syncWthVersion(long timeInMillis, V version) throws ZoieException
	{
	  _thread.syncWthVersion(timeInMillis, version);
	}
	
	private static final class DataThread<D,V extends ZoieVersion> extends Thread
	{
	  private Collection<DataEvent<D,V>> _batch;
		private V _currentVersion;
		private final StreamDataProvider<D,V> _dataProvider;
		private boolean _paused;
		private boolean _stop;
		private AtomicLong _eventCount = new AtomicLong(0);
		private volatile long _eventStart = System.nanoTime();
		private volatile long _throttle = 40000;//Long.MAX_VALUE;
		
		private void resetEventTimer()
		{
		  _eventCount.set(0);
		  _eventStart = System.nanoTime();
		}
		
		private String getStatus()
		{
		  synchronized(this)
		  {
		    if (_stop) return "stopped";
		    if (_paused) return "paused";
		    return "running";
		  }
		}
		
    DataThread(StreamDataProvider<D,V> dataProvider)
		{
			super("Stream DataThread");
			setDaemon(false);
			_dataProvider = dataProvider;
			_currentVersion = null;
			_paused = false;
			_stop = false;
			_batch = new LinkedList<DataEvent<D,V>>();
		}
		@Override
		public void start()
		{
		  super.start();
		  resetEventTimer();
		}
		
		void terminate()
		{
			synchronized(this)
			{
	       _stop = true;
			   this.notifyAll();
			}
		}
		
		void pauseDataFeed()
		{
		    synchronized(this)
		    {
		        _paused = true;
		    }
		}
		
		void resumeDataFeed()
		{
			synchronized(this)
			{
	      _paused = false;
	      resetEventTimer();
				this.notifyAll();
			}
		}
		
		  private void flush()
	    {
	    	// FLUSH
		    Collection<DataEvent<D,V>> tmp;
		    tmp = _batch;
        _batch = new LinkedList<DataEvent<D,V>>();

		    try
	        {
		      if(_dataProvider._consumer!=null)
		      {
		        _eventCount.getAndAdd(tmp.size());
		        //System.out.println("_dataProvider " + _dataProvider + ", _dataProvider._consumer: " + _dataProvider._consumer);
		    	  _dataProvider._consumer.consume(tmp);
		      }
	        }
	        catch (ZoieException e)
	        {
	          log.error(e.getMessage(), e);
	        }
	    }
		
		public V getCurrentVersion()
		{
			synchronized(this)
			{
		      return _currentVersion;
			}
		}
		
		public void syncWthVersion(long timeInMillis, V version) throws ZoieException
		{
		  long now = System.currentTimeMillis();
		  long due = now + timeInMillis;
		  synchronized(this)
		  {
		    //while(_currentVersion < version)
		    while(_currentVersion == null || _currentVersion.compareTo(version)<0)
		    {
		      if(now > due)
          {
            throw new ZoieException("StreamDataProvdier: syncWthVersion: sync timed out");
          }
		      try
		      {
            this.notifyAll();
		        this.wait(due - now);
		      }
		      catch(InterruptedException e)
		      {
	          log.warn(e.getMessage(), e);
		      }
		      now = System.currentTimeMillis();
		    }
		  }
		}

		public void run()
		{
      V version = _currentVersion;
		  while (!_stop)
		  {
		    synchronized(this)
		    {
		      while(!_stop && (_paused || (getEventsPerMinute() > _throttle)))
		      {
		        try {
		          this.wait(500);
		        } catch (InterruptedException e) {
		          continue;
		        }
		      }
		    }
		    if (!_stop)
		    {
		      DataEvent<D,V> data = _dataProvider.next();
		      if (data!=null)
		      {
		        //version = Math.max(version, data.getVersion());
		        //System.out.println("SystemDataProvider: run(): version: before" + version + "  data : " + data);
		        version = (version == null) ? data.getVersion() : (version.compareTo(data.getVersion()) < 0 ? data.getVersion() : version);
		        //System.out.println("SystemDataProvider: run(): version: after" + version + "  data : " + data);
		        synchronized(this)
		        {
		          _batch.add(data);
		          if (_batch.size()>=_dataProvider._batchSize)
		          {
		            flush();
                _currentVersion = version;
		            this.notifyAll();
		          }
		        }
		      }
		      else
		      {
		        synchronized(this)
		        {
		          flush();
              _stop=true;
              _currentVersion = version;
              this.notifyAll();
		          return;
		        }
		      }
		    }
		  }
		}

		private long getEventCount()
		{
		  return _eventCount.get();
		}

		private long getEventsPerMinute(){
		  long diff = (System.nanoTime() - _eventStart)/1000000;
		  if ( diff<=0 ) return 0;
		  return _eventCount.get()*60000/diff;
		}
		
		private long getMaxEventsPerMinute()
		{
		  return _throttle;
		}
		
    private void setMaxEventsPerMinute(long maxEventsPerMinute)
    {
      _throttle = maxEventsPerMinute;
    }
	}
}
