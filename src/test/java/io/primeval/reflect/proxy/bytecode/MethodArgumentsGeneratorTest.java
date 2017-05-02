package io.primeval.reflect.proxy.bytecode;

//
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.primeval.reflect.proxy.arguments.Arguments;
import io.primeval.reflect.proxy.arguments.ArgumentsUpdater;
import io.primeval.reflect.proxy.bytecode.shared.ProxyUtils;
import io.primeval.reflect.proxy.theory.TheoreticalDelegate;

public final class MethodArgumentsGeneratorTest {

    @Test
    public void test() throws Exception {

        Method method = ProxyUtils.getMethodUnchecked(TheoreticalDelegate.class, "test", PrintStream.class, int.class,
                byte.class,
                String.class);
        List<Parameter> parameters = Arrays.asList(method.getParameters());

        String methodArgsClassName = MethodArgumentsGenerator.getName(TheoreticalDelegate.class, method, 0);

        // Practical to debug the generated bytecode with javap -c
        // Files.write(File.createTempFile("test", ".class").toPath(), bytecode);

        ProxyClassLoader dynamicClassLoader = new ProxyClassLoader(
                MethodArgumentsGeneratorTest.class.getClassLoader());
        dynamicClassLoader.declareClassToProxy(TheoreticalDelegate.class, new Class<?>[0], new Method[] { method },
                m -> true);
        Class<?> genClass = dynamicClassLoader.loadClass(methodArgsClassName);

        Arguments args = (Arguments) genClass
                .getConstructor(List.class, PrintStream.class, int.class, byte.class, String.class)
                .newInstance(parameters, System.out, 5, (byte) 42, "foo");

        assertThat(args.parameters()).isSameAs(parameters);

        PrintStream ps = args.objectArg("ps");
        assertThat(ps).isSameAs(System.out);

        String foo = args.objectArg("s");
        assertThat(foo).isEqualTo("foo");

        byte b = args.byteArg("b");
        assertThat(b).isEqualTo((byte) 42);

        int i = args.intArg("i");
        assertThat(i).isEqualTo(5);

        assertThatThrownBy(() -> {
            args.objectArg("unknown");
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No Object parameter named unknown").hasNoCause();

        assertThatThrownBy(() -> {
            args.floatArg("radius");
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No float parameter named radius").hasNoCause();

        ArgumentsUpdater updater = args.updater();
        Arguments arguments = updater.update();

        assertThat(arguments).isEqualTo(args);

        Arguments args2 = updater.setObjectArg("s", "bar").update();
        assertThat(args2.objectArg("s", String.class)).isEqualTo("bar");

        Arguments args3 = updater.setIntArg("i", 30).update();
        assertThat(args3.intArg("i")).isEqualTo(30);

    }

}
