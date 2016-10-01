package net.gunther.cdi.lock;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Lock(LockType.READ)
public class LockedBean {

	@Inject
	LockedBean self;

	private String val;
	private int counter = 0;

	/**
	 * Reset instance variables for tests.
	 */
	@Lock(LockType.WRITE)
	public void reset() {
		this.val = null;
		this.counter = 0;
	}

	@Lock(LockType.WRITE)
	public void setVal(String val) {
		this.val = val;
	}

	public void upgradeLock() {
		self.setVal("TEST");
	}

	@Lock(LockType.WRITE)
	public void incrCounter() {
		this.counter++;
	}

	public int getCounter() {
		return this.counter;
	}
}
