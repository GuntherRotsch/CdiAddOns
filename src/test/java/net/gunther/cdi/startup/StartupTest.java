package net.gunther.cdi.startup;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import net.gunther.cdi.test.WeldJUnit4Runner;

@RunWith(WeldJUnit4Runner.class)
public class StartupTest {

	@Inject
	StartupBean testee;

	@Test
	public void toStringCalledOnStartup() {
		assertTrue(testee.isToStringCalled());
	}
}
