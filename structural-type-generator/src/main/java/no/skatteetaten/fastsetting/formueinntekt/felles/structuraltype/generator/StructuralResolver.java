package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Type;

public interface StructuralResolver<PROPERTY> {

    Iterable<PROPERTY> getProperties(Class<?> type);

    boolean isRequired(PROPERTY property, Class<?> type);

    boolean isBranch(Class<?> type);

    String getName(PROPERTY property);

    Class<?> getType(PROPERTY property);

    Type getGenericType(PROPERTY property);
}
