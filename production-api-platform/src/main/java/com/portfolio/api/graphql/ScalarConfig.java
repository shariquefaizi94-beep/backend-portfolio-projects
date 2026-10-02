package com.portfolio.api.graphql;

import graphql.language.StringValue;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Configuration for custom GraphQL scalar types.
 */
@Configuration
public class ScalarConfig {
    
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            .scalar(uuidScalar())
            .scalar(bigDecimalScalar())
            .scalar(dateTimeScalar());
    }
    
    private GraphQLScalarType uuidScalar() {
        return GraphQLScalarType.newScalar()
            .name("UUID")
            .description("UUID scalar type")
            .coercing(new Coercing<UUID, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof UUID) {
                        return dataFetcherResult.toString();
                    }
                    throw new CoercingSerializeException("Expected UUID type");
                }
                
                @Override
                public UUID parseValue(Object input) throws CoercingParseValueException {
                    try {
                        if (input instanceof String) {
                            return UUID.fromString((String) input);
                        }
                        throw new CoercingParseValueException("Expected String for UUID");
                    } catch (IllegalArgumentException e) {
                        throw new CoercingParseValueException("Invalid UUID format", e);
                    }
                }
                
                @Override
                public UUID parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return UUID.fromString(((StringValue) input).getValue());
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseLiteralException("Invalid UUID format", e);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected StringValue for UUID");
                }
            })
            .build();
    }
    
    private GraphQLScalarType bigDecimalScalar() {
        return GraphQLScalarType.newScalar()
            .name("BigDecimal")
            .description("BigDecimal scalar type for precise decimal values")
            .coercing(new Coercing<BigDecimal, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof BigDecimal) {
                        return dataFetcherResult.toString();
                    }
                    throw new CoercingSerializeException("Expected BigDecimal type");
                }
                
                @Override
                public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                    try {
                        if (input instanceof String) {
                            return new BigDecimal((String) input);
                        }
                        if (input instanceof Number) {
                            return new BigDecimal(input.toString());
                        }
                        throw new CoercingParseValueException("Expected String or Number for BigDecimal");
                    } catch (NumberFormatException e) {
                        throw new CoercingParseValueException("Invalid BigDecimal format", e);
                    }
                }
                
                @Override
                public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return new BigDecimal(((StringValue) input).getValue());
                        } catch (NumberFormatException e) {
                            throw new CoercingParseLiteralException("Invalid BigDecimal format", e);
                        }
                    }
                    if (input instanceof graphql.language.IntValue) {
                        return new BigDecimal(((graphql.language.IntValue) input).getValue());
                    }
                    if (input instanceof graphql.language.FloatValue) {
                        return ((graphql.language.FloatValue) input).getValue();
                    }
                    throw new CoercingParseLiteralException("Expected StringValue or numeric value for BigDecimal");
                }
            })
            .build();
    }
    
    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("DateTime scalar type (ISO-8601 format)")
            .coercing(new Coercing<Instant, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Instant) {
                        return dataFetcherResult.toString();
                    }
                    throw new CoercingSerializeException("Expected Instant type");
                }
                
                @Override
                public Instant parseValue(Object input) throws CoercingParseValueException {
                    try {
                        if (input instanceof String) {
                            return Instant.parse((String) input);
                        }
                        throw new CoercingParseValueException("Expected String for DateTime");
                    } catch (DateTimeParseException e) {
                        throw new CoercingParseValueException("Invalid DateTime format", e);
                    }
                }
                
                @Override
                public Instant parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return Instant.parse(((StringValue) input).getValue());
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException("Invalid DateTime format", e);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected StringValue for DateTime");
                }
            })
            .build();
    }
}
