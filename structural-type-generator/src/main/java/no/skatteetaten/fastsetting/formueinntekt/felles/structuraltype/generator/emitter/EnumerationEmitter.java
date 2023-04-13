package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.EnumerationOf;

public class EnumerationEmitter implements BiConsumer<CompoundDescription, Map<String, Map<Class<?>, Enum<?>>>> {

    private final NameResolver nameResolver;
    private final BiConsumer<ClassName, JavaFile> consumer;

    public EnumerationEmitter(
        NameResolver nameResolver,
        BiConsumer<ClassName, JavaFile> consumer
    ) {
        this.nameResolver = nameResolver;
        this.consumer = consumer;
    }

    @Override
    public void accept(CompoundDescription compound, Map<String, Map<Class<?>, Enum<?>>> constants) {
        ClassName structure = nameResolver.structure(compound);
        TypeSpec.Builder builder = TypeSpec.enumBuilder(structure)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(EnumerationOf.class).addMember("value", CodeBlock.builder().add(
                "{ " + String.join(", ", Collections.nCopies(compound.getSingulars().size(), "$T.class")) + " }",
                compound.getSingulars().stream().map(SingularDescription::getType).toArray(Object[]::new)
            ).build()).build());
        constants.keySet().forEach(builder::addEnumConstant);
        CodeBlock.Builder unwrap = CodeBlock.builder()
            .beginControlFlow("if (value == null)")
            .addStatement("return null");
        Map<Enum<?>, String> names = constants.entrySet().stream()
            .flatMap(entry -> entry.getValue().values().stream().map(enumeration -> Map.entry(enumeration, entry.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        compound.getSingulars().forEach(singular -> {
            CodeBlock.Builder wrap = CodeBlock.builder()
                .beginControlFlow("if (value == null)")
                .addStatement("return null")
                .endControlFlow()
                .beginControlFlow("switch (value)");
            @SuppressWarnings("unchecked")
            Enum<?>[] enumerations = ((Class<? extends Enum<?>>) singular.getType()).getEnumConstants();
            for (Enum<?> enumeration : enumerations) {
                wrap.add("case $N:\n", enumeration.name())
                    .indent()
                    .addStatement("return $T.$N", structure, names.get(enumeration))
                    .unindent();
            }
            builder.addMethod(MethodSpec.methodBuilder("wrap")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(structure)
                .addParameter(singular.getType(), "value")
                .addCode(wrap.add("default:\n")
                    .indent()
                    .addStatement("throw new $T(value.toString())", IllegalStateException.class)
                    .unindent()
                    .endControlFlow()
                    .build())
                .build());
            unwrap.nextControlFlow("else if (type == $T.class)", singular.getType()).beginControlFlow("switch (value)");
            constants.forEach((constant, values) -> {
                unwrap.add("case $N:\n", constant).indent();
                if (values.containsKey(singular.getType())) {
                    unwrap.addStatement("return (E) $T.$N", singular.getType(), values.get(singular.getType()).name());
                } else {
                    unwrap.addStatement("return null");
                }
                unwrap.unindent();
            });
            unwrap.add("default:\n")
                .indent()
                .addStatement("throw new $T(value.toString())", IllegalStateException.class)
                .unindent()
                .endControlFlow();
        });
        builder.addMethod(MethodSpec.methodBuilder("unwrap")
            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember(
                "value", CodeBlock.builder().add("$S", "unchecked").build()
            ).build())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(TypeVariableName.get(
                "E",
                ParameterizedTypeName.get(ClassName.get(Enum.class), TypeVariableName.get("E"))
            ))
            .returns(TypeVariableName.get("E"))
            .addParameter(structure, "value")
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("E")), "type")
            .addCode(unwrap.nextControlFlow("else").addStatement(
                "throw new $T(type.getTypeName() + $S + $T.class.getTypeName())",
                IllegalArgumentException.class,
                " is not enumerated by ",
                structure
            ).endControlFlow().build())
            .build());
        consumer.accept(structure, JavaFile.builder(
            structure.packageName(), builder.alwaysQualify(
                compound.getSingulars().stream()
                    .map(singular -> singular.getType().getSimpleName())
                    .distinct()
                    .toArray(String[]::new)
            ).build()
        ).skipJavaLangImports(true).build());

    }
}
