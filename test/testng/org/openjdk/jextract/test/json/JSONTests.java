/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jextract.test.json;

import java.util.Optional;
import java.util.OptionalDouble;
import org.openjdk.jextract.json.JSON;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class JSONTests {
    // delta for floating point comparisons
    private static final double DELTA = 0.0001;

    public record Point(int x, int y) {}

    @Test
    public void testRecordAsJSON() {
        Point pt = new Point(233, 435);
        String json = JSON.toString(pt, false);
        assertEquals(json, "{\"x\":233,\"y\":435}");
    }

    @Test
    public void testParseRecordFromJSON() {
        String json = "{\"x\":-989,\"y\":256}";
        Point pt = JSON.parse(json, Point.class);
        assertEquals(pt.x(), -989);
        assertEquals(pt.y(), 256);
    }

    @Test
    public void testRecordAsJSONPretty() {
        Point pt = new Point(555, 242);
        String json = JSON.toString(pt, true);
        assertEquals(json, "{\n  \"x\": 555,\n  \"y\": 242\n}");
        // default of pretty is true!
        json = JSON.toString(pt);
        assertEquals(json, "{\n  \"x\": 555,\n  \"y\": 242\n}");
    }

    @Test
    public void testParseRecordFromJSONPretty() {
        String json = "{\n  \"x\": 233,\n  \"y\": 435\n}";
        Point pt = JSON.parse(json, Point.class);
        assertEquals(pt.x(), 233);
        assertEquals(pt.y(), 435);
    }

    @Test
    public void testParseRecordFromJSONExtended() {
        String json = """
        {
          "x" : 888,  // X coordinate value
          "y" : -777, // Y coordinate value
        }""";
        Point pt = JSON.parse(json, Point.class, true);
        assertEquals(pt.x(), 888);
        assertEquals(pt.y(), -777);

        // parsing without extension should fail!
        boolean gotException = false;
        try {
            JSON.parse(json, Point.class, false);
        } catch (RuntimeException re) {
            gotException = true;
        }
        assertTrue(gotException);

        // parsing without extension should fail!
        gotException = false;
        try {
            // default mode is to disable extensions
            JSON.parse(json, Point.class);
        } catch (RuntimeException re) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    // simple expression language tree model

    // Expr = BinaryExpr | UnaryExpr | OptionalDouble
    public record Expr(
        Optional<BinaryExpr> binary,
        Optional<UnaryExpr> unary,
        OptionalDouble literal) {
        public Expr {
            int emptyCount = 0;
            if (binary.isEmpty()) emptyCount++;
            if (unary.isEmpty()) emptyCount++;
            if (literal.isEmpty()) emptyCount++;
            if (emptyCount != 2)
                throw new RuntimeException("only one of binary, unary, literal has to be non-empty");
        }

        @Override
        public String toString() {
            if (binary.isPresent())
                return binary.get().toString();
            if (unary.isPresent())
                return unary.get().toString();
            if (literal.isPresent())
                return Double.toString(literal.getAsDouble());
            // atleast one should be non-empty
            // as enforced in the constructor
            throw new RuntimeException("should not reach here");
        }
    }

    public enum UnaryOperator { PLUS, MINUS;
        @Override
        public String toString() {
            return this == PLUS ? "+" : "-";
        }
    }

    public record UnaryExpr(UnaryOperator operator, Expr expr) {
        @Override
        public String toString() {
            return String.format("(%s%s)",
                operator.toString(), expr.toString());
        }
    }

    public enum BinaryOperator { ADD, SUB, MUL, DIV;
        @Override
        public String toString() {
            return switch (this) {
                case ADD -> "+";
                case SUB -> "-";
                case MUL -> "*";
                case DIV -> "/";
            };
        }
    }

    public record BinaryExpr(BinaryOperator operator, Expr left, Expr right) {
        @Override
        public String toString() {
            return String.format("(%s %s %s)",
                left.toString(), operator.toString(), right.toString());
        }
    }

    @Test
    public void testExprTree() {
        // -33.2 / 44.5

        // parse expression object from JSON a String
        Expr expr = JSON.parse("""
        {
            "binary": {
                "operator": "DIV",
                "left": { "unary" : { "operator": "MINUS", "expr": { "literal": 33.2 } } },
                "right": { "literal": 44.5 }
            }
        }
        """,
        Expr.class);

        assertTrue(expr.literal().isEmpty());
        assertTrue(expr.unary().isEmpty());
        assertTrue(expr.binary().isPresent());

        BinaryExpr binaryExpr = expr.binary().get();
        assertEquals(binaryExpr.operator(), BinaryOperator.DIV);

        Expr leftExpr = binaryExpr.left();
        assertTrue(leftExpr.binary().isEmpty());
        assertTrue(leftExpr.literal().isEmpty());
        UnaryExpr unaryExpr = leftExpr.unary().get();
        assertEquals(unaryExpr.operator(), UnaryOperator.MINUS);
        assertEquals(unaryExpr.expr().literal().getAsDouble(), 33.2, DELTA);

        Expr rightExpr = binaryExpr.right();
        assertTrue(rightExpr.unary().isEmpty());
        assertTrue(rightExpr.binary().isEmpty());
        assertEquals(rightExpr.literal().getAsDouble(), 44.5, DELTA);

        assertEquals(expr.toString(), "((-33.2) / 44.5)");
    }
}
