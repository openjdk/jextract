package org.openjdk.jextract;

import java.util.Collection;
import java.util.Optional;

/**
 * Subtypes of this interface can be customized with a variable number of attributes. Each attribute
 * is modelled as an instance of a record class. The record class is used to lookup attributes.
 * There can be at most one attribute associated with a given record class. Moreover, the set of
 * attributes associated with an entity implementing this interface can only monotonically increase over time
 * (that is, removing or replacing attributes is not supported).
 */
public interface Attributed {

    /**
     * {@return the attributes associated with this entity}
     */
    Collection<Record> attributes();

    /**
     * Obtains an attribute from this entity.
     * @param attributeClass the class of the attribute to be obtained.
     * @param <R> the attribute's type.
     * @return the attribute (if any).
     */
    <R extends Record> Optional<R> getAttribute(Class<R> attributeClass);

    /**
     * Adds a new attribute to this entity.
     * @param attribute the attribute to be added.
     * @param <R> the attribute's type.
     */
    <R extends Record> void addAttribute(R attribute);
}
