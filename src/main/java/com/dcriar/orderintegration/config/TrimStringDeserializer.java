package com.dcriar.orderintegration.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class TrimStringDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        String value = parser.getString();
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
