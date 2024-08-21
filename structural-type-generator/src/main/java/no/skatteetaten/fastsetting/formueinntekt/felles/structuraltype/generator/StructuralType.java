package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.CompoundOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.DelegatedBy;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.EnumerationOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ExpansionOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectionOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplatedBy;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter.EnumerationEmitter;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter.NameResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter.ProjectionEmitter;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter.PropertyResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter.StructureEmitter;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter.TemplateEmitter;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer.DefaultNormalizer;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer.EnumeratingNormalizer;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer.IntersectingNormalizer;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer.KeyNormalizer;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

public class StructuralType {

    private final NamingStrategy namingStrategy;

    private final PropertyStrategy propertyStrategy;

    private final Function<Class<?>, StructuralResolver<?>> structuralResolver;

    private final TypeResolver typeResolver;

    private final AccessResolver accessResolver;

    private final InterfaceResolver interfaceResolver;

    private final NodeResolver nodeResolver;

    private final BiPredicate<Class<?>, String> condition;

    private final boolean normalizeIntersections, normalizeEnumerations;

    private final List<Function<Class<?>, ?>> keyResolvers;

    private final Map<ClassName, Predefinition> predefinitions;

    private final Function<List<List<SingularDescription>>, ? extends Collection<List<SingularDescription>>> grouper;

    private final Set<PropertyGeneration> propertyGenerations;

    private final Set<FeatureGeneration> featureGenerations;

    private final Set<ImplementationGeneration> implementationGenerations;

    private final boolean exceptionOnEmptySetter;

    public StructuralType() {
        namingStrategy = new CommonPrefixNamingStrategy();
        propertyStrategy = new BeanPropertyStrategy();
        structuralResolver = new SimpleStructuralResolver().asFactory();
        typeResolver = new SimpleTypeResolver();
        accessResolver = new BeanAccessResolver();
        interfaceResolver = (name, types) -> Collections.emptyList();
        nodeResolver = (type, property) -> Optional.empty();
        condition = (type, property) -> true;
        normalizeIntersections = true;
        normalizeEnumerations = false;
        keyResolvers = Collections.emptyList();
        predefinitions = Collections.emptyMap();
        grouper = new IndexAlignedGrouper();
        propertyGenerations = EnumSet.allOf(PropertyGeneration.class);
        featureGenerations = EnumSet.allOf(FeatureGeneration.class);
        implementationGenerations = EnumSet.allOf(ImplementationGeneration.class);
        exceptionOnEmptySetter = false;
    }

    private StructuralType(
        NamingStrategy namingStrategy,
        PropertyStrategy propertyStrategy,
        Function<Class<?>, StructuralResolver<?>> structuralResolver,
        TypeResolver typeResolver,
        AccessResolver accessResolver,
        InterfaceResolver interfaceResolver,
        NodeResolver nodeResolver,
        BiPredicate<Class<?>, String> condition,
        boolean normalizeIntersections,
        boolean normalizeEnumerations,
        List<Function<Class<?>, ?>> keyResolvers,
        Map<ClassName, Predefinition> predefinitions,
        Function<List<List<SingularDescription>>, ? extends Collection<List<SingularDescription>>> grouper,
        Set<PropertyGeneration> propertyGenerations,
        Set<FeatureGeneration> featureGenerations,
        Set<ImplementationGeneration> implementationGenerations,
        boolean exceptionOnEmptySetter
    ) {
        this.namingStrategy = namingStrategy;
        this.propertyStrategy = propertyStrategy;
        this.structuralResolver = structuralResolver;
        this.typeResolver = typeResolver;
        this.accessResolver = accessResolver;
        this.interfaceResolver = interfaceResolver;
        this.nodeResolver = nodeResolver;
        this.condition = condition;
        this.normalizeIntersections = normalizeIntersections;
        this.normalizeEnumerations = normalizeEnumerations;
        this.keyResolvers = keyResolvers;
        this.predefinitions = predefinitions;
        this.grouper = grouper;
        this.propertyGenerations = propertyGenerations;
        this.featureGenerations = featureGenerations;
        this.implementationGenerations = implementationGenerations;
        this.exceptionOnEmptySetter = exceptionOnEmptySetter;
    }

