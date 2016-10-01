package net.gunther.cdi.test;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.environment.se.WeldSEProvider;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class WeldJUnit4Runner extends BlockJUnit4ClassRunner {

	private static Weld weld;
	private static WeldContainer container;

	static {
		weld = new Weld();
		container = weld.initialize();
	}

	private final Class<?> testClazz;
	private boolean withRequestContext;

	public WeldJUnit4Runner(final Class<?> clazz) throws InitializationError {
		super(clazz);
		this.testClazz = clazz;

		withRequestContext = (clazz.getDeclaredAnnotation(WithRequestContext.class) != null);
	}

	@Override
	protected Object createTest() {
		return container.instance().select(testClazz).get();
	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		if (withRequestContext) {
			runChildWithRequestContext(method, notifier);
		} else {
			super.runChild(method, notifier);
		}
	}

	/**
	 * Each call of a test method is executed with its own request context, i.e.
	 * call of test method is seen as a single request.
	 */
	private void runChildWithRequestContext(FrameworkMethod method, RunNotifier notifier) {
		Map<String, Object> requestDataStore = new HashMap<>();
		BoundRequestContext requestContext = container.instance().select(BoundRequestContext.class).get();

		requestContext.associate(requestDataStore);
		requestContext.activate();

		try {
			super.runChild(method, notifier);
		} finally {
			try {
				requestContext.invalidate();
				requestContext.deactivate();
			} finally {
				requestContext.dissociate(requestDataStore);
			}
		}
	}
}
