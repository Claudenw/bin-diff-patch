/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xenei.diffpatch;

/**
 * An enumeration that defines the three operations: Delete, Insert and Equal.
 *
 * <p>
 * The data structure representing a diff is a Linked list of Diff.Fragment
 * objects. For example:
 *
 * <pre>
 * { Diff(Operation.DELETE, "Hello"), Diff(Operation.INSERT, "Goodbye"), Diff(Operation.EQUAL, " world.") }
 * </pre>
 *
 * means: delete "Hello", add "Goodbye" and keep " world."
 * </p>
 */
public enum Operation {
    DELETE('-'), INSERT('+'), EQUAL(' ');

    private char diffChar; // price of each apple

    // Constructor
    Operation(final char diffChar) {
        this.diffChar = diffChar;
    }

    public char getChar() {
        return diffChar;
    }

    /**
     * Create an operation from a char. The char must be one of:
     * <ul>
     * <li>'-' : Delete</li>
     * <li>'+' : Insert</li>
     * <li>' ' : Equal</li>
     * </ul>
     *
     * @param ch the character to create the operation from.
     * @return the operation
     * @throws IllegalArgumentException if not one of the specified characters.
     */
    public static Operation fromChar(final char ch) {
        switch (ch) {
        case '-':
            return DELETE;
        case '+':
            return INSERT;
        case ' ':
            return EQUAL;
        default:
            throw new IllegalArgumentException("character must be '-'. '+' or ' '");
        }

    }
}