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
package com.ibm.common.diffpatch.patch;

import static org.junit.Assert.assertEquals;

import org.xenei.span.LongSpan;
import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.SpanBuffer;
import org.junit.Test;
import org.xenei.diffpatch.Operation;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.diffpatch.patch.PatchFragment;

public class PatchFragmentTest {
    private SpanBuffer context = Factory.wrap("0123456789ABCDEF");
    private SpanBuffer longContext = Factory.wrap("0123456789ABCDEF0123456789ABCDEF");
    private PatchFragment fragment;
    private int padding = 3;
    private int shortPadding = padding - 1;

    @Test
    public void shortenInMiddle() {
        LongSpan l1 = LongSpan.fromLength(6, 4);
        LongSpan r1 = LongSpan.fromLength(6, 2);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("6789")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("HI")));
        fragment.addContext(context, padding);
        assertEquals(l1.getOffset() - padding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + (2 * padding), fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - padding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + (2 * padding), fragment.getRightSpan().getLength());
        assertEquals("@@ -4,10 +4,8 @@\n 345\n-6789\n+HI\n ABC\n", fragment.toString());
    }

    @Test
    public void shortenInMiddleLongContext() {
        int effectivePadding = 12;
        int leftPadding = 6; // start position.
        LongSpan l1 = LongSpan.fromLength(6, 4);
        LongSpan r1 = LongSpan.fromLength(6, 2);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("6789")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("HI")));
        fragment.addContext(longContext, padding);
        assertEquals(0, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + effectivePadding + leftPadding, fragment.getLeftSpan().getLength());
        assertEquals(0, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + effectivePadding + leftPadding, fragment.getRightSpan().getLength());
        assertEquals("@@ -1,22 +1,20 @@\n 012345\n-6789\n+HI\n ABCDEF012345\n", fragment.toString());
    }

    @Test
    public void lengthenInMiddle() {
        LongSpan l1 = LongSpan.fromLength(6, 2);
        LongSpan r1 = LongSpan.fromLength(6, 4);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("67")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("-HI-")));
        fragment.addContext(context, padding);
        assertEquals(l1.getOffset() - padding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + (2 * padding), fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - padding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + (2 * padding), fragment.getRightSpan().getLength());
        assertEquals("@@ -4,8 +4,10 @@\n 345\n-67\n+-HI-\n 89A\n", fragment.toString());
    }

    @Test
    public void shortenAtEnd() {
        /*
         * be padding-1 from end on l1
         */
        LongSpan l1 = LongSpan.fromLength(10, 4);
        LongSpan r1 = LongSpan.fromLength(10, 2);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("ABCD")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("HI")));
        fragment.addContext(context, padding);
        assertEquals(l1.getOffset() - padding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + padding + shortPadding, fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - padding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + padding + shortPadding, fragment.getRightSpan().getLength());
        assertEquals("@@ -8,9 +8,7 @@\n 789\n-ABCD\n+HI\n EF\n", fragment.toString());
    }

    @Test
    public void shortenAtEndLongContext() {
        int effectivePadding = 15;
        int rightPadding = 2; // 2 bytes before end
        LongSpan l1 = LongSpan.fromLength(26, 4);
        LongSpan r1 = LongSpan.fromLength(26, 2);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("ABCD")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("HI")));
        fragment.addContext(longContext, padding);
        assertEquals(l1.getOffset() - effectivePadding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + effectivePadding + rightPadding, fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - effectivePadding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + effectivePadding + rightPadding, fragment.getRightSpan().getLength());
        assertEquals("@@ -12,21 +12,19 @@\n BCDEF0123456789\n-ABCD\n+HI\n EF\n", fragment.toString());
    }

    @Test
    public void lengthenAtEnd() {
        /*
         * be padding-1 from end on l1
         */
        LongSpan l1 = LongSpan.fromLength(12, 2);
        LongSpan r1 = LongSpan.fromLength(12, 4);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("CD")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("-HI-")));
        fragment.addContext(context, padding);
        assertEquals(l1.getOffset() - padding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + padding + shortPadding, fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - padding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + padding + shortPadding, fragment.getRightSpan().getLength());
        assertEquals("@@ -10,7 +10,9 @@\n 9AB\n-CD\n+-HI-\n EF\n", fragment.toString());
    }

    @Test
    public void shortenAtStart() {
        /*
         * be padding-1 from start on l1 and l2
         */
        LongSpan l1 = LongSpan.fromLength(2, 4);
        LongSpan r1 = LongSpan.fromLength(2, 2);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("2345")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("HI")));
        fragment.addContext(context, padding);
        assertEquals(l1.getOffset() - shortPadding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + shortPadding + padding, fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - shortPadding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + shortPadding + padding, fragment.getRightSpan().getLength());
        assertEquals("@@ -1,9 +1,7 @@\n 01\n-2345\n+HI\n 678\n", fragment.toString());
    }

    @Test
    public void shortenAtStartLongContext() {
        int effectivePadding = 15;
        int leftPadding = 2; // start position.
        LongSpan l1 = LongSpan.fromLength(2, 4);
        LongSpan r1 = LongSpan.fromLength(2, 2);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("2345")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("HI")));
        fragment.addContext(longContext, padding);
        assertEquals(0, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + effectivePadding + leftPadding, fragment.getLeftSpan().getLength());
        assertEquals(0, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + effectivePadding + leftPadding, fragment.getRightSpan().getLength());
        assertEquals("@@ -1,21 +1,19 @@\n 01\n-2345\n+HI\n 6789ABCDEF01234\n", fragment.toString());
    }

    @Test
    public void lengthenAtStart() {
        /*
         * be padding-1 from start on l1
         */
        LongSpan l1 = LongSpan.fromLength(2, 2);
        LongSpan r1 = LongSpan.fromLength(2, 4);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("23")));
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("-HI-")));
        fragment.addContext(context, padding);
        assertEquals(l1.getOffset() - shortPadding, fragment.getLeftSpan().getOffset());
        assertEquals(l1.getLength() + shortPadding + padding, fragment.getLeftSpan().getLength());
        assertEquals(r1.getOffset() - shortPadding, fragment.getRightSpan().getOffset());
        assertEquals(r1.getLength() + shortPadding + padding, fragment.getRightSpan().getLength());
        assertEquals("@@ -1,7 +1,9 @@\n 01\n-23\n+-HI-\n 456\n", fragment.toString());
    }

    @Test
    public void lengthenBeforeText() {
        /*
         * be padding-1 from end on l1
         */
        LongSpan l1 = LongSpan.fromLength(0, 1);
        LongSpan r1 = LongSpan.fromLength(0, 4);
        fragment = new PatchFragment(l1, r1);
        fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("ABC")));
        fragment.add(new DiffFragment(Operation.EQUAL, Factory.wrap("D")));
        fragment.addContext(Factory.wrap("D"), padding);
        assertEquals(0, fragment.getLeftSpan().getOffset());
        assertEquals(1, fragment.getLeftSpan().getLength());
        assertEquals(0, fragment.getRightSpan().getOffset());
        assertEquals(4, fragment.getRightSpan().getLength());
        assertEquals("@@ -1 +1,4 @@\n+ABC\n D\n", fragment.toString());
    }
}
