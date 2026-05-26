package com.mju.Jumoney.global.realtime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class RealtimeBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token != null && token.isNumeric()) {
            return parser.getDecimalValue();
        }
        if (token == JsonToken.VALUE_STRING) {
            return parseString(parser.getValueAsString());
        }

        parser.skipChildren();
        return null;
    }

    private BigDecimal parseString(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replace(",", "").trim();
        if (normalized.isEmpty() || "-".equals(normalized)) {
            return null;
        }
        if (normalized.endsWith("%")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
