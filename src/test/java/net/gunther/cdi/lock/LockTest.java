package net.gunther.cdi.lock;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.gunther.cdi.test.WeldJUnit4Runner;

@RunWith(WeldJUnit4Runner.class)
public class LockTest {

	@Inject
	LockedBean testee;

	@Before
	public void resetTestBean() {
		testee.reset();
	}

	@Test(expected = IllegalStateException.class)
	public void throwsIllegalStateExceptionOnLockPromotion() {
		// this test also shows, that methods without LockType annotation get
		// the LockType of bean's class
		testee.upgradeLock();
	}

	@Test
	public void incrementsCounterConcurrently() throws InterruptedException {
		Runnable incrementer = () -> {
			for (int i = 0; i < 1000; i++) {
				testee.incrCounter();
				Thread.yield();
			}
		};
		List<Runnable> runnables = asList(
				new Runnable[] { incrementer, incrementer, incrementer, incrementer, incrementer });
		assertConcurrent("Incrementing counter of TestBean concurrently failed.", runnables, 10);
		assertEquals(5000, testee.getCounter());
	}

	@Test
	public void incrementAndGetCounterConcurrently() throws InterruptedException {
		Runnable incrementer = () -> {
			for (int i = 0; i < 1000; i++) {
				testee.incrCounter();
				Thread.yield();
			}
		};
		Runnable getter = () -> {
			while (testee.getCounter() < 3000) {
				Thread.yield();
			}
		};
		List<Runnable> runnables = asList(new Runnable[] { incrementer, incrementer, incrementer, getter });
		assertConcurrent("Incrementing counter of TestBean concurrently failed.", runnables, 10);
		assertEquals(3000, testee.getCounter());
	}

	public static void assertConcurrent(String message, List<? extends Runnable> runnables, int maxTimeoutSeconds)
			throws InterruptedException {
		int numThreads = runnables.size();
		List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
		ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		try {
			CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
			CountDownLatch afterInitBlocker = new CountDownLatch(1);
			CountDownLatch allDone = new CountDownLatch(numThreads);
			runnables.stream()
					.map(r -> wrapRunnable(r, allExecutorThreadsReady, afterInitBlocker, allDone, exceptions))
					.forEach(r -> threadPool.submit(r));
			// wait until all threads are ready
			assertTrue(
					"Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent",
					allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
			// start all test runners
			afterInitBlocker.countDown();
			assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds",
					allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
		} finally {
			threadPool.shutdownNow();
		}
		assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
	}

	private static Runnable wrapRunnable(Runnable submittedTestRunnable, CountDownLatch allExecutorThreadsReady,
			CountDownLatch afterInitBlocker, CountDownLatch allDone, List<Throwable> exceptions) {
		return new Runnable() {
			public void run() {
				allExecutorThreadsReady.countDown();
				try {
					afterInitBlocker.await();
					submittedTestRunnable.run();
				} catch (Throwable e) {
					exceptions.add(e);
				} finally {
					allDone.countDown();
				}
			}
		};
	}
}
