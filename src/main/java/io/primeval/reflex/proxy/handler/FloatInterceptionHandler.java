package io.primeval.reflex.proxy.handler;

import io.primeval.reflex.arguments.Arguments;
import io.primeval.reflex.arguments.ArgumentsProvider;

public interface FloatInterceptionHandler extends ArgumentsProvider {

    // Proceed with overridden arguments and return the normal value
    <E extends Throwable> float invoke(Arguments arguments) throws E;

    // Proceed with default arguments and return the normal value
    default <E extends Throwable> float invoke() throws E {
        return invoke(getArguments());
    }

    default public InterceptionHandler<Float> boxed() {
        return new InterceptionHandler<Float>() {

            @Override
            public <E extends Throwable> Float invoke(Arguments arguments) throws E {
                return FloatInterceptionHandler.this.invoke(arguments);
            }

            @Override
            public <E extends Throwable> Float invoke() throws E {
                return FloatInterceptionHandler.this.invoke();
            }

            @Override
            public Arguments getArguments() {
                return FloatInterceptionHandler.this.getArguments();
            }
        };
    }
}
