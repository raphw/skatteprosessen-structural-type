package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

public class JaxbStructuralResolver implements StructuralResolver<Field> {

    private static final String DEFAULT_VALUE = "##default";

    private final Class<? extends Annotation> xmlTransient, xmlElement, xmlType, xmlSeeAlso;

    private final MethodHandle name, required, value;

    private final boolean subtyping;

    @SuppressWarnings("unchecked")
    private JaxbStructuralResolver(ClassLoader classLoader, String namespace) {
        this.subtyping = true;
        try {
            xmlTransient = (Class<? extends Annotation>) Class.forName(namespace + ".XmlTransient", true, classLoader);
            xmlElement = (Class<? extends Annotation>) Class.forName(namespace + ".XmlElement", true, classLoader);
            name = MethodHandles.publicLookup().findVirtual(xmlElement, "name", MethodType.methodType(String.class));
            required = MethodHandles.publicLookup().findVirtual(xmlElement, "required", MethodType.methodType(boolean.class));
            xmlType = (Class<? extends Annotation>) Class.forName(namespace + ".XmlType", true, classLoader);
            xmlSeeAlso = (Class<? extends Annotation>) Class.forName(namespace + ".XmlSeeAlso", true, classLoader);
            value = MethodHandles.publicLookup().findVirtual(xmlSeeAlso, "value", MethodType.methodType(Class[].class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
        }
    }

    private JaxbStructuralResolver(
        Class<? extends Annotation> xmlTransient,
        Class<? extends Annotation> xmlElement,
        Class<? extends Annotation> xmlType,
        Class<? extends Annotation> xmlSeeAlso,
        MethodHandle name,
        MethodHandle required,
        MethodHandle value,
        boolean subtyping
    ) {
        this.xmlTransient = xmlTransient;
        this.xmlElement = xmlElement;
        this.xmlType = xmlType;
        this.xmlSeeAlso = xmlSeeAlso;
        this.name = name;
        this.required = required;
        this.value = value;
        this.subtyping = subtyping;
    }

    public static JaxbStructuralResolver ofJavax() {
        return ofJavax(JaxbStructuralResolver.class.getClassLoader());
    }

    public static JaxbStructuralResolver ofJavax(ClassLoader classLoader) {
        return new JaxbStructuralResolver(classLoader, "javax.xml.bind.annotation");
    }

    public static JaxbStructuralResolver ofJakarta() {
        return ofJakarta(JaxbStructuralResolver.class.getClassLoader());
    }

    public static JaxbStructuralResolver ofJakarta(ClassLoader classLoader) {
        return new JaxbStructuralResolver(classLoader, "jakarta.xml.bind.annotation");
    }

    public StructuralResolver<?> withSubtyping(boolean subtyping) {
        return new JaxbStructuralResolver(xmlTransient, xmlElement, xmlType, xmlSeeAlso, name, required, value, subtyping);
    }

    @Override
    public Iterable<Field> getProperties(Class<?> type) {
        return () -> {
            Set<String> names = new HashSet<>();
            return Stream.<Class<?>>iterate(
                    type,
                    current -> current != Object.class
                        && current != null
                        && (!subtyping || current == type || !current.isAnnotationPresent(xmlType)),
                    Class::getSuperclass
                )
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
                : ((String) name.invoke(annotation)).replace("[^A-Za-z0-9]", "_");
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public Class<?> getType(Field property) {
        return property.getType();
    }

    @Override
    public Type getGenericType(Field property) {
        return property.getGenericType();
    }

    @Override
    public Optional<Class<?>> getSuperClass(Class<?> type) {
        if (subtyping) {
            Class<?> superType = type.getSuperclass();
            return superType != null && superType.isAnnotationPresent(xmlType)
                    ? Optional.of(superType)
                    : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<Class<?>> getSubClasses(Class<?> type) {
        if (subtyping) {
            Annotation xmlSeeAlso = type.getAnnotation(this.xmlSeeAlso);
            try {
                return xmlSeeAlso == null
                        ? Collections.emptyList()
                        : Arrays.asList((Class<?>[]) value.invoke(xmlSeeAlso));
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        } else {
            return Collections.emptyList();
        }
    }
}
