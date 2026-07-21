package com.novadb.executor;

import com.novadb.exception.NovaDbException;
import com.novadb.model.Row;
import com.novadb.parser.ast.BinaryExpression;
import com.novadb.parser.ast.Expression;
import com.novadb.parser.ast.IdentifierExpression;
import com.novadb.parser.ast.LiteralExpression;

public class ExpressionEvaluator {

    public static Object evaluate(Expression expr, Row row) {
        if (expr instanceof LiteralExpression lit) {
            return lit.value();
        } else if (expr instanceof IdentifierExpression id) {
            return row.get(id.name());
        } else if (expr instanceof BinaryExpression bin) {
            Object left = evaluate(bin.left(), row);
            Object right = evaluate(bin.right(), row);
            return evaluateBinary(left, bin.operator(), right);
        }
        throw new NovaDbException("Unknown expression type: " + expr.getClass());
    }

    private static Object evaluateBinary(Object left, String op, Object right) {
        if (op.equalsIgnoreCase("AND")) {
            return toBoolean(left) && toBoolean(right);
        } else if (op.equalsIgnoreCase("OR")) {
            return toBoolean(left) || toBoolean(right);
        }

        if (left == null || right == null) {
            if (op.equals("=")) return left == right;
            if (op.equals("!=")) return left != right;
            return false;
        }

        if (left instanceof Number numL && right instanceof Number numR) {
            double l = numL.doubleValue();
            double r = numR.doubleValue();
            return switch (op) {
                case "=" -> l == r;
                case "!=" -> l != r;
                case ">" -> l > r;
                case ">=" -> l >= r;
                case "<" -> l < r;
                case "<=" -> l <= r;
                default -> throw new NovaDbException("Unsupported operator for numbers: " + op);
            };
        } else if (left instanceof String strL && right instanceof String strR) {
            return switch (op) {
                case "=" -> strL.equals(strR);
                case "!=" -> !strL.equals(strR);
                case "LIKE" -> strL.matches(strR.replace("%", ".*"));
                default -> throw new NovaDbException("Unsupported operator for strings: " + op);
            };
        } else if (left instanceof Boolean boolL && right instanceof Boolean boolR) {
            return switch (op) {
                case "=" -> boolL.equals(boolR);
                case "!=" -> !boolL.equals(boolR);
                default -> throw new NovaDbException("Unsupported operator for booleans: " + op);
            };
        }

        // fallback to string comparison
        String strL = left.toString();
        String strR = right.toString();
        return switch (op) {
            case "=" -> strL.equals(strR);
            case "!=" -> !strL.equals(strR);
            default -> throw new NovaDbException("Unsupported operator for mixed types: " + op);
        };
    }

    private static boolean toBoolean(Object obj) {
        if (obj instanceof Boolean b) return b;
        if (obj == null) return false;
        throw new NovaDbException("Expected boolean, got: " + obj.getClass());
    }
}
