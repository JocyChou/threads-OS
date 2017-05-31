package nachos.threads;

import nachos.machine.*;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		long curTime = Machine.timer().getTime();
		boolean needYield = false;
		boolean intStatus = Machine.interrupt().disable();
		if(wakeTimeList.size() == 0);
		else
		{
			Iterator<Map.Entry<KThread,Long>> iter = wakeTimeList.entrySet().iterator();
			while(iter.hasNext())
			{
				Map.Entry<KThread, Long> entry = iter.next();
				long time = entry.getValue();
				if(curTime > time)
				{
					needYield = true;
					entry.getKey().ready();
					iter.remove();	
				}
			}
			if( needYield )
			{
				KThread.currentThread().yield();
			}
		}
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		
		boolean intStatus = Machine.interrupt().disable();
		
		long wakeTime = Machine.timer().getTime() + x;
		KThread thread = KThread.currentThread();
		wakeTimeList.put(thread, wakeTime);
		thread.sleep();
		Machine.interrupt().restore(intStatus);
	}
	
	
	private Map<KThread,Long> wakeTimeList = new HashMap<KThread,Long>();
}