    public StructuralType withNamingStrategy(NamingStrategy namingStrategy) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withPropertyStrategy(PropertyStrategy propertyStrategy) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withStructuralResolver(StructuralResolver<?> structuralResolver) {
        return new StructuralType(
                namingStrategy,
                propertyStrategy,
                structuralResolver.asFactory(),
                typeResolver,
                accessResolver,
                interfaceResolver,
                nodeResolver,
                condition,
                normalizeIntersections,
                normalizeEnumerations,
                keyResolvers,
                predefinitions,
                grouper,
                propertyGenerations,
                featureGenerations,
                implementationGenerations,
                exceptionOnEmptySetter
        );
    }

    public StructuralType withStructuralResolver(Function<Class<?>, StructuralResolver<?>> structuralResolver) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withCondition(BiPredicate<Class<?>, String> condition) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            this.condition.and(condition),
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withTypeResolver(TypeResolver typeResolver) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withAccessResolver(AccessResolver accessResolver) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withInterfaceResolver(InterfaceResolver interfaceResolver) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withNodeResolver(NodeResolver nodeResolver) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withNormalizedIntersections(boolean normalizeIntersections) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withNormalizedEnumerations(boolean normalizeEnumerations) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    @SafeVarargs
    public final StructuralType withNormalizedKeys(Function<Class<?>, ?>... resolvers) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            Arrays.asList(resolvers),
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withPredefinitions(Class<?>... structures) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            Stream.concat(
                predefinitions.entrySet().stream(),
                Arrays.stream(structures).map(structure -> {
                    if (!structure.isAnnotationPresent(CompoundOf.class) && !structure.isAnnotationPresent(EnumerationOf.class)) {
                        throw new IllegalStateException("Expected " + structure.getTypeName() + " to be an a structure");
                    }
                    TemplatedBy templatedBy = structure.getAnnotation(TemplatedBy.class);
                    DelegatedBy delegatedBy = structure.getAnnotation(DelegatedBy.class);
                    return Map.entry(ClassName.get(structure), new Predefinition(
                        templatedBy == null ? null : structure.getAnnotation(TemplatedBy.class).value(),
                        delegatedBy == null ? Collections.emptyMap() : Arrays.stream(delegatedBy.value()).collect(Collectors.toMap(
                            projection -> projection.getAnnotation(ProjectionOf.class).value(),
                            Function.identity()
                        ))
                    ));
                })
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left)),
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withGrouper(Function<List<List<SingularDescription>>, ? extends Collection<List<SingularDescription>>> grouper) {
        return new StructuralType(
                namingStrategy,
                propertyStrategy,
                structuralResolver,
                typeResolver,
                accessResolver,
                interfaceResolver,
                nodeResolver,
                condition,
                normalizeIntersections,
                normalizeEnumerations,
                keyResolvers,
                predefinitions,
                grouper,
                propertyGenerations,
                featureGenerations,
                implementationGenerations,
                exceptionOnEmptySetter
        );
    }

    public StructuralType withProperties(PropertyGeneration... generations) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            generations.length == 0 ? EnumSet.noneOf(PropertyGeneration.class) : EnumSet.of(
                generations[0], Arrays.stream(generations).skip(1).toArray(PropertyGeneration[]::new)
            ),
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withFeatures(FeatureGeneration... generations) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            generations.length == 0 ? EnumSet.noneOf(FeatureGeneration.class) : EnumSet.of(
                generations[0], Arrays.stream(generations).skip(1).toArray(FeatureGeneration[]::new)
            ),
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public StructuralType withImplementations(ImplementationGeneration... generations) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            generations.length == 0 ? EnumSet.noneOf(ImplementationGeneration.class) : EnumSet.of(
                generations[0], Arrays.stream(generations).skip(1).toArray(ImplementationGeneration[]::new)
            ),
            exceptionOnEmptySetter
        );
    }

    public StructuralType withExceptionOnEmptySetter(boolean exceptionOnEmptySetter) {
        return new StructuralType(
            namingStrategy,
            propertyStrategy,
            structuralResolver,
            typeResolver,
            accessResolver,
            interfaceResolver,
            nodeResolver,
            condition,
            normalizeIntersections,
            normalizeEnumerations,
            keyResolvers,
            predefinitions,
            grouper,
            propertyGenerations,
            featureGenerations,
            implementationGenerations,
            exceptionOnEmptySetter
        );
    }

    public Map<ClassName, JavaFile> make(Class<?>... types) {
        return make(Arrays.asList(types));
    }

    public Map<ClassName, JavaFile> make(List<Class<?>> types) {
        Map<Enum<?>, String> enumerations = new HashMap<>();
        CompoundDescription root = CompoundDescription.of(
            typeResolver::merge,
            enumeration -> enumerations.computeIfAbsent(
                enumeration,
                value -> nodeResolver.resolve(value).orElseGet(value::name)
            ),
            grouper,
            singulars -> {
                Function<List<SingularDescription>, List<SingularDescription>> normalizer = new DefaultNormalizer();
                if (normalizeIntersections) {
                    normalizer = normalizer.andThen(IntersectingNormalizer.of(singulars));
                }
                if (normalizeEnumerations) {
                    normalizer = normalizer.andThen(EnumeratingNormalizer.of(singulars));
                }
                for (Function<Class<?>, ?> resolver : keyResolvers) {
                    normalizer = normalizer.andThen(KeyNormalizer.of(singulars, resolver));
                }
                return normalizer;
            },
            SingularDescription.of(structuralResolver, nodeResolver, condition, types)
        );
        PrecomputedResolver resolver = new PrecomputedResolver();
        root.traverse(
            (compound, properties) -> resolver.proccess(compound, false),
            (compound, properties) -> resolver.proccess(compound, true)
        );
        resolver.complete();
        Map<ClassName, JavaFile> files = new LinkedHashMap<>();
        root.traverse((description, constants) -> {
        }, resolver.guard(new StructureEmitter(
            propertyGenerations, featureGenerations, implementationGenerations,
            resolver, resolver, resolver.interfaces, files::put
        )));
        if (implementationGenerations.contains(ImplementationGeneration.TEMPLATE)) {
            root.traverse((description, constants) -> {
            }, resolver.guard(new TemplateEmitter(
                propertyGenerations, featureGenerations,
                resolver, resolver, files::put
            )));
        }
        if (implementationGenerations.contains(ImplementationGeneration.PROJECTION)) {
            root.traverse(new EnumerationEmitter(
                resolver, files::put
            ), resolver.guard(new ProjectionEmitter(
                propertyGenerations, featureGenerations,
                resolver, resolver, typeResolver, accessResolver, exceptionOnEmptySetter, files::put
            )));
        }
        return files;
    }

    private class PrecomputedResolver implements NameResolver, PropertyResolver {

        private final Set<ClassName> reserved = new HashSet<>();

        private final Map<CompoundDescription, ClassName> structures = new HashMap<>(), templates = new HashMap<>();

        private final Map<CompoundDescription, Map<SingularDescription, ClassName>> projections = new HashMap<>();

        private final Map<ClassName, Map<String, Map<PropertyGeneration, String>>> properties = new HashMap<>();

        private final Map<ClassName, List<Class<?>>> interfaces = new HashMap<>();

        @Override
        public ClassName structure(CompoundDescription compound) {
            return structures.get(compound);
        }

        @Override
        public ClassName template(CompoundDescription compound) {
            return templates.get(compound);
        }

        @Override
        public ClassName projection(CompoundDescription compound, SingularDescription singular) {
            return projections.get(compound).get(singular);
        }

        @Override
        public String accessor(ClassName structure, String name, PropertyGeneration sort) {
            return properties.get(structure).get(name).get(sort);
        }

        private boolean proccess(CompoundDescription compound, boolean branch) {
            ClassName structure = namingStrategy.structure(
                compound.getSingulars().stream().filter(
                    singular -> !branch || !singular.isLeaf()
                ).map(SingularDescription::getType).collect(Collectors.toSet()),
                compound.getSort() == CompoundDescription.Sort.ENUMERATED_LEAF,
                reserved::contains
            );
            Predefinition predefinition = predefinitions.get(structure);
            if (predefinition == null && !reserved.add(structure)) {
                throw new IllegalStateException("Name already in use: " + structure);
            }
            structures.put(compound, structure);
            if (branch) {
                if (implementationGenerations.contains(ImplementationGeneration.TEMPLATE)) {
                    ClassName template = predefinition == null ? namingStrategy.template(structure, reserved::contains) : predefinition.template().orElseThrow(
                        () -> new IllegalStateException("Predefined structure " + structure + " does not declare a template")
                    );
                    if (predefinition == null && !reserved.add(template)) {
                        throw new IllegalStateException("Name already in use: " + template);
                    }
                    templates.put(compound, template);
                }
                if (implementationGenerations.contains(ImplementationGeneration.PROJECTION)) {
                    projections.put(compound, compound.getSingulars().stream().collect(Collectors.toMap(
                        Function.identity(),
                        singular -> {
                            ClassName projection = predefinition == null ? namingStrategy.projection(
                                structure,
                                singular.getType(),
                                singular.isLeaf(),
                                reserved::contains
                            ) : predefinition.resolve(singular.getType()).orElseThrow(() -> new IllegalStateException(
                                "Expected predefined " + structure + " to " + (singular.isLeaf() ? "expand" : "project") + " " + singular.getType()
                            ));
                            if (predefinition == null && !reserved.add(projection)) {
                                throw new IllegalStateException("Name already in use: " + projection);
                            }
                            return projection;
                        })
                    ));
                }
            }
            return true;
        }

        private void complete() {
            Map<CompoundDescription, ClassName> processed = structures.entrySet().stream().filter(
                entry -> !predefinitions.containsKey(entry.getValue())
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            processed.forEach((description, name) -> interfaces.put(name, interfaceResolver.resolve(
                name,
                description.getSingulars().stream()
                    .filter(singular -> !singular.isLeaf() || description.getSort().isLeaf())
                    .map(SingularDescription::getType)
                    .collect(Collectors.toList())
            )));
            processed.forEach((description, name) -> description.accept(
                type -> { },
                constants -> { },
                properties -> this.properties.put(name, properties.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        Map<PropertyGeneration, String> values = new EnumMap<>(PropertyGeneration.class);
                        Arrays.stream(PropertyGeneration.values()).filter(generation -> {
                            switch (generation) {
                            case GETTER:
                            case OWNER:
                                return propertyGenerations.contains(generation);
                            case ASSUME:
                                return entry.getValue().getCardinality() == Cardinality.OPTIONAL;
                            case SETTER:
                                if (entry.getKey().isEmpty()) {
                                    return false;
                                } else if (entry.getValue().getDescription().getSort().isLeaf()) {
                                    return !Collections.disjoint(propertyGenerations, EnumSet.of(
                                        PropertyGeneration.SETTER,
                                        PropertyGeneration.TRIAL,
                                        PropertyGeneration.CLEAR,
                                        PropertyGeneration.FLUENT
                                    ));
                                } else {
                                    return !Collections.disjoint(propertyGenerations, EnumSet.of(
                                        PropertyGeneration.SETTER,
                                        PropertyGeneration.TRIAL,
                                        PropertyGeneration.FLUENT,
                                        PropertyGeneration.MERGE,
                                        PropertyGeneration.CLEAR,
                                        PropertyGeneration.FACTORY
                                    ));
                                }
                            case MERGE:
                            case FACTORY:
                                return !entry.getKey().isEmpty()
                                    && !entry.getValue().getDescription().getSort().isLeaf()
                                    && propertyGenerations.contains(generation);
                            default:
                                return !entry.getKey().isEmpty() && propertyGenerations.contains(generation);
                            }
                        }).forEach(generation -> entry.getValue().getDescription().accept(
                            type -> values.put(generation, propertyStrategy.property(
                                name, entry.getKey(), TypeName.get(type),
                                entry.getValue().getCardinality(), generation
                            )), ignored -> values.put(generation, propertyStrategy.property(
                                name, entry.getKey(), structures.get(entry.getValue().getDescription()),
                                entry.getValue().getCardinality(), generation
                            )), ignored -> values.put(generation, propertyStrategy.property(
                                name, entry.getKey(), structures.get(entry.getValue().getDescription()),
                                entry.getValue().getCardinality(), generation
                            ))
                        ));
                        return values;
                    }
                )))
            ));
        }

        private BiPredicate<CompoundDescription, Map<String, CompoundDescription.Property>> guard(
            BiConsumer<CompoundDescription, Map<String, CompoundDescription.Property>> delegate
        ) {
            return (description, properties) -> {
                if (predefinitions.containsKey(structures.get(description))) {
                    return false;
                } else {
                    delegate.accept(description, properties);
                    return true;
                }
            };
        }
    }

    private static class Predefinition {

        private final Class<?> template;

        private final Map<Class<?>, Class<?>> delegations;

        private Predefinition(Class<?> template, Map<Class<?>, Class<?>> delegations) {
            this.template = template;
            this.delegations = delegations;
        }

        private Optional<ClassName> template() {
            return Optional.ofNullable(template).map(ClassName::get);
        }

        private Optional<ClassName> resolve(Class<?> delegate) {
            return Optional.ofNullable(delegations.get(delegate)).map(ClassName::get);
        }
    }
}
