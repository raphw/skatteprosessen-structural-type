package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class JaxbStructuralResolver implements StructuralResolver<Field> {

    private static final String DEFAULT_VALUE = "##default";

    private final Class<? extends Annotation> xmlTransient, xmlElement, xmlType;

    private final MethodHandle name, required;

    @SuppressWarnings("unchecked")
    private JaxbStructuralResolver(ClassLoader classLoader, String namespace) {
        try {
            xmlTransient = (Class<? extends Annotation>) Class.forName(namespace + ".XmlTransient", true, classLoader);
            xmlElement = (Class<? extends Annotation>) Class.forName(namespace + ".XmlElement", true, classLoader);
            name = MethodHandles.publicLookup().findVirtual(xmlElement, "name", MethodType.methodType(String.class));
            required = MethodHandles.publicLookup().findVirtual(xmlElement, "required", MethodType.methodType(boolean.class));
            xmlType = (Class<? extends Annotation>) Class.forName(namespace + ".XmlType", true, classLoader);
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
        }
    }

    public static StructuralResolver<?> ofJavax() {
        return ofJavax(JaxbStructuralResolver.class.getClassLoader());
    }

    public static StructuralResolver<?> ofJavax(ClassLoader classLoader) {
        return new JaxbStructuralResolver(classLoader, "javax.xml.bind.annotation");
    }

    public static StructuralResolver<?> ofJakarta() {
        return ofJakarta(JaxbStructuralResolver.class.getClassLoader());
    }

    public static StructuralResolver<?> ofJakarta(ClassLoader classLoader) {
        return new JaxbStructuralResolver(classLoader, "jakarta.xml.bind.annotation");
    }

    @Override
    public Iterable<Field> getProperties(Class<?> type) {
        return () -> {
            Set<String> names = new HashSet<>();
            return Stream.<Class<?>>iterate(type, current -> current != Object.class && current != null, Class::getSuperclass)
                .flatMap(current -> Stream.of(current.getDeclaredFields()))
                .filter(field -> !field.isSynthetic())
                .filter(field -> !field.isAnnotationPresent(xmlTransient))
                .filter(field -> names.add(field.getName()))
                .iterator();
        };
    }

    @Override
    public boolean isRequired(Field property, Class<?> type) {
        Annotation element = property.getAnnotation(xmlElement);
        try {
            return element != null && (boolean) required.invoke(element);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public boolean isBranch(Class<?> type) {
        return type.isAnnotationPresent(xmlType);
    }

    @Override
    public String getName(Field property) {
        try {
            Annotation annotation = property.getAnnotation(xmlElement);
            return annotation == null || name.invoke(annotation).equals(DEFAULT_VALUE)
                ? property.getName()
                : (String) name.invoke(annotation);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public Class<?> getType(Field field) {
        return field.getType();
    }

    @Override
    public Type getGenericType(Field field) {
        return field.getGenericType();
    }
}
