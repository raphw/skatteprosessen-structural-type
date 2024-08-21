package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface StructuralResolver<PROPERTY> {

    Iterable<PROPERTY> getProperties(Class<?> type);

    boolean isRequired(PROPERTY property, Class<?> type);

    boolean isBranch(Class<?> type);

    String getName(PROPERTY property);

    Class<?> getType(PROPERTY property);

    Type getGenericType(PROPERTY property);

    Optional<Class<?>> getSuperClass(Class<?> type);

    List<Class<?>> getSubClasses(Class<?> type);

    default Function<Class<?>, StructuralResolver<?>> asFactory() {
        return type -> {
            if (!isBranch(type)) {
                throw new IllegalArgumentException("Cannot create resolver for non-branch type: " + type.getTypeName());
            }
            return this;
        };
    }
}
