package io.primeval.reflect.proxy.bytecode;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import io.primeval.reflect.arguments.Arguments;
import io.primeval.reflect.proxy.composite.ArgumentsMock;
import io.primeval.reflect.proxy.handler.DoubleInterceptionHandler;
import io.primeval.reflect.proxy.handler.IntInterceptionHandler;
import io.primeval.reflect.proxy.handler.ObjectInterceptionHandler;
import io.primeval.reflect.proxy.shared.ProxyUtils;
import io.primeval.reflect.proxy.testset.simpleservice.SimpleService;
import io.primeval.reflect.proxy.theory.TheoreticalDelegate;

public class InterceptionHandlerGeneratorTest {

    @Test
    public void createObjectInterceptionHandlerForParameterlessMethod() throws Exception {
        Method method = ProxyUtils.getMethodUnchecked(TheoreticalDelegate.class, "hello");

        String methodInterceptionHandlerClassName = InterceptionHandlerGenerator.getName(
                TheoreticalDelegate.class,
                method, 0);

        // Practical to debug the generated bytecode with javap -c
        // Files.write(File.createTempFile("test", ".class").toPath(), bytecode);

        ProxyClassLoader dynamicClassLoader = new ProxyClassLoader(
                MethodArgumentsGeneratorTest.class.getClassLoader());
        dynamicClassLoader.declareClassToProxy(TheoreticalDelegate.class, new Class<?>[0], new Method[] { method },
                m -> true);
        Class<?> genClass = dynamicClassLoader.loadClass(methodInterceptionHandlerClassName);

        TheoreticalDelegate delegate = new TheoreticalDelegate();

        @SuppressWarnings("unchecked")
        ObjectInterceptionHandler<String> helloMethodHandler = (ObjectInterceptionHandler<String>) genClass
                .getConstructor(TheoreticalDelegate.class)
                .newInstance(delegate);

        Assertions.assertThat(helloMethodHandler.getArguments()).isEqualTo(Arguments.EMPTY_ARGUMENTS);
        Assertions.assertThat(helloMethodHandler.invoke()).isEqualTo("hello");
    }

    @Test
    public void createIntInterceptionHandlerForParameterlessMethod() throws Exception {
        Method method = ProxyUtils.getMethodUnchecked(SimpleService.class, "times");

        String methodInterceptionHandlerClassName = InterceptionHandlerGenerator.getName(
                SimpleService.class, method, 0);

        // Practical to debug the generated bytecode with javap -c
        // Files.write(File.createTempFile("test", ".class").toPath(), bytecode);

        ProxyClassLoader dynamicClassLoader = new ProxyClassLoader(
                MethodArgumentsGeneratorTest.class.getClassLoader());
        dynamicClassLoader.declareClassToProxy(SimpleService.class, new Class<?>[0], new Method[] { method },
                m -> true);
        Class<?> genClass = dynamicClassLoader.loadClass(methodInterceptionHandlerClassName);

        SimpleService delegate = new SimpleService();

        IntInterceptionHandler timesMethodHandler = (IntInterceptionHandler) genClass
                .getConstructor(SimpleService.class)
                .newInstance(delegate);

        Assertions.assertThat(timesMethodHandler.getArguments()).isEqualTo(Arguments.EMPTY_ARGUMENTS);
        Assertions.assertThat(timesMethodHandler.invoke()).isEqualTo(4);

    }

    @Test
    public void createDoubleInterceptionHandler() throws Exception {
        Method method = ProxyUtils.getMethodUnchecked(TheoreticalDelegate.class, "foo", double.class, int[].class);
        List<Parameter> parameters = Arrays.asList(method.getParameters());

        String methodInterceptionHandlerClassName = InterceptionHandlerGenerator.getName(
                TheoreticalDelegate.class, method, 0);

        // Practical to debug the generated bytecode with javap -c
        // Files.write(File.createTempFile("test", ".class").toPath(), bytecode);

        String methodArgsClassName = MethodArgumentsGenerator.getName(TheoreticalDelegate.class, method, 0);

        ProxyClassLoader dynamicClassLoader = new ProxyClassLoader(
                MethodArgumentsGeneratorTest.class.getClassLoader());
        dynamicClassLoader.declareClassToProxy(TheoreticalDelegate.class, new Class<?>[0], new Method[] { method },
                m -> true);
        Class<?> methodArgsGenClass = dynamicClassLoader.loadClass(methodArgsClassName);

        Arguments args = (Arguments) methodArgsGenClass
                .getConstructor(List.class, double.class, int[].class)
                .newInstance(parameters, 42.0d, new int[] { 5, 6 });

        TheoreticalDelegate delegate = new TheoreticalDelegate();

        DoubleInterceptionHandler fooMethodHandler = (DoubleInterceptionHandler) dynamicClassLoader
                .loadClass(methodInterceptionHandlerClassName)
                .getConstructors()[0]
                .newInstance(delegate, args);

        Assertions.assertThat(fooMethodHandler.getArguments()).isEqualTo(args);
        Assertions.assertThat(fooMethodHandler.invoke()).isEqualTo(47.0d);

        Arguments argumentsMock = new ArgumentsMock(parameters).setDoubleArg("a", 21)
                .setObjectArg("b", new int[] { -32 }).update();

        Assertions.assertThat(fooMethodHandler.invoke(argumentsMock)).isEqualTo(-11);

    }

}