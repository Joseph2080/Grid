package org.bazar.vektrlabs.validation;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.jericho.common.exception.InvalidParameterException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class AttributeSchemaValidator {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;

    public AttributeSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12
        );
    }

    public void validate(
            String schema,
            Map<String, String> attributes) {
        Assert.notNull(schema, "Attribute schema must not be null");
        Assert.notNull(attributes, "Attributes must not be null");
        try {
            Schema jsonSchema = schemaRegistry.getSchema(
                    schema,
                    InputFormat.JSON
            );
            String attributesJson = objectMapper.writeValueAsString(attributes);
            List<Error> errors = jsonSchema.validate(attributesJson, InputFormat.JSON);
            if (!errors.isEmpty()) {
                String message = errors.stream()
                        .map(Error::getMessage)
                        .sorted()
                        .reduce(
                                (first, second) ->
                                        first + "; " + second
                        )
                        .orElse("Invalid attributes");

                throw new InvalidParameterException(
                        "Invalid attributes: " + message
                );
            }

        } catch (InvalidParameterException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to validate attributes against schema",
                    exception
            );
        }
    }
}