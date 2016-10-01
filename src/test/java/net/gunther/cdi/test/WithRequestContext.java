package net.gunther.cdi.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allows unit tests which are executed by
 * <code>WeldJunit4Runner</code> to activate <code>RequestContext</code>.
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface WithRequestContext {

}
