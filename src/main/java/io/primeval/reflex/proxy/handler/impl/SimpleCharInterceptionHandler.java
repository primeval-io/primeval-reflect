package io.primeval.reflex.proxy.handler.impl;

import io.primeval.reflex.arguments.Arguments;
import io.primeval.reflex.proxy.handler.CharInterceptionHandler;

public final class SimpleCharInterceptionHandler implements CharInterceptionHandler {

    @FunctionalInterface
    public interface ToCharFunction<A> {
        char applyAsChar(A a);
    }

    private final Arguments arguments;
    private final ToCharFunction<Arguments> invokeFun;

    public SimpleCharInterceptionHandler(Arguments arguments, ToCharFunction<Arguments> invokeFun) {
        this.arguments = arguments;
        this.invokeFun = invokeFun;
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public <E extends Throwable> char invoke(Arguments arguments) throws E {
        return invokeFun.applyAsChar(arguments);
    }
}
