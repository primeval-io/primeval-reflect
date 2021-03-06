package io.primeval.reflex.proxy.theory;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import io.primeval.reflex.proxy.CallContext;
import io.primeval.reflex.proxy.bytecode.Proxy;
import io.primeval.reflex.proxy.shared.SharedProxyUtils;

public final class TheoreticalProxy extends Proxy implements Hello, Goodbye, Stuff {

    private final static Method meth0 = SharedProxyUtils.getMethodUnchecked(TheoreticalDelegate.class, "hello");
    private final static CallContext cc0 = new CallContext(TheoreticalDelegate.class, meth0,
            Arrays.asList(meth0.getParameters()));

    private final static Method meth1 = SharedProxyUtils.getMethodUnchecked(TheoreticalDelegate.class, "test",
            PrintStream.class, int.class,
            byte.class, String.class);
    private final static CallContext cc1 = new CallContext(TheoreticalDelegate.class, meth1,
            Arrays.asList(meth1.getParameters()));

    private final static Method meth2 = SharedProxyUtils.getMethodUnchecked(TheoreticalDelegate.class, "foo", double.class,
            int[].class,
            byte.class, String.class);
    private final static CallContext cc2 = new CallContext(TheoreticalDelegate.class, meth2,
            Arrays.asList(meth2.getParameters()));

    private final TheoreticalDelegate delegate;
    private final M0InterceptionHandler handler0;

    public TheoreticalProxy(TheoreticalDelegate delegate) {
        this.delegate = delegate;
        this.handler0 = new M0InterceptionHandler(delegate);
    }

    @Override
    public String hello() {
        return interceptor.onCall(cc0, handler0);
    }

    @Override
    public void test(PrintStream ps, int i, byte b, String s) {
        interceptor.onCall(cc1, new M1InterceptionHandler(delegate, new M1Args(cc1.parameters, ps, i, b, s)));
    }

    @Override
    public double foo(double a, int[] b) {
        return interceptor.onCall(cc2, new M2InterceptionHandler(delegate, new M2Args(cc2.parameters, a, b)));
    }

    @Override
    // Not intercepted
    public String goodbye() {
        return delegate.goodbye();
    }

    public AssertionError newChecked() {
        throw new AssertionError();
    }

}
