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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.SpanBuffer;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xenei.diffpatch.Diff;
import org.xenei.diffpatch.Operation;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.diffpatch.patch.PatchFragment;
import org.xenei.span.LongSpan;

public class PatchFragmentTest {
	private PatchFragment fragment;
	private SpanBuffer bb;
	private int patchMargin = 4;

	@Before
	public void setup() {
		bb = Factory.wrap("Hello World");
		fragment = new PatchFragment(LongSpan.EMPTY, LongSpan.EMPTY);
	}

	@Test
	public void addTest() throws IOException {
		assertTrue(fragment.isEmpty());

		// small equal test should not be added
		DiffFragment other = new DiffFragment(Operation.EQUAL, Factory.wrap("Hello"));
		fragment.add(other, patchMargin, bb, 0);
		assertTrue(fragment.isEmpty());

		other = new DiffFragment(Operation.DELETE, Factory.wrap("Hello".getBytes()));
		fragment.add(other, patchMargin, bb, 5);
		assertFalse(fragment.isEmpty());
	}

	@Test
	public void addContextTest() throws IOException {

		DiffFragment other = new DiffFragment(Operation.DELETE, Factory.wrap("Hello "));
		SpanBuffer after = fragment.add(other, patchMargin, bb, 0);
		fragment.addContext(bb, patchMargin);
		Assert.assertEquals("@@ -1,10 +1,4 @@\n-Hello \n Worl\n", fragment.toString());
		Assert.assertEquals("World", after.getText());

	}

	// p =
	// dmp.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
	// dmp.patch_addContext(p, "The quick brown fox jumps over the lazy dog.");
	// assertEquals("patch_addContext: Simple case.",
	// "@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", p.toString());

	@Test
	public void testPatchAddContext_SimpleCase() {
		// spans are 0 based - patches are 1 based
		fragment = new PatchFragment(LongSpan.fromLength(20, 4), LongSpan.fromLength(20, 10));
		fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("jump")));
		fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("somersault")));
		fragment.addContext(Factory.wrap("The quick brown fox jumps over the lazy dog."), patchMargin);
		assertEquals("@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", fragment.toString());
	}

	// p =
	// dmp.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
	// dmp.patch_addContext(p, "The quick brown fox jumps.");
	// assertEquals("patch_addContext: Not enough trailing context.",
	// "@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString());
	@Test
	public void testPatchAddContext_NotEnoughTrailingContext() {
		// spans are 0 based - patches are 1 based
		fragment = new PatchFragment(LongSpan.fromLength(20, 4), LongSpan.fromLength(20, 10));
		fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("jump")));
		fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("somersault")));
		fragment.addContext(Factory.wrap("The quick brown fox jumps."), patchMargin);
		assertEquals("@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", fragment.toString());
	}

	// p = dmp.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
	// dmp.patch_addContext(p, "The quick brown fox jumps.");
	// assertEquals("patch_addContext: Not enough leading context.",
	// "@@ -1,7 +1,8 @@\n Th\n-e\n+at\n qui\n", p.toString());
	@Test
	public void testPatchAddContext_NotEnoughLeadingContext() {
		// spans are 0 based - patches are 1 based
		fragment = new PatchFragment(LongSpan.fromLength(2, 1), LongSpan.fromLength(2, 2));
		fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("e")));
		fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("at")));
		fragment.addContext(Factory.wrap("The quick brown fox jumps."), patchMargin);
		assertEquals("@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n", fragment.toString());
	}

	// p = dmp.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
	// dmp.patch_addContext(p,
	// "The quick brown fox jumps. The quick brown fox crashes.");
	// assertEquals("patch_addContext: Ambiguity.",
	// "@@ -1,27 +1,28 @@\n Th\n-e\n+at\n quick brown fox jumps. \n",
	// p.toString());
	// }

	@Test
	public void testPatchAddContext_Ambiguity() {
		// spans are 0 based - patches are 1 based
		fragment = new PatchFragment(LongSpan.fromLength(2, 1), LongSpan.fromLength(2, 2));
		fragment.add(new DiffFragment(Operation.DELETE, Factory.wrap("e")));
		fragment.add(new DiffFragment(Operation.INSERT, Factory.wrap("at")));
		fragment.addContext(Factory.wrap("The quick brown fox jumps.  The quick brown fox crashes."), patchMargin);
		assertEquals("@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n", fragment.toString());
	}

	@Test
	public void getDiffTest() throws IOException {
		Diff diff = fragment.getDiff();
		assertTrue(diff.isEmpty());

		// should not add small diff
		DiffFragment other = new DiffFragment(Operation.EQUAL, Factory.wrap("Hello"));
		fragment.add(other, patchMargin, bb, 0);
		diff = fragment.getDiff();
		assertTrue(diff.isEmpty());

		other = new DiffFragment(Operation.DELETE, Factory.wrap("Hello "));
		fragment.add(other, patchMargin, bb, 5);
		diff = fragment.getDiff();
		assertEquals(1, diff.getFragments().size());
	}

	@Test
	public void isEmptyTest() throws IOException {
		assertTrue(fragment.isEmpty());
		DiffFragment other = new DiffFragment(Operation.DELETE, Factory.wrap("Hello"));
		fragment.add(other, patchMargin, bb, 0);
		assertFalse(fragment.isEmpty());
	}

}
