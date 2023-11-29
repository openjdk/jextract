package org.openjdk.jextract.impl;

import org.openjdk.jextract.Attributed;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AttributedImpl implements Attributed {

    private final Map<Class<?>, Record> attributes = new HashMap<>();

    public Collection<Record> attributes() {
        return attributes.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Record> Optional<R> getAttribute(Class<R> attributeClass) {
        return Optional.ofNullable((R)attributes.get(attributeClass));
    }

    public <R extends Record> void addAttribute(R attribute) {
        if (attributes.containsKey(attribute.getClass())) {
            throw new IllegalStateException("Attribute already exists: " + attribute.getClass().getSimpleName());
        }
        attributes.put(attribute.getClass(), attribute);
    }

    public <R extends Record> void dropAttribute(Class<R> attributeClass) {
        if (!attributes.containsKey(attributeClass)) {
            throw new IllegalStateException("No attribute: " + attributeClass.getSimpleName());
        }
        attributes.remove(attributeClass);
    }
}
