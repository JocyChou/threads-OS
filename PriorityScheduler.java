package nachos.threads;
import java.util.Comparator;
import java.util.PriorityQueue;


import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());


		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() 
	{
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() 
	{
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;

	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;

	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue 
	{
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) 
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
			
			
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if(heap.isEmpty())
			{
				return null;
			}
			else
			{
				acquire(heap.poll().thread);
				return queuelockingthread;
			}
			
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			return heap.peek();
		}

		public void print() {
			//Lib.assertTrue(Machine.interrupt().disabled());
			//// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		private KThread queuelockingthread = null;
		
		//Comparator<ThreadState> comparator = new ThreadComparator();
		private java.util.PriorityQueue<ThreadState> heap = new java.util.PriorityQueue<ThreadState>(10, new ThreadComparator<ThreadState>(this)); 
	
		public class ThreadComparator<T extends ThreadState> implements Comparator<T>
		{
			protected ThreadComparator(nachos.threads.PriorityScheduler.PriorityQueue pq) 
			{
				priQueue = pq;
			}
			@Override
			public int compare( T a, T b )
			{
				int a_effectivePriority = a.getEffectivePriority(), b_effectivePriority = b.getEffectivePriority();
				if (a_effectivePriority > b_effectivePriority) 
				{
					return -1;
				} 
				else if (a_effectivePriority < b_effectivePriority) 
				{
					return 1;
				} 
				else 
				{
					long a_waitTime = a.waiting.get(priQueue), b_waitTime = b.waiting.get(priQueue);
					if (a_waitTime < b_waitTime) 
					{
						return -1;
					} 
					else if(a_waitTime > b_waitTime) 
					{
						return 1;
					} 
					else 
					{
						return 0;
					}
				}
			}
			private nachos.threads.PriorityScheduler.PriorityQueue priQueue;
			
		}
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			effectivePriority = priorityDefault;

			setPriority(priorityDefault);
		}
		

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() 
		{
			return pri;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() 
		{
			// implement me
			
			
			return effpri;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority the new priority.
		 */
		public void setPriority(int priority) 
		{
			

			this.pri = priority;
			updateEffectivePriority();
		}

		protected void updateEffectivePriority() 
		{
			for(PriorityQueue pq : waiting.keySet())
			{
				pq.heap.remove(this);
			}
			int temPri = pri;
			
			for(PriorityQueue pq : acquired)
			{
				if(pq.transferPriority)
				{
					ThreadState topTS = pq.heap.peek();
					if (topTS != null) 
					{
						int topPQ_AP = topTS.getEffectivePriority();
						
						if (topPQ_AP > temPri)
							temPri = topPQ_AP;
					}
				}
			}
			boolean needToTransfer = temPri != effectivePriority;
			
			effectivePriority = temPri;
			
			for (PriorityQueue pq : waiting.keySet())
			{
				pq.heap.add(this);
			}

			if (needToTransfer)
			{
				for (PriorityQueue pq : waiting.keySet()) 
				{
					if (pq.transferPriority && pq.queuelockingthread != null)
						getThreadState(pq.queuelockingthread).updateEffectivePriority();
				}
			}

			// implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) 
		{
			if (!waiting.containsKey(waitQueue)) 
			{
				//Unlock this wait queue, if THIS holds it
				release(waitQueue);
				
				//Put it on the queue
				waiting.put(waitQueue, Machine.timer().getTime());
				
				//The effective priority of this shouldn't change, so just shove it onto the waitQueue's members
				waitQueue.heap.add(this);
				
				if (waitQueue.queuelockingthread != null) 
				{
					getThreadState(waitQueue.queuelockingthread).updateEffectivePriority();
				}
			}
			// implement me
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) 
		{
			// implement me
			if (waitQueue.queuelockingthread != null) 
			{
				getThreadState(waitQueue.queuelockingthread).release(waitQueue);
			}
			
			/*
			 * Remove the passed thread state from the queues, if it exists on them
			 */
			waitQueue.heap.remove(this);
			
			//Acquire the thread
			waitQueue.queuelockingthread = this.thread;
			acquired.add(waitQueue);
			waiting.remove(waitQueue);
			
			updateEffectivePriority();
		}
		
		private void release(PriorityQueue priorityQueue) {
			// remove priorityQueue from my acquired set
			if (acquired.remove(priorityQueue)) {
				priorityQueue.queuelockingthread = null;
				updateEffectivePriority();
			}
		}
		
		protected int pri;
		protected int effpri;

		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;
		
		/** A set of all the PriorityQueues this ThreadState has acquired */
		private HashSet<nachos.threads.PriorityScheduler.PriorityQueue> acquired = new HashSet<nachos.threads.PriorityScheduler.PriorityQueue>();
		
		/** A map of all the PriorityQueues this ThreadState is waiting on mapped to the time they were waiting on them*/
		public HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long> waiting = new HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long>();
	}
}
