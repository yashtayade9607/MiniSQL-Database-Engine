package com.novadb.parser.ast;

/**
 * Base interface for all expressions.
 */
public sealed interface Expression permits
        BinaryExpression,
        LiteralExpression,
        IdentifierExpression {
}
