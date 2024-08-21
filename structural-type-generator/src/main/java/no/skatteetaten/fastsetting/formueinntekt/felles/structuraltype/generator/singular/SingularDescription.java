package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.NodeResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.StructuralResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;

public interface SingularDescription {

    Class<?> getType();

    boolean isLeaf();

    Map<String, Property> getProperties();

    Optional<SingularDescription> getSuperDescription();

    List<SingularDescription> getSubDescriptions();

    default boolean hasProperty(String name) {
        return getProperties().containsKey(name);
    }

    default List<Property> toProperties(String name) {
        Queue<SingularDescription> queue = new ArrayDeque<>(Set.of(this));
        Set<SingularDescription> checked = new HashSet<>(queue);
        List<Property> properties = new ArrayList<>();
        do {
            SingularDescription description = queue.remove();
            Property property = description.getProperties().get(name);
            if (property != null) {
                properties.add(property);
            }
            description.getSuperDescription().filter(checked::add).ifPresent(queue::add);
            description.getSubDescriptions().stream().filter(checked::add).forEach(queue::add);
        } while (!queue.isEmpty());
        return properties;
    }

    static List<SingularDescription> of(
        Function<Class<?>, StructuralResolver<?>> structuralResolver,
        NodeResolver nodeResolver,
        BiPredicate<Class<?>, String> condition,
        List<Class<?>> types
    ) {
        Set<Class<?>> resolved = new HashSet<>();
        Map<Class<?>, SingularDescription> references = new HashMap<>();
        return types.stream().distinct()
            .map(type -> of(structuralResolver.apply(type), nodeResolver, condition, type, resolved, references))
            .collect(Collectors.toList());
    }

    private static <PROPERTY> SingularDescription of(
        StructuralResolver<PROPERTY> structuralResolver,
        NodeResolver nodeResolver,
        BiPredicate<Class<?>, String> condition,
        Class<?> type,
        Set<Class<?>> resolved,
        Map<Class<?>, SingularDescription> references
    ) {
        if (!resolved.add(type)) {
            return new SingularRecursiveDescription(() -> references.get(type));
        }
        SingularDescription description;
        if (type.isEnum() || type.isPrimitive() || !structuralResolver.isBranch(type)) {
            description = new SingularLeafDescription(type);
        } else {
            Map<String, Property> properties = new LinkedHashMap<>();
            for (PROPERTY property : structuralResolver.getProperties(type)) {
                String name = structuralResolver.getName(property);
                if (!condition.test(type, name)) {
                    continue;
                }
                Class<?> target = structuralResolver.getType(property);
                Cardinality cardinality;
                if (List.class.isAssignableFrom(target)) {
                    Type generic = structuralResolver.getGenericType(property);
                    if (!(generic instanceof ParameterizedType)
                        || ((ParameterizedType) generic).getActualTypeArguments().length != 1
                        || !(((ParameterizedType) generic).getActualTypeArguments()[0] instanceof Class<?>)) {
                        throw new IllegalArgumentException("Unexpected generic type for " + property);
                    }
                    target = (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
                    cardinality = Cardinality.LIST;
                } else if (Collection.class.isAssignableFrom(target) || Map.class.isAssignableFrom(target)) {
                    throw new IllegalArgumentException("Only list collection types are supported: " + property);
                } else {
                    cardinality = target.isPrimitive() || structuralResolver.isRequired(property, target)
                        ? Cardinality.SINGLE
                        : Cardinality.OPTIONAL;
                }
                properties.put(nodeResolver.resolve(type, name).orElse(name), new Property(
                    of(structuralResolver, nodeResolver, condition, target, resolved, references),
                    cardinality,
                    name
                ));
            }
            description = new SingularBranchDescription(
                type,
                properties,
                structuralResolver.getSuperClass(type)
                    .map(superType -> of(structuralResolver, nodeResolver, condition, superType, resolved, references))
                    .orElse(null),
                structuralResolver.getSubClasses(type).stream()
                    .map(subType -> of(structuralResolver, nodeResolver, condition, subType, resolved, references))
                    .collect(Collectors.toList())
            );
        }
        references.put(type, description);
        return description;
    }

    class Property {

        private final SingularDescription description;

        private final Cardinality cardinality;

        private final String name;

        Property(SingularDescription description, Cardinality cardinality, String name) {
            this.description = description;
            this.cardinality = cardinality;
            this.name = name;
        }

        public SingularDescription getDescription() {
            return description;
        }

        public Cardinality getCardinality() {
            return cardinality;
        }

        public String getName() {
            return name;
        }
    }
}
