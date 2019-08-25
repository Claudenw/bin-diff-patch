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

import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.SpanBuffer;

import org.junit.Before;
import org.junit.Test;
import org.xenei.diffpatch.Diff;
import org.xenei.diffpatch.Operation;
import org.xenei.diffpatch.diff.DiffFragment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DiffTest {

	private Diff.Builder builder;

	@Before
	public void setup() throws IOException {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
		builder = new Diff.Builder().setUnlimitedProcessTime();
	}

	@Test
	public void testSimpleCreateDiff() throws Exception {
		StringBuilder sb = new StringBuilder("Hello World").append("\n")
				.append("The quick brown fox jumped over the lazy dog");
		SpanBuffer first = Factory.wrap(sb);
		SpanBuffer second = Factory.wrap(sb.append("Another String"));

		Diff diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		List<DiffFragment> lst = diff.getFragments();
		assertEquals(2, lst.size());
		assertEquals(first.getText(), lst.get(0).getBuffer().getText());
		assertEquals("Hello World\nThe quick brown fox jumped over the lazy dog", lst.get(0).getText());
		assertEquals("Another String", lst.get(1).getText());
	}

	@Test
	public void testCompleteDiff() throws Exception {
		SpanBuffer first = Factory.wrap("abcdefg");
		SpanBuffer second = Factory.wrap("ABCDEFG");

		Diff diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		List<DiffFragment> lst = diff.getFragments();
		assertEquals(2, lst.size());
		assertEquals("abcdefg", lst.get(0).getText());
		assertEquals("ABCDEFG", lst.get(1).getText());

	}

	@Test
	public void testConstructDiff() throws Exception {
		SpanBuffer first = Factory.wrap("123456789");
		SpanBuffer second = Factory.wrap("12345ss89");
		builder.setUnlimitedProcessTime();

		Diff diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		List<DiffFragment> lst = diff.getFragments();
		assertEquals(4, lst.size());
		assertEquals("12345", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("67", lst.get(1).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ss", lst.get(2).getText());
		assertEquals(Operation.INSERT, lst.get(2).getOperation());
		assertEquals("89", lst.get(3).getText());
		assertEquals(Operation.EQUAL, lst.get(3).getOperation());

		builder.setProcessHours(4);
		diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		lst = diff.getFragments();
		assertEquals(4, lst.size());
		assertEquals("12345", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("67", lst.get(1).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ss", lst.get(2).getText());
		assertEquals(Operation.INSERT, lst.get(2).getOperation());
		assertEquals("89", lst.get(3).getText());
		assertEquals(Operation.EQUAL, lst.get(3).getOperation());

		builder.setSkipDetail();
		diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		lst = diff.getFragments();
		assertEquals(4, lst.size());
		assertEquals("12345", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("67", lst.get(1).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ss", lst.get(2).getText());
		assertEquals(Operation.INSERT, lst.get(2).getOperation());
		assertEquals("89", lst.get(3).getText());
		assertEquals(Operation.EQUAL, lst.get(3).getOperation());
	}

	@Test
	public void halfMatchTest() throws Exception {
		SpanBuffer first = Factory.wrap("Now is the time for all good men to come to the aid of their country");
		SpanBuffer second = Factory.wrap("Then was the time for all good men to have come to the aid of their country");
		builder.setProcessHours(50);
		Diff diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		List<DiffFragment> lst = diff.getFragments();
		assertEquals(8, lst.size());
		assertEquals("Now", lst.get(0).getText());
		assertEquals(Operation.DELETE, lst.get(0).getOperation());
		assertEquals("Then", lst.get(1).getText());
		assertEquals(Operation.INSERT, lst.get(1).getOperation());
		assertEquals(" ", lst.get(2).getText());
		assertEquals(Operation.EQUAL, lst.get(2).getOperation());
		assertEquals("i", lst.get(3).getText());
		assertEquals(Operation.DELETE, lst.get(3).getOperation());
		assertEquals("wa", lst.get(4).getText());
		assertEquals(Operation.INSERT, lst.get(4).getOperation());
		assertEquals("s the time for all good men to", lst.get(5).getText());
		assertEquals(Operation.EQUAL, lst.get(5).getOperation());
		assertEquals(" have", lst.get(6).getText());
		assertEquals(Operation.INSERT, lst.get(6).getOperation());
		assertEquals(" come to the aid of their country", lst.get(7).getText());
		assertEquals(Operation.EQUAL, lst.get(7).getOperation());

	}

//	private void assertBufferEquals( String title, String[] expected, SpanBuffer[] result)
//	{
//		assertNotNull( title+" Result was null", result );
//		assertEquals( title+" Wrong length", expected.length, result.length);
//		for (int i=0;i<expected.length;i++)
//		{
//			assertEquals( title+" Error at item "+i, Factory.wrap(expected[i]), result[i]);
//		}
//	}

	@Test
	public void testCleanupMerge() throws Exception {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("en")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap(" ")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("w")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("i")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("a")));
		diff.cleanupMerge();
		assertEquals(4, diff.getFragments().size());

		Iterator<DiffFragment> iter = diff.getFragments().iterator();
		DiffFragment df = iter.next();
		assertEquals("en", df.getText());
		assertEquals(Operation.INSERT, df.getOperation());

		df = iter.next();
		assertEquals(" ", df.getText());
		assertEquals(Operation.EQUAL, df.getOperation());

		df = iter.next();
		assertEquals("i", df.getText());
		assertEquals(Operation.DELETE, df.getOperation());

		df = iter.next();
		assertEquals("wa", df.getText());
		assertEquals(Operation.INSERT, df.getOperation());
	}

	@Test
	public void testConstructDiffWithOffset() throws Exception {
		SpanBuffer first = Factory.wrap(3, "123456789");
		SpanBuffer second = Factory.wrap("12345ss89");
		Diff diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		List<DiffFragment> lst = diff.getFragments();
		assertEquals(4, lst.size());
		assertEquals(4, lst.size());
		assertEquals("12345", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("67", lst.get(1).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ss", lst.get(2).getText());
		assertEquals(Operation.INSERT, lst.get(2).getOperation());
		assertEquals("89", lst.get(3).getText());
		assertEquals(Operation.EQUAL, lst.get(3).getOperation());

		first = Factory.wrap("123456789");
		second = Factory.wrap(3, "12345ss89");
		diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		lst = diff.getFragments();
		assertEquals(4, lst.size());
		assertEquals("12345", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("67", lst.get(1).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ss", lst.get(2).getText());
		assertEquals(Operation.INSERT, lst.get(2).getOperation());
		assertEquals("89", lst.get(3).getText());
		assertEquals(Operation.EQUAL, lst.get(3).getOperation());

		first = Factory.wrap(5, "123456789");
		second = Factory.wrap(3, "12345ss89");
		diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		lst = diff.getFragments();
		assertEquals(4, lst.size());
		assertEquals("12345", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("67", lst.get(1).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ss", lst.get(2).getText());
		assertEquals(Operation.INSERT, lst.get(2).getOperation());
		assertEquals("89", lst.get(3).getText());
		assertEquals(Operation.EQUAL, lst.get(3).getOperation());

	}

	@Test
	public void testCommonTailDiff() throws Exception {
		SpanBuffer first = Factory.wrap("aaaaag");
		SpanBuffer second = Factory.wrap("bbbbbg");

		Diff diff = builder.build(first, second);

		assertFalse(diff.isEmpty());
		List<DiffFragment> lst = diff.getFragments();
		assertEquals(3, lst.size());
		assertEquals("aaaaa", lst.get(0).getText());
		assertEquals(Operation.DELETE, lst.get(0).getOperation());
		assertEquals("bbbbb", lst.get(1).getText());
		assertEquals(Operation.INSERT, lst.get(1).getOperation());
		assertEquals("g", lst.get(2).getText());
		assertEquals(Operation.EQUAL, lst.get(2).getOperation());

	}

	@Test
	public void addFragmentTest() {
		Diff di = new Diff();
		di.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		assertNotNull(di.getFragments());
		assertEquals(1, di.getFragments().size());
	}

	@Test
	public void addDiffImplTest() {
		// adding an empty is still empty
		Diff di = new Diff();
		Diff di2 = new Diff();
		di.add(di2);
		assertTrue(di.isEmpty());

		di = new Diff();
		di.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		di2 = new Diff();
		di.add(di2);
		assertNotNull(di.getFragments());
		assertEquals(1, di.getFragments().size());

		di = new Diff();
		di2 = new Diff();
		di2.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		di.add(di2);
		assertNotNull(di.getFragments());
		assertEquals(1, di.getFragments().size());

		di = new Diff();
		di.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		di2 = new Diff();
		di2.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		di.add(di2);
		assertNotNull(di.getFragments());
		assertEquals(2, di.getFragments().size());
	}

	@Test
	public void addFirstTest() {
		Diff di = new Diff();
		di.addFirst(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		assertNotNull(di.getFragments());
		assertEquals(1, di.getFragments().size());
		assertEquals(Operation.EQUAL, di.getFragments().get(0).getOperation());

		di = new Diff();
		di.add(new DiffFragment(Operation.DELETE, Factory.EMPTY));
		di.addFirst(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		assertNotNull(di.getFragments());
		assertEquals(2, di.getFragments().size());
		assertEquals(Operation.EQUAL, di.getFragments().get(0).getOperation());

	}

	@Test
	public void addLastTest() {
		Diff di = new Diff();
		di.addLast(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		assertNotNull(di.getFragments());
		assertEquals(1, di.getFragments().size());
		assertEquals(Operation.EQUAL, di.getFragments().get(0).getOperation());

		di = new Diff();
		di.add(new DiffFragment(Operation.DELETE, Factory.EMPTY));
		di.addLast(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		assertNotNull(di.getFragments());
		assertEquals(2, di.getFragments().size());
		assertEquals(Operation.EQUAL, di.getFragments().get(1).getOperation());
	}

	@Test
	public void cleanupMergeTest() throws IOException {
		Diff di = new Diff();
		di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ".getBytes())));
		di.addLast(new DiffFragment(Operation.DELETE, Factory.wrap("men".getBytes())));
		di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("women".getBytes())));
		di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap(" to come to the aid of their country.".getBytes())));
		di.cleanupMerge();
		List<DiffFragment> frags = di.getFragments();
		assertEquals("Now is the time for all good ", frags.get(0).getText());
		assertEquals(Operation.EQUAL, frags.get(0).getOperation());
		assertEquals("wo", frags.get(1).getText());
		assertEquals(Operation.INSERT, frags.get(1).getOperation());
		assertEquals("men to come to the aid of their country.", frags.get(2).getText());
		assertEquals(Operation.EQUAL, frags.get(2).getOperation());
	}

	@Test
	public void cleanupMergeTest_NullCase() throws IOException {
		Diff diff = new Diff();
		diff.cleanupMerge();
		assertTrue("Null case", diff.getFragments().isEmpty());
	}

	@Test
	public void cleanupMergeTest_NoChange() throws IOException {
		Diff diff = new Diff();

		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("b"));
		DiffFragment df3 = new DiffFragment(Operation.INSERT, Factory.wrap("c"));
		diff.add(df1);
		diff.add(df2);
		diff.add(df3);
		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 3, lst.size());
		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
		assertEquals("frag 3", df3, lst.get(2));
	}

//	   
//    diffs = diffList(new Diff(EQUAL, "a"), new Diff(EQUAL, "b"), new Diff(EQUAL, "c"));
//    dmp.diff_cleanupMerge(diffs);
//    assertEquals("diff_cleanupMerge: Merge equalities.", diffList(new Diff(EQUAL, "abc")), diffs);
//
	@Test
	public void cleanupMergeTest_MergeEqualities() throws IOException {
		Diff diff = new Diff();

		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("b"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("c"));
		diff.add(df1);
		diff.add(df2);
		diff.add(df3);
		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 1, lst.size());
		df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("abc"));
		assertEquals("frag 1", df1, lst.get(0));
	}

//diffs = diffList(new Diff(DELETE, "a"), new Diff(DELETE, "b"), new Diff(DELETE, "c"));
//dmp.diff_cleanupMerge(diffs);
//assertEquals("diff_cleanupMerge: Merge deletions.", diffList(new Diff(DELETE, "abc")), diffs);
//

	@Test
	public void cleanupMergeTest_MergeDeletions() throws IOException {
		Diff diff = new Diff();

		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("b"));
		DiffFragment df3 = new DiffFragment(Operation.DELETE, Factory.wrap("c"));
		diff.add(df1);
		diff.add(df2);
		diff.add(df3);
		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 1, lst.size());
		df1 = new DiffFragment(Operation.DELETE, Factory.wrap("abc"));
		assertEquals("frag 1", df1, lst.get(0));
	}

//	    diffs = diffList(new Diff(INSERT, "a"), new Diff(INSERT, "b"), new Diff(INSERT, "c"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Merge insertions.", diffList(new Diff(INSERT, "abc")), diffs);

	@Test
	public void cleanupMergeTest_MergeInsertions() throws IOException {
		Diff diff = new Diff();

		DiffFragment df1 = new DiffFragment(Operation.INSERT, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("b"));
		DiffFragment df3 = new DiffFragment(Operation.INSERT, Factory.wrap("c"));
		diff.add(df1);
		diff.add(df2);
		diff.add(df3);
		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 1, lst.size());
		df1 = new DiffFragment(Operation.INSERT, Factory.wrap("abc"));
		assertEquals("frag 1", df1, lst.get(0));
	}

//	    diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"), new Diff(DELETE, "c"), new Diff(INSERT, "d"), new Diff(EQUAL, "e"), new Diff(EQUAL, "f"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Merge interweave.", diffList(new Diff(DELETE, "ac"), new Diff(INSERT, "bd"), new Diff(EQUAL, "ef")), diffs);

	@Test
	public void cleanupMergeTest_MergeInterweave() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("a")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("b")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("c")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("d")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("e")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("f")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 3, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("ac"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("bd"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("ef"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
		assertEquals("frag 3", df3, lst.get(2));
	}

//
//	    diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Prefix and suffix detection.", diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "d"), new Diff(INSERT, "b"), new Diff(EQUAL, "c")), diffs);
	@Test
	public void cleanupMergeTest_PrefixSuffixDetection() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("a")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("abc")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("dc")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 4, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("d"));
		DiffFragment df3 = new DiffFragment(Operation.INSERT, Factory.wrap("b"));
		DiffFragment df4 = new DiffFragment(Operation.EQUAL, Factory.wrap("c"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
		assertEquals("frag 3", df3, lst.get(2));
		assertEquals("frag 4", df4, lst.get(3));
	}

//
//	    diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"), new Diff(EQUAL, "y"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Prefix and suffix detection with equalities.", diffList(new Diff(EQUAL, "xa"), new Diff(DELETE, "d"), new Diff(INSERT, "b"), new Diff(EQUAL, "cy")), diffs);

	@Test
	public void cleanupMergeTest_PrefixSuffixDetectionWithEqualities() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("x")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("a")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("abc")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("dc")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("y")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 4, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("xa"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("d"));
		DiffFragment df3 = new DiffFragment(Operation.INSERT, Factory.wrap("b"));
		DiffFragment df4 = new DiffFragment(Operation.EQUAL, Factory.wrap("cy"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
		assertEquals("frag 3", df3, lst.get(2));
		assertEquals("frag 4", df4, lst.get(3));
	}

//	    diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "ba"), new Diff(EQUAL, "c"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Slide edit left.", diffList(new Diff(INSERT, "ab"), new Diff(EQUAL, "ac")), diffs);
	@Test
	public void cleanupMergeTest_SlideEditLeft() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("a")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("ba")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("c")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 2, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.INSERT, Factory.wrap("ab"));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("ac"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
	}

//	    diffs = diffList(new Diff(EQUAL, "c"), new Diff(INSERT, "ab"), new Diff(EQUAL, "a"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Slide edit right.", diffList(new Diff(EQUAL, "ca"), new Diff(INSERT, "ba")), diffs);

	@Test
	public void cleanupMergeTest_SlideEditRight() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("c")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("ab")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("a")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 2, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("ca"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("ba"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
	}

//	    diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(EQUAL, "c"), new Diff(DELETE, "ac"), new Diff(EQUAL, "x"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Slide edit left recursive.", diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "acx")), diffs);

	@Test
	public void cleanupMergeTest_SlideEditLeftRecursive() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("a")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("b")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("c")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("ac")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("x")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 2, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("abc"));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("acx"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
	}

//	    diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "ca"), new Diff(EQUAL, "c"), new Diff(DELETE, "b"), new Diff(EQUAL, "a"));
//	    dmp.diff_cleanupMerge(diffs);
//	    assertEquals("diff_cleanupMerge: Slide edit right recursive.", diffList(new Diff(EQUAL, "xca"), new Diff(DELETE, "cba")), diffs);

	@Test
	public void cleanupMergeTest_SlideEditRightRecursive() throws IOException {
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("x")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("ca")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("c")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("b")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("a")));

		diff.cleanupMerge();
		List<DiffFragment> lst = diff.getFragments();
		assertEquals("size", 2, lst.size());

		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("xca"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("cba"));

		assertEquals("frag 1", df1, lst.get(0));
		assertEquals("frag 2", df2, lst.get(1));
	}

	@Test
	public void cleanupMergeTestEndEqual() throws IOException {
		Diff di = new Diff();
		di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ".getBytes())));
		di.cleanupMerge();
		List<DiffFragment> frags = di.getFragments();
		assertEquals(1, frags.size());
		assertEquals("Now is the time for all good ", frags.get(0).getText());
		assertEquals(Operation.EQUAL, frags.get(0).getOperation());
	}

	private void assertEqualBuff(String s, SpanBuffer bb, int offset) throws IOException {
		byte[] b = s.getBytes();
		for (int i = 0; i < b.length; i++) {
			assertEquals(b[i], bb.read(i + offset));
		}

	}

	@Test
	public void extractTest() throws IOException {
		Diff di = new Diff();
		di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("E")));
		di.addLast(new DiffFragment(Operation.DELETE, Factory.wrap("D")));
		di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("I")));
		SpanBuffer bb = di.extract(Operation.EQUAL);
		assertEqualBuff("D", bb, 0);
		assertEqualBuff("I", bb, 1);

		bb = di.extract(Operation.DELETE);
		assertEqualBuff("E", bb, 0);
		assertEqualBuff("I", bb, 1);

		bb = di.extract(Operation.INSERT);
		assertEqualBuff("E", bb, 0);
		assertEqualBuff("D", bb, 1);

	}

	@Test
	public void extractTest2() throws Exception {
		Diff diff = new Diff();

		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("jump")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("s")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("ed")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap(" over ")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("the")));
		diff.add(new DiffFragment(Operation.INSERT, Factory.wrap("a")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap(" lazy")));

		SpanBuffer sb = diff.extract(Operation.DELETE);
		assertEquals("jumped over a lazy", sb.getText());
		sb = diff.extract(Operation.INSERT);
		assertEquals("jumps over the lazy", sb.getText());
	}

	@Test
	public void isEmptyTest() {
		Diff di = new Diff();
		assertTrue(di.isEmpty());
		di.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		assertFalse(di.isEmpty());
	}

	@Test
	public void testDiffNoDifference() throws IOException {
		StringBuilder sb = new StringBuilder("Hello World").append(System.lineSeparator())
				.append("The quick brown fox jumped over the lazy dog");
		SpanBuffer first = Factory.wrap(sb.toString().getBytes());
		SpanBuffer second = Factory.wrap(sb.toString().getBytes());
		Diff diff = builder.build(first, second);
		assertFalse(diff.isEmpty());
		List<DiffFragment> ls = diff.getFragments();
		assertEquals(1, ls.size());
	}

	@Test
	public void constructTest() throws FileNotFoundException, IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("oldData");

		SpanBuffer buffer1 = Factory.wrap(url.openStream());

		url = Thread.currentThread().getContextClassLoader().getResource("newData");

		SpanBuffer buffer2 = Factory.wrap(url.openStream());

		Diff.Builder builder = new Diff.Builder();
		Diff diff = builder.build(buffer1, buffer2);

		List<DiffFragment> lst = diff.getFragments();

		checkDiff(lst, 0, Operation.EQUAL, 48);
		checkDiff(lst, 1, Operation.DELETE, 2);
		checkDiff(lst, 2, Operation.EQUAL, 1);
		checkDiff(lst, 3, Operation.DELETE, 6);
		checkDiff(lst, 4, Operation.EQUAL, 1);
		checkDiff(lst, 5, Operation.DELETE, 2);
		checkDiff(lst, 6, Operation.INSERT, 1);
		checkDiff(lst, 7, Operation.EQUAL, 115);
		checkDiff(lst, 8, Operation.DELETE, 3);
		checkDiff(lst, 9, Operation.INSERT, 3);
		checkDiff(lst, 10, Operation.EQUAL, 1);
		checkDiff(lst, 11, Operation.INSERT, 2);
		checkDiff(lst, 12, Operation.EQUAL, 275);
		checkDiff(lst, 13, Operation.DELETE, 1);
		checkDiff(lst, 14, Operation.INSERT, 1);
		checkDiff(lst, 15, Operation.EQUAL, 1);
		checkDiff(lst, 16, Operation.DELETE, 3);
		checkDiff(lst, 17, Operation.INSERT, 7);
		checkDiff(lst, 18, Operation.EQUAL, 1);
		checkDiff(lst, 19, Operation.DELETE, 2);
		checkDiff(lst, 20, Operation.INSERT, 1);
		checkDiff(lst, 21, Operation.EQUAL, 77);
		checkDiff(lst, 22, Operation.DELETE, 9);
		checkDiff(lst, 23, Operation.EQUAL, 350);
		checkDiff(lst, 24, Operation.DELETE, 417);
		checkDiff(lst, 25, Operation.EQUAL, 273);
	}

	private void checkDiff(List<DiffFragment> dfl, int i, Operation op, long len) {
		assertTrue(dfl.size() > i);
		DiffFragment df = dfl.get(i);
		assertEquals("fragment " + i + " wrong op", op, df.getOperation());
		assertEquals("fragment " + i + " wrong length", len, df.getLength());
	}

	@Test
	public void cleanupMergePhase2EditSurroundedByEqualsTest() throws IOException {

		final List<Boolean> flags = new ArrayList<Boolean>();
		Diff diff = new Diff() {
			@Override
			public void cleanupMerge() {
				flags.add(Boolean.TRUE);
			}
		};

		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("Prev")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("ThisPrev")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("Next")));

		diff.cleanupMergePhase2();

		List<DiffFragment> lst = diff.getFragments();
		assertEquals(Operation.DELETE, lst.get(0).getOperation());
		assertEquals("PrevThis", lst.get(0).getText());
		assertEquals(Operation.EQUAL, lst.get(1).getOperation());
		assertEquals("PrevNext", lst.get(1).getText());
		assertFalse(flags.isEmpty());
	}

	@Test
	public void cleanupMergePhase2EditSurroundedByEqualsTestPt2() throws IOException {

		final List<Boolean> flags = new ArrayList<Boolean>();
		Diff diff = new Diff() {
			@Override
			public void cleanupMerge() {
				flags.add(Boolean.TRUE);
			}
		};

		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("Prev")));
		diff.add(new DiffFragment(Operation.DELETE, Factory.wrap("NextThis")));
		diff.add(new DiffFragment(Operation.EQUAL, Factory.wrap("Next")));

		diff.cleanupMergePhase2();

		List<DiffFragment> lst = diff.getFragments();
		assertEquals(Operation.EQUAL, lst.get(0).getOperation());
		assertEquals("PrevNext", lst.get(0).getText());
		assertEquals(Operation.DELETE, lst.get(1).getOperation());
		assertEquals("ThisNext", lst.get(1).getText());
		assertFalse(flags.isEmpty());
	}

	@Test
	public void testConstructorTrivial() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.EMPTY, Factory.EMPTY);
		assertTrue(diff.getFragments().isEmpty());
	}

	@Test
	public void testConstructorEquality() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("abc"), Factory.wrap("abc"));
		assertEquals(1, diff.getFragments().size());
		assertEquals(new DiffFragment(Operation.EQUAL, Factory.wrap("abc")), diff.getFragments().get(0));
	}

	@Test
	public void testConstructorSimpleInsertion() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("abc"), Factory.wrap("ab123c"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(3, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("ab"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("123"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("c"));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
	}

	@Test
	public void testConstructorSimpleDeletion() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("a123bc"), Factory.wrap("abc"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(3, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("123"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("bc"));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
	}

	@Test
	public void testConstructorTwoInsertions() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("abc"), Factory.wrap("a123b456c"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(5, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("123"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("b"));
		DiffFragment df4 = new DiffFragment(Operation.INSERT, Factory.wrap("456"));
		DiffFragment df5 = new DiffFragment(Operation.EQUAL, Factory.wrap("c"));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
	}

	@Test
	public void testConstructorTwoDeletions() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("a123b456c"), Factory.wrap("abc"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(5, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.DELETE, Factory.wrap("123"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("b"));
		DiffFragment df4 = new DiffFragment(Operation.DELETE, Factory.wrap("456"));
		DiffFragment df5 = new DiffFragment(Operation.EQUAL, Factory.wrap("c"));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
	}

	@Test
	public void testConstructorSimpleCase1() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("a"), Factory.wrap("b"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(2, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("b"));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
	}

	@Test
	public void testConstructorSimpleCase2() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("Apples are a fruit."), Factory.wrap("Bananas are also fruit."));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(5, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("Apple"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("Banana"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("s are a"));
		DiffFragment df4 = new DiffFragment(Operation.INSERT, Factory.wrap("lso"));
		DiffFragment df5 = new DiffFragment(Operation.EQUAL, Factory.wrap(" fruit."));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
	}

	@Test
	public void testConstructorSimpleCase3() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("ax\t"), Factory.wrap("\u0680x\000"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(5, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("a"));
		DiffFragment df2 = new DiffFragment(Operation.INSERT, Factory.wrap("\u0680"));
		DiffFragment df3 = new DiffFragment(Operation.EQUAL, Factory.wrap("x"));
		DiffFragment df4 = new DiffFragment(Operation.DELETE, Factory.wrap("\t"));
		DiffFragment df5 = new DiffFragment(Operation.INSERT, Factory.wrap("\000"));
		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
	}

	@Test
	public void testConstructorOverlap1() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("1ayb2"), Factory.wrap("abxab"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(6, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("1"));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df3 = new DiffFragment(Operation.DELETE, Factory.wrap("y"));
		DiffFragment df4 = new DiffFragment(Operation.EQUAL, Factory.wrap("b"));
		DiffFragment df5 = new DiffFragment(Operation.DELETE, Factory.wrap("2"));
		DiffFragment df6 = new DiffFragment(Operation.INSERT, Factory.wrap("xab"));

		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
		assertEquals(df6, lst.get(5));
	}

	@Test
	public void testConstructorOverlap2() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("abcy"), Factory.wrap("xaxcxabc"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(3, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.INSERT, Factory.wrap("xaxcx"));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("abc"));
		DiffFragment df3 = new DiffFragment(Operation.DELETE, Factory.wrap("y"));

		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
	}

	@Test
	public void testConstructorOverlap3() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("ABCDa=bcd=efghijklmnopqrsEFGHIJKLMNOefg"),
				Factory.wrap("a-bcd-efghijklmnopqrs"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(9, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.DELETE, Factory.wrap("ABCD"));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df3 = new DiffFragment(Operation.DELETE, Factory.wrap("="));
		DiffFragment df4 = new DiffFragment(Operation.INSERT, Factory.wrap("-"));
		DiffFragment df5 = new DiffFragment(Operation.EQUAL, Factory.wrap("bcd"));
		DiffFragment df6 = new DiffFragment(Operation.DELETE, Factory.wrap("="));
		DiffFragment df7 = new DiffFragment(Operation.INSERT, Factory.wrap("-"));
		DiffFragment df8 = new DiffFragment(Operation.EQUAL, Factory.wrap("efghijklmnopqrs"));
		DiffFragment df9 = new DiffFragment(Operation.DELETE, Factory.wrap("EFGHIJKLMNOefg"));

		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
		assertEquals(df6, lst.get(5));
		assertEquals(df7, lst.get(6));
		assertEquals(df8, lst.get(7));
		assertEquals(df9, lst.get(8));
	}

	@Test
	public void testConstructorLargeEquality() throws Exception {
		Diff.Builder builder = new Diff.Builder();

		Diff diff = builder.build(Factory.wrap("a [[Pennsylvania]] and [[New"), Factory.wrap(" and [[Pennsylvania]]"));
		List<DiffFragment> lst = diff.getFragments();

		assertEquals(5, lst.size());
		DiffFragment df1 = new DiffFragment(Operation.INSERT, Factory.wrap(" "));
		DiffFragment df2 = new DiffFragment(Operation.EQUAL, Factory.wrap("a"));
		DiffFragment df3 = new DiffFragment(Operation.INSERT, Factory.wrap("nd"));
		DiffFragment df4 = new DiffFragment(Operation.EQUAL, Factory.wrap(" [[Pennsylvania]]"));
		DiffFragment df5 = new DiffFragment(Operation.DELETE, Factory.wrap(" and [[New"));

		assertEquals(df1, lst.get(0));
		assertEquals(df2, lst.get(1));
		assertEquals(df3, lst.get(2));
		assertEquals(df4, lst.get(3));
		assertEquals(df5, lst.get(4));
	}

	@Test
	public void testConstructorTestTimeout() throws Exception {

		Diff.Builder builder = new Diff.Builder().setProcessTime(100);
		SpanBuffer a = Factory.wrap(
				"`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n");
		SpanBuffer b = Factory.wrap(
				"I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n");

		// Increase the text lengths by 1024 times to ensure a timeout.
		for (int x = 0; x < 10; x++) {
			a = Factory.merge(a, a);
			b = Factory.merge(b, b);
		}
		long startTime = System.currentTimeMillis();
		Diff diff = builder.build(a, b);
		long endTime = System.currentTimeMillis();
		// Test that we took at least the timeout period.
		assertTrue("diff_main: Timeout min.", 100 <= endTime - startTime);

		assertEquals(3, diff.getFragments().size());
	}

//
//    // Test the linemode speedup.
//    // Must be long to pass the 100 char cutoff.
//    a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
//    b = "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n";
//    assertEquals("diff_main: Simple line-mode.", dmp.diff_main(a, b, true), dmp.diff_main(a, b, false));
//
//    a = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
//    b = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
//    assertEquals("diff_main: Single line-mode.", dmp.diff_main(a, b, true), dmp.diff_main(a, b, false));
//
//    a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
//    b = "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n";
//    String[] texts_linemode = diff_rebuildtexts(dmp.diff_main(a, b, true));
//    String[] texts_textmode = diff_rebuildtexts(dmp.diff_main(a, b, false));
//    assertArrayEquals("diff_main: Overlap line-mode.", texts_textmode, texts_linemode);
//
//    // Test null inputs.
//    try {
//      dmp.diff_main(null, null);
//      fail("diff_main: Null inputs.");
//    } catch (IllegalArgumentException ex) {
//      // Error expected.
//    }
//  }

}
