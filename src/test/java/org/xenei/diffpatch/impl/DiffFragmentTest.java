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
package org.xenei.diffpatch.impl;

import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.SpanBuffer;
import org.junit.Before;
import org.junit.Test;
import org.xenei.diffpatch.Operation;
import org.xenei.diffpatch.diff.DiffFragment;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiffFragmentTest {
    private DiffFragment fragment;
    private SpanBuffer bb;

    @Before
    public void setup() {
        bb = Factory.wrap("Hello World");
        fragment = new DiffFragment(Operation.DELETE, bb);
    }

    @Test
    public void startsWithTest() throws IOException {
        DiffFragment other = new DiffFragment(Operation.EQUAL, Factory.wrap("Hello"));
        assertTrue(fragment.startsWith(other));
        assertFalse(other.startsWith(fragment));
        other = new DiffFragment(Operation.EQUAL, Factory.wrap("World"));
        assertFalse(fragment.startsWith(other));
        assertFalse(other.startsWith(fragment));
    }

    @Test
    public void endsWithTest() throws IOException {
        DiffFragment other = new DiffFragment(Operation.EQUAL, Factory.wrap("Hello"));
        assertFalse(fragment.endsWith(other));
        assertFalse(other.endsWith(fragment));
        other = new DiffFragment(Operation.EQUAL, Factory.wrap("World"));
        assertTrue(fragment.endsWith(other));
        assertFalse(other.endsWith(fragment));

    }

    @Test
    public void getOperationTest() {
        assertEquals(Operation.DELETE, fragment.getOperation());
    }

    @Test
    public void getTextTest() throws IOException {
        assertEquals("Hello World", fragment.getText());
    }

}
