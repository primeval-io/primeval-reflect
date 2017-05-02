package io.primeval.reflect.proxy.testset.api.generic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.primeval.reflect.proxy.testset.api.GenericInterface;

public final class GenericService implements GenericInterface<Object, String> {

    private final List<Object> objects = new CopyOnWriteArrayList<>();

    public List<Object> getObjects() {
        return objects;
    }

    @Override
    public String makeB() {
        return "foo";
    }

    @Override
    public void consumeA(Object boo) {
        objects.add(boo);
    }

}
