package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson;

import java.lang.reflect.Constructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.CompoundOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.DelegationOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ExpansionOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplateOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplatedBy;

class StructuralTypeAnnotationIntrospector extends NopAnnotationIntrospector {

    private final String expansion, getter;

    StructuralTypeAnnotationIntrospector(String expansion, String getter) {
        this.expansion = expansion;
        this.getter = getter;
    }

    @Override
    public JavaType refineDeserializationType(MapperConfig<?> config, Annotated a, JavaType baseType) throws JsonMappingException {
        TemplatedBy templatedBy = a.getAnnotation(TemplatedBy.class);
        if (templatedBy != null) {
            return config.constructType(templatedBy.value());
        }
        return super.refineDeserializationType(config, a, baseType);
    }

    @Override
    public JavaType refineSerializationType(MapperConfig<?> config, Annotated a, JavaType baseType) throws JsonMappingException {
        DelegationOf projectionOf = a.getAnnotation(DelegationOf.class);
        if (projectionOf != null) {
            return config.constructType(projectionOf.value());
        }
        TemplateOf templateOf = a.getAnnotation(TemplateOf.class);
        if (templateOf != null) {
            return config.constructType(templateOf.value());
        }
        return super.refineSerializationType(config, a, baseType);
    }

    @Override
    public String findImplicitPropertyName(AnnotatedMember member) {
        if (isExpansionProperty(member) || isTemplateConstructorParameter(member)) {
            return expansion;
        }
        return super.findImplicitPropertyName(member);
    }

    @Override
    public Integer findPropertyIndex(Annotated ann) {
        if (ann instanceof AnnotatedMember && isExpansionProperty((AnnotatedMember) ann)) {
            return 0;
        }
        return super.findPropertyIndex(ann);
    }

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
        if (a instanceof AnnotatedConstructor && ((AnnotatedConstructor) a).getDeclaringClass().isAnnotationPresent(TemplateOf.class)) {
            return JsonCreator.Mode.PROPERTIES;
        }
        return super.findCreatorAnnotation(config, a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        if (isStructuralSetter(a)) {
            return PropertyName.USE_DEFAULT;
        }
        return super.findNameForDeserialization(a);
    }

    @Override
    public JsonSetter.Value findSetterInfo(Annotated a) {
        if (isStructuralSetter(a)) {
            return JsonSetter.Value.construct(Nulls.DEFAULT, Nulls.DEFAULT);
        }
        return super.findSetterInfo(a);
    }

    @Override
    public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1, AnnotatedMethod setter2) {
        if (isStructuralSetter(setter1)) {
            if (!isStructuralSetter(setter2)) {
                return setter1;
            }
        } else if (isStructuralSetter(setter2)) {
            return setter2;
        }
        return super.resolveSetterConflict(config, setter1, setter2);
    }

    private static boolean isStructuralSetter(Annotated a) {
        return a instanceof AnnotatedMethod
                && isStructuralType(((AnnotatedMember) a).getDeclaringClass())
                && ((AnnotatedMethod) a).getParameterCount() == 1
                && isStructuralType(((AnnotatedMethod) a).getRawParameterType(0));
    }

    private boolean isExpansionProperty(AnnotatedMember member) {
        return member.getName().equals(getter) && isStructuralType(member.getMember().getDeclaringClass());
    }

    private static boolean isStructuralType(Class<?> type) {
        return type.isAnnotationPresent(CompoundOf.class)
            || type.isAnnotationPresent(DelegationOf.class)
            || type.isAnnotationPresent(TemplateOf.class)
            || type.isAnnotationPresent(ExpansionOf.class);
    }

    private static boolean isTemplateConstructorParameter(AnnotatedMember parameter) {
        return parameter instanceof AnnotatedParameter
            && parameter.getMember() instanceof Constructor<?>
            && parameter.getDeclaringClass().isAnnotationPresent(TemplateOf.class);
    }
}
