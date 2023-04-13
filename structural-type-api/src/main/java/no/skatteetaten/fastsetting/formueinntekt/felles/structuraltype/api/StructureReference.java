package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.lang.annotation.*;

@Documented
@Inherited
@Target(ElementType.TYPE_PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface StructureReference {

    String value();
}
