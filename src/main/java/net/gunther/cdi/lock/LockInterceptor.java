package net.gunther.cdi.lock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Lock
@Dependent
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class LockInterceptor {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

	@AroundInvoke
	public Object concurrencyControl(InvocationContext ctx) throws Exception {
		Lock lockAnnotation = getLockAnnotation(ctx);

		Object returnValue = null;
		switch (lockAnnotation.value()) {
		case WRITE:
			if (lock.getReadHoldCount() > 0) {
				// prohibit lock-promotion because it isn't possible
				throw new IllegalStateException("Upgrade of read-lock of thread not allowed.");
			}
			ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
			writeLock.lock();
			returnValue = proceed(ctx, () -> writeLock.unlock());
			break;
		case READ:
			if (lock.getWriteHoldCount() > 0) {
				// down-grading locks isn't sensible because after returning
				// from intercepted, write-protected call the lock need to
				// upgraded, which isn't possible; therefore we just keep an
				// already existing write-lock
				returnValue = ctx.proceed();
			} else {
				ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
				readLock.lock();
				returnValue = proceed(ctx, () -> readLock.unlock());
			}
			break;
		}
		return returnValue;
	}

	private Object proceed(InvocationContext ctx, Runnable unlocker) throws Exception {
		Object returnValue;
		try {
			returnValue = ctx.proceed();
		} finally {
			unlocker.run();
		}
		return returnValue;
	}

	private Lock getLockAnnotation(InvocationContext ctx) {
		Lock lockAnnotation = ctx.getMethod().getAnnotation(Lock.class);
		if (lockAnnotation == null) {
			lockAnnotation = ctx.getTarget().getClass().getAnnotation(Lock.class);
		}
		return lockAnnotation;
	}
}
