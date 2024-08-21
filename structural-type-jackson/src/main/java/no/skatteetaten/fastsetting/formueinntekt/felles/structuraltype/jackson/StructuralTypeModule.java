package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class StructuralTypeModule extends SimpleModule {

    public static final String EXPANSION = "$value";

    private final String expansion, getter;

    public StructuralTypeModule() {
        this(EXPANSION);
    }

    public StructuralTypeModule(String expansion) {
        this(expansion, "get");
    }

    public StructuralTypeModule(String expansion, String getter) {
        this.expansion = expansion;
        this.getter = getter;
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);

        context.appendAnnotationIntrospector(new StructuralTypeAnnotationIntrospector(expansion, getter));
    }
}
