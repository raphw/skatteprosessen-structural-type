package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplatedBy;

@TemplatedBy(SimpleStructure.Template.class)
public interface SimpleStructure {

    String getValue();

    void setValue(String value);

    class Template implements SimpleStructure {

        private String value;

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
