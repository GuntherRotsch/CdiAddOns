package net.gunther.cdi.startup;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Startup
public class StartupBean {

	private boolean toStringCalled = false;

	public boolean isToStringCalled() {
		return toStringCalled;
	}

	@Override
	public String toString() {
		toStringCalled = true;
		return "StartupBean";
	}
}
