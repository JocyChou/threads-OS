package nachos.threads;


import java.util.Vector;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	
	private int liscounter;
	private int specounter;
	Lock lock = new Lock();
	Condition2 lisstate = new Condition2(lock);
	Condition2 spestate = new Condition2(lock);
	Vector wd = new Vector();
	
	public Communicator() {
		specounter = 0;
		liscounter = 0;
				
		
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		boolean intStatus = Machine.interrupt().disable();
		lock.acquire();
		if(liscounter == 0){
			specounter ++;
			wd.addElement(word);
			spestate.sleep();
			
		}else{
			wd.addElement(word);
			lisstate.wake();
			liscounter --;
		}
		lock.release();
		Machine.interrupt().restore(intStatus);
		return;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() 
	{
		boolean intStatus = Machine.interrupt().disable();
		lock.acquire();
		if(specounter == 0)
		{
			liscounter ++;
			lisstate.sleep();
		}
		else
		{
			spestate.wake();
		}
		
		lock.release();
		if(wd.isEmpty())
		{
			return 0;
		}
		else
		{
			return (Integer)wd.remove(0);
		}
	}
}