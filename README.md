# CdiAddOns
Add-ons for Development of (Micro)Services based on CDI


* Java EE without EJB - CDI only

Currently, we see a trend to move from EJB to more light-weight technologies, in particular in the field of microservices.
Even for those, how want to be still with standard Java EE technology, the new Micro Profile initiative could be an option.
This profile contains only CDI, JAX/RS, and JSON-P.

But refusing EJBs does also mean, that you give up some benefits, because not all capabilities of standard EJBs like
- Security
- Transaction
- Concurrency Management
- Lifecycle Management

do have a counterpart in CDI world, even if some people claim that "CDI beans are the new EJB".
Lifecycle management for example is implemented differently: While EJBs (I speek of Stateless Session beans here) are usually
pooled by the container, CDI controls the lifecycle of its beans according to bean's contexts, but never pools beans.


## Concurrency Control

Another feature is concurrency control, allowing a programming model application developers can almost ignore the fact that multiple
threads of a server application are active at the same time. If all CDI beans are request scoped, multi-threading usually isn't an
issue, too. This is because the Java EE request processing model strongly tieds an request to a thread processing that request.
So there won't be any concurrent threads using request scoped CDI beans.

The situation is different for application scoped CDI beans. Those can best compared to singleton EJBs (annotation javax.ejb.Singleton),
i.e. singleton instances of a particular type within a Java Virtual Machine. But in contrast to EJB technology, which by default
ensures, that only one thread enters any method of a singleton EJB at a time, application scoped beans don't implement any
concurrency control mechanism. In the following I'll demonstrate how this feature can be refitted.


### Interceptors

When taking a look at EJB technology, it becomes clear that injected EJBs are never plain Java objects, but always proxies that
eventually delegate calls to application code. Before a call reaches any business code, control flow runs through several interceptors.
A trace of these interceptors can be inspected when looking at the lenghty stacktraces of exceptions thrown from inside an EJB.
Every EJB capability seems to be implemented by its own interceptor.

CDI also supports interceptors, which can be used to refit CDI with Java EE features. First of aöö, implementing interceptors in
CDI requires an annotation, with which CDI beans can be marked:

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Inherited;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    import javax.interceptor.InterceptorBinding;

    @Inherited
    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface Lock {
    }

In the next step the actual interceptor class can be defined:

    import static javax.interceptor.Interceptor.Priority.LIBRARY_BEFORE;
    
    import javax.annotation.Priority;
    import javax.interceptor.AroundInvoke;
    import javax.interceptor.Interceptor;
    import javax.interceptor.InvocationContext;
    
    @Lock
    @Interceptor
    @Priority(LIBRARY_BEFORE)
    public class TraceInterceptor {

	    private static final boolean fair = true;
	    private ReentrantLock lock = new ReentrantLock(fair);
	
       @AroundInvoke
       public Object invokeLockWithLock(InvocationContext ctx) throws Exception {
		    lock.lock();
		    try {
		    	return ctx.proceed();
		    } finally {
			    lock.unlock();
		    }
        }
    }


Implementation Note: Call lock.lock() is left out of try-finally block to prevent unlock to take place when calling lock gives an exception and
lock therefore not hold, which would result in another excpetion when unlocking.

--- Tests needed for this simple concurrency control mechanism ---

Interceptors are <code>@DependentScoped</code> by default, i.e. inherit the context from intercepted CDI bean. Hence, each intercepted CDI bean
does have its own interceptor and reentrant lock.


HOW TO SELF-INVOKE EJB 3.X WITH(OUT) "THIS" - http://www.adam-bien.com/roller/abien/entry/how_to_self_invoke_ejb


http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantReadWriteLock.html:
Lock down-grading
Reentrancy also allows down-grading from the write lock to a read lock, by acquiring the write lock, then the read lock and then releasing the write lock. However, upgrading from a read lock to the write lock is not possible.


Although, lock down-grading for ReentrantReadWriteLocks is possible, but we will prohibit too. The reason is the following: When a thread holds a write lock
and does a recursive call to a method guarded by a read lock, down-grading the lock would be possible. But when the thread returns from read protected method,
execution of write protected method will continue. Before the read lock has to be upgraded, which is not possible. Therefore, when a thread holding a write
lock recursively calls a method guarded by read lock, the write lock should be kept.


