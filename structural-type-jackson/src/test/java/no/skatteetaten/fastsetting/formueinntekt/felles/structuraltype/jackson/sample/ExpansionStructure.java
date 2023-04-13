package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.CompoundOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ExpansionOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplateOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplatedBy;

@TemplatedBy(ExpansionStructure.Template.class)
public interface ExpansionStructure {

    NestedStructure getNested();

    @JsonSetter
    void setNested(NestedStructure nested);

    void setNested(String value);

    class Template implements ExpansionStructure {

        private NestedStructure nested;

        @Override
        public NestedStructure getNested() {
            return nested;
        }

        @Override
        @JsonSetter
        public void setNested(NestedStructure nested) {
            this.nested = nested;
        }

        @Override
        public void setNested(String value) {
            nested = new NestedExpansion(value);
        }
    }

    @CompoundOf({})
    @TemplatedBy(NestedTemplate.class)
    interface NestedStructure {

        String get();

        String getValue();

        void setValue(String value);
    }

    @ExpansionOf(NestedStructure.class)
    class NestedExpansion implements NestedStructure {

        private final String value;

        public NestedExpansion(String value) {
            this.value = value;
        }

        @Override
        public String get() {
            return value;
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue(String value) {
        }
    }

    @TemplateOf(NestedStructure.class)
    class NestedTemplate implements NestedStructure {

        private final String expansion;

        private String value;

        public NestedTemplate() {
            expansion = null;
        }

        public NestedTemplate(String expansion) {
            this.expansion = expansion;
        }

        @Override
        public String get() {
            return expansion;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }
    }
}
