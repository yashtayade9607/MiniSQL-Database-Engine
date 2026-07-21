package com.novadb.parser.ast;

public record BinaryExpression(Expression left, String operator, Expression right) implements Expression {}
