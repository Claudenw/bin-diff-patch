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

import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.diffpatch.patch.PatchFragment;
import org.xenei.span.LongSpan;
import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.SpanBuffer;
import org.junit.Test;
import org.xenei.diffpatch.Patch.ApplyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PatchTest {
    // positions are 1 based in the patch string
    private static final String TEST_STRING = "@@ -22,16 +22,18 @@\n ll good \n+wo\n men to c\n";
    private int leftStart = 21;
    private int leftLength = 16;
    private int rightStart = 21;
    private int rightLength = 18;
    private SpanBuffer paddingBuffer;

    public PatchTest() {
        byte[] b = new byte[4];
        b[0] = 1;
        b[1] = 2;
        b[2] = 3;
        b[3] = 4;
        paddingBuffer = Factory.wrap(b);
    }

    private SpanBuffer middle = Factory.wrap("Knowing that the time had come for her to leave this world, where she "
            + "had been within such a short space of time a wife, a mother, and a "
            + "widow, she went to her room, where slept her son George, guarded by "
            + "waiting women.  He was three years old; his long eyelashes threw a "
            + "pretty shade on his cheeks, and his mouth was like a flower.  Seeing how "
            + "small he was and how young, she began to cry.");

    private SpanBuffer left = Factory.wrap("This eBook is for the use of anyone anywhere in the United States and most"
            + " other parts of the world at no cost and with almost no restrictions "
            + "whatsoever.  You may copy it, give it away or re-use it under the terms of"
            + " the Project Gutenberg License included with this eBook or online at "
            + "www.gutenberg.org.  If you are not located in the United States, you'll have "
            + "to check the laws of the country where you are located before using this ebook.");

    private SpanBuffer right = Factory.wrap("'Legal Entity' shall mean the union of the acting entity and all "
            + "other entities that control, are controlled by, or are under common "
            + "control with that entity. For the purposes of this definition, "
            + "'control' means (i) the power, direct or indirect, to cause the "
            + "direction or management of such entity, whether by contract or "
            + "otherwise, or (ii) ownership of fifty percent (50%) or more of the "
            + "outstanding shares, or (iii) beneficial ownership of such entity.");

    @Test
    public void makePatchTest() throws IOException {
        Diff di = new Diff();
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men to come to the aid of their country.")));
        Patch p = new Patch(di);
        assertEquals(1, p.getSize());
        Patch.Fragment fragment = p.getFragments().findFirst().get();
        assertEquals(leftStart, fragment.getLeftSpan().getOffset());
        assertEquals(leftLength, fragment.getLeftSpan().getLength());
        assertEquals(rightStart, fragment.getRightSpan().getOffset());
        assertEquals(rightLength, fragment.getRightSpan().getLength());

    }

    @Test
    public void patchOutputTest() throws IOException {
        Diff di = new Diff();
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men to come to the aid of their country.")));
        Patch p = new Patch(di);

        assertEquals(TEST_STRING, p.toString());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.write(baos);
        assertArrayEquals(TEST_STRING.getBytes(), baos.toByteArray());

    }

    @Test
    public void reverseTest_Simple() throws IOException {
        Diff di = new Diff();
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men to come to the aid of their country.")));
        Patch p = new Patch(di);
        Patch p2 = p.reverse();
        assertEquals(1, p2.getSize());
        Patch.Fragment fragment = p2.getFragments().findFirst().get();
        assertEquals(rightStart, fragment.getLeftSpan().getOffset());
        assertEquals(rightLength, fragment.getLeftSpan().getLength());
        assertEquals(leftStart, fragment.getRightSpan().getOffset());
        assertEquals(leftLength, fragment.getRightSpan().getLength());

    }

    @Test
    public void reverseTest_ShorterResult() throws IOException {
        Diff di = new Diff();
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the ")));
        di.addLast(new DiffFragment(Operation.DELETE, Factory.wrap("mo")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("ti")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("me")));
        di.addLast(new DiffFragment(Operation.DELETE, Factory.wrap("nt")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap(" for all good ")));
        di.addLast(new DiffFragment(Operation.DELETE, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men\nto come to the aid of their country.")));
        Patch p = new Patch(di);
        Patch p2 = p.reverse();
        assertEquals(2, p2.getSize());
        Iterator<PatchFragment> iter = p2.getFragments().iterator();
        Patch.Fragment fragment = iter.next();
        assertEquals(8 - 1, fragment.getLeftSpan().getOffset());
        assertEquals(12, fragment.getLeftSpan().getLength());
        assertEquals(8 - 1, fragment.getRightSpan().getOffset());
        assertEquals(14, fragment.getRightSpan().getLength());

        fragment = iter.next();
        assertEquals(28 - 1, fragment.getLeftSpan().getOffset());
        assertEquals(8, fragment.getLeftSpan().getLength());
        assertEquals(28 - 1, fragment.getRightSpan().getOffset());
        assertEquals(10, fragment.getRightSpan().getLength());

    }

    @Test
    public void reverseTest_LongerResult() throws IOException {
        Diff di = new Diff();
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the ")));
        di.addLast(new DiffFragment(Operation.DELETE, Factory.wrap("ti")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("mo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("me")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("nt")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap(" for all good ")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men\nto come to the aid of their country.")));
        Patch p = new Patch(di);
        Patch p2 = p.reverse();
        assertEquals(2, p2.getSize());
        Iterator<PatchFragment> iter = p2.getFragments().iterator();
        Patch.Fragment fragment = iter.next();
        assertEquals(8 - 1, fragment.getLeftSpan().getOffset());
        assertEquals(14, fragment.getLeftSpan().getLength());
        assertEquals(8 - 1, fragment.getRightSpan().getOffset());
        assertEquals(12, fragment.getRightSpan().getLength());

        fragment = iter.next();
        assertEquals(22 - 1, fragment.getLeftSpan().getOffset());
        assertEquals(18, fragment.getLeftSpan().getLength());
        assertEquals(22 - 1, fragment.getRightSpan().getOffset());
        assertEquals(16, fragment.getRightSpan().getLength());

    }

    @Test
    public void makeReadPatchTest() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_STRING.getBytes());

        Patch p2 = new Patch(bais);

        assertEquals(1, p2.getSize());
        Patch.Fragment fragment = p2.getFragments().findFirst().get();
        assertEquals(leftStart, fragment.getLeftSpan().getOffset());
        assertEquals(leftLength, fragment.getLeftSpan().getLength());
        assertEquals(rightStart, fragment.getRightSpan().getOffset());
        assertEquals(rightLength, fragment.getRightSpan().getLength());

        assertEquals(TEST_STRING, p2.toString());

    }

    @Test
    public void applyTest() throws IOException {
        String str1 = "Now is the time for all good men to come to the aid of their country";
        String str2 = "Now is the time for all good women to come to the aid of their country";
        Diff di = new Diff();

        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men to come to the aid of their country.")));
        Patch p = new Patch(di);
        SpanBuffer buff = Factory.wrap(str1);
        ApplyResult result = p.apply(buff);
        String strR = result.getResult().getText();
        assertEquals(str2, strR);
    }

    @Test
    public void applyImperfectTest() throws IOException {
        String str1 = "Now is the time for all good mens to come to the aid of their country";
        String str2 = "Now is the time for all good womens to come to the aid of their country";
        Diff di = new Diff();

        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("Now is the time for all good ")));
        di.addLast(new DiffFragment(Operation.INSERT, Factory.wrap("wo")));
        di.addLast(new DiffFragment(Operation.EQUAL, Factory.wrap("men to come to the aid of their country.")));
        Patch p = new Patch(di);
        SpanBuffer buff = Factory.wrap(str1);
        ApplyResult result = p.apply(buff);
        String strR = result.getResult().getText();
        assertEquals(str2, strR);
    }

    @Test
    public void getLeftSpanTest() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_STRING.getBytes());

        Patch p2 = new Patch(bais);

        Iterator<LongSpan> spans = p2.getLeftSpans();
        assertTrue(spans.hasNext());
        LongSpan s = spans.next();
        assertEquals(leftStart, s.getOffset());
        assertEquals(leftLength, s.getLength());

    }

    @Test
    public void getRightSpanTest() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_STRING.getBytes());

        Patch p2 = new Patch(bais);
        Iterator<LongSpan> spans = p2.getRightSpans();
        assertTrue(spans.hasNext());
        LongSpan s = spans.next();
        assertEquals(rightStart, s.getOffset());
        assertEquals(rightLength, s.getLength());

    }

    @Test
    public void applyBigDeleteBeforeBigInsert() throws IOException {
        // Testing maxSplit

        PatchFragment frag = new PatchFragment(LongSpan.fromLength(0, left.getLength()),
                LongSpan.fromLength(0, right.getLength()));
        frag.add(new DiffFragment(Operation.DELETE, left));
        frag.add(new DiffFragment(Operation.INSERT, right));

        Patch p = new Patch();
        p.addFragment(frag);
        ApplyResult result = p.apply(left);
        assertEquals(right.getText(), result.getResult().getText());

    }

    @Test
    public void applyBigEqualsInsert() throws IOException {
        // Testing maxSplit

        PatchFragment frag = new PatchFragment(LongSpan.fromLength(0, left.getLength()),
                LongSpan.fromLength(0, right.getLength()));
        frag.add(new DiffFragment(Operation.EQUAL, left));
        frag.add(new DiffFragment(Operation.INSERT, right));

        Patch p = new Patch();
        p.addFragment(frag);
        ApplyResult result = p.apply(left);
        assertEquals(left.concat(right).getText(), result.getResult().getText());

    }

    @Test
    public void applyBigInsertEqual() throws IOException {
        // Testing maxSplit

        PatchFragment frag = new PatchFragment(LongSpan.fromLength(0, left.getLength()),
                LongSpan.fromLength(0, right.getLength()));
        frag.add(new DiffFragment(Operation.INSERT, right));
        frag.add(new DiffFragment(Operation.EQUAL, left));

        Patch p = new Patch();
        p.addFragment(frag);
        ApplyResult result = p.apply(left);
        assertEquals(right.concat(left).getText(), result.getResult().getText());

    }

    @Test
    public void applyBigDeleteMiddleInsert() throws IOException {

        PatchFragment frag = new PatchFragment(LongSpan.fromLength(0, left.getLength()),
                LongSpan.fromLength(0, right.getLength()));
        frag.add(new DiffFragment(Operation.DELETE, left));
        frag.add(new DiffFragment(Operation.EQUAL, middle));
        frag.add(new DiffFragment(Operation.INSERT, right));

        Patch p = new Patch();
        p.addFragment(frag);
        ApplyResult result = p.apply(left.concat(middle));
        assertEquals(middle.getText() + right.getText(), result.getResult().getText());

    }

    @Test
    public void applyBigInsertBeforeBigDelete() throws IOException {
        // Testing maxSplit

        PatchFragment frag = new PatchFragment(LongSpan.fromLength(0, left.getLength()),
                LongSpan.fromLength(0, right.getLength()));
        frag.add(new DiffFragment(Operation.INSERT, right));
        frag.add(new DiffFragment(Operation.DELETE, left));

        Patch p = new Patch();
        p.addFragment(frag);
        ApplyResult result = p.apply(left);
        assertEquals(right.getText(), result.getResult().getText());
    }

    @Test
    public void applyBigInsertMiddleDelete() throws IOException {
        // Testing maxSplit

        PatchFragment frag = new PatchFragment(LongSpan.fromLength(0, left.getLength()),
                LongSpan.fromLength(0, right.getLength()));
        frag.add(new DiffFragment(Operation.INSERT, right));
        frag.add(new DiffFragment(Operation.EQUAL, middle));
        frag.add(new DiffFragment(Operation.DELETE, left));

        Patch p = new Patch();
        p.addFragment(frag);
        ApplyResult result = p.apply(middle.concat(left));
        assertEquals(right.concat(middle).getText(), result.getResult().getText());
    }

    @Test
    public void constructBigEquals() throws IOException {

        Diff diff = new Diff();
        diff.add(new DiffFragment(Operation.INSERT, right));
        diff.add(new DiffFragment(Operation.EQUAL, middle));
        diff.add(new DiffFragment(Operation.DELETE, left));

        Patch p = new Patch(diff);
        // these need to be one short
        LongSpan[] left = { LongSpan.fromLength(0, 8), LongSpan.fromLength(841, 446) };
        LongSpan[] right = { LongSpan.fromLength(0, 463), LongSpan.fromLength(841, 4) };
        Iterator<PatchFragment> iter = p.getFragments().iterator();
        int i = 0;
        while (iter.hasNext()) {
            assertTrue(i < 2);
            PatchFragment pf = iter.next();
            assertTrue(String.format("left span out at fragment %s, %s != %s", i, left[i], pf.getLeftSpan()),
                    isEq(left[i], pf.getLeftSpan()));
            assertTrue(String.format("right span out at fragment %s, %s != %s", i, right[i], pf.getLeftSpan()),
                    isEq(right[i], pf.getRightSpan()));
            i++;
        }
        assertEquals("did not read all fragments", 2, i);

    }

    private boolean isEq(LongSpan a, LongSpan b) {
        return a.getOffset() == b.getOffset() && a.getLength() == b.getLength();
    }

    @Test
    public void splitMaxTest() throws IOException {

        PatchFragment pf = new PatchFragment(LongSpan.fromLength(516, 65), LongSpan.fromLength(516, 8));
        pf.add(new DiffFragment(Operation.EQUAL, Factory.wrap(".bin")));
        pf.add(new DiffFragment(Operation.DELETE,
                Factory.wrap("xboot system flash c2800nm-advipservicesk9-mz.124-32a.bin")));
        pf.add(new DiffFragment(Operation.EQUAL, Factory.wrap("xxAbo")));

        LinkedList<PatchFragment> frags = new LinkedList<PatchFragment>();
        frags.add(pf);

        Patch p = new Patch();

        p.splitMax(frags, 4);
        assertEquals(3, frags.size());

        pf = frags.get(0);
        assertEquals(516, pf.getLeftSpan().getOffset());
        assertEquals(32, pf.getLeftSpan().getLength());
        assertEquals(516, pf.getRightSpan().getOffset());
        assertEquals(8, pf.getRightSpan().getLength());
        checkPatchFragment(pf, ".bin", "xboot system flash c2800", "nm-a");

        pf = frags.get(1);
        assertEquals(540, pf.getLeftSpan().getOffset());
        assertEquals(32, pf.getLeftSpan().getLength());
        assertEquals(516, pf.getRightSpan().getOffset());
        assertEquals(8, pf.getRightSpan().getLength());
        checkPatchFragment(pf, ".bin", "nm-advipservicesk9-mz.12", "4-32");

        pf = frags.get(2);
        assertEquals(564, pf.getLeftSpan().getOffset());
        assertEquals(18, pf.getLeftSpan().getLength());
        assertEquals(516, pf.getRightSpan().getOffset());
        assertEquals(9, pf.getRightSpan().getLength());
        checkPatchFragment(pf, ".bin", "4-32a.bin", "xxAbo");
    }

    private void checkPatchFragment(PatchFragment pf, String s1, String s2, String s3) throws IOException {
        List<DiffFragment> dFrags = pf.getFragments();
        assertEquals(3, dFrags.size());
        DiffFragment df = dFrags.get(0);
        assertEquals(Operation.EQUAL, df.getOperation());
        assertEquals(s1, df.getText());
        df = dFrags.get(1);
        assertEquals(Operation.DELETE, df.getOperation());
        assertEquals(s2, df.getText());
        df = dFrags.get(2);
        assertEquals(Operation.EQUAL, df.getOperation());
        assertEquals(s3, df.getText());

    }

    @Test
    public void constructFromDiffTest() throws FileNotFoundException, IOException {

        URL url = Thread.currentThread().getContextClassLoader().getResource("oldData");

        SpanBuffer buffer1 = Factory.wrap(url.openStream());

        url = Thread.currentThread().getContextClassLoader().getResource("newData");

        SpanBuffer buffer2 = Factory.wrap(url.openStream());

        Diff.Builder builder = new Diff.Builder();
        Diff diff = builder.build(buffer1, buffer2);

        Patch patch = new Patch(diff);

        List<PatchFragment> pfl = patch.getFragments().collect(Collectors.toCollection(ArrayList::new));

        checkFragment(pfl, 0, LongSpan.fromLength(45, 20), LongSpan.fromLength(45, 11));

        checkFragment(pfl, 1, LongSpan.fromLength(163, 12), LongSpan.fromLength(163, 14));

        checkFragment(pfl, 2, LongSpan.fromLength(440, 24), LongSpan.fromLength(440, 27));

        checkFragment(pfl, 3, LongSpan.fromLength(532, 17), LongSpan.fromLength(532, 8));

        checkFragment(pfl, 4, LongSpan.fromLength(882, 425), LongSpan.fromLength(882, 8));
    }

    private void checkFragment(List<PatchFragment> pfl, int idx, LongSpan left,
            LongSpan right/* ,String s1,String s2, String s3 */) throws IOException {
        assertTrue(pfl.size() > idx);
        PatchFragment pf = pfl.get(idx);
        assertEquals("wrong left start at " + idx, left.getOffset() - 1, pf.getLeftSpan().getOffset());
        assertEquals("wrong left length at " + idx, left.getLength(), pf.getLeftSpan().getLength());
        assertEquals("wrong right start at " + idx, right.getOffset() - 1, pf.getRightSpan().getOffset());
        assertEquals("wrong right length at " + idx, right.getLength(), pf.getRightSpan().getLength());

    }

    @Test
    public void constructFromText2() throws IOException {
        Patch patch = makePatch("abcdefghijklmnopqrstuvwxyz--------------------1234567890",
                "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
        Object[] frags = patch.getFragments().toArray();
        assertEquals(2, frags.length);
        assertEquals("@@ -1,11 +1,21 @@\n abc\n+XXXXXXXXXX\n defghijk\n", frags[0].toString());
        assertEquals("@@ -56,11 +56,21 @@\n -1234567\n+YYYYYYYYYY\n 890\n", frags[1].toString());
    }

    @Test
    public void constructFromTextNull() throws IOException {
        Patch p = new Patch(Factory.wrap("").getInputStream());
        assertEquals(0, p.getSize());
    }
    //
    // String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n over \n-the\n+a\n
    // %0Alaz\n";
    // assertEquals("patch_fromText: #1.", strp,
    // dmp.patch_fromText(strp).get(0).toString());

    @Test
    public void constructFromText() throws IOException {
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals("from text #1", strp, new Patch(Factory.wrap(strp).getInputStream()).toString());

        strp = "@@ -1 +1 @@\n-a\n+b\n";
        assertEquals("from text #2", strp, new Patch(Factory.wrap(strp).getInputStream()).toString());

        strp = "@@ -1,3 +0,0 @@\n-abc\n";
        assertEquals("from text #3", strp, new Patch(Factory.wrap(strp).getInputStream()).toString());

        strp = "@@ -0,0 +1,3 @@\n+abc\n";
        assertEquals("from text #4", strp, new Patch(Factory.wrap(strp).getInputStream()).toString());

        // Generates error.
        strp = "Bad\nPatch\n";
        try {
            new Patch(Factory.wrap(strp).getInputStream());
            fail("from text: #5.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }
    }

    //
    // public void testPatchToText() {
    // String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n over \n-the\n+a\n
    // laz\n";
    // List<Patch> patches;
    // patches = dmp.patch_fromText(strp);
    // assertEquals("patch_toText: Single.", strp, dmp.patch_toText(patches));
    //
    // strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n
    // tes\n";
    // patches = dmp.patch_fromText(strp);
    // assertEquals("patch_toText: Dual.", strp, dmp.patch_toText(patches));
    // }
    //
    // @Test
    // public void testPatchAddContext() {
    // String strp = "@@ -21,4 +21,10 @@\n-jump\n+somersault\n";
    // Patch p = new Patch( Factory.wrap(strp).getInputStream() );
    //
    // dmp.Patch_Margin = 4;
    // Patch p;
    // p = dmp.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
    // dmp.patch_addContext(p, "The quick brown fox jumps over the lazy dog.");
    // assertEquals("patch_addContext: Simple case.", "@@ -17,12 +17,18 @@\n fox
    // \n-jump\n+somersault\n s ov\n", p.toString());
    //
    // p = dmp.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
    // dmp.patch_addContext(p, "The quick brown fox jumps.");
    // assertEquals("patch_addContext: Not enough trailing context.", "@@ -17,10
    // +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString());
    //
    // p = dmp.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
    // dmp.patch_addContext(p, "The quick brown fox jumps.");
    // assertEquals("patch_addContext: Not enough leading context.", "@@ -1,7 +1,8
    // @@\n Th\n-e\n+at\n qui\n", p.toString());
    //
    // p = dmp.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
    // dmp.patch_addContext(p, "The quick brown fox jumps. The quick brown fox
    // crashes.");
    // assertEquals("patch_addContext: Ambiguity.", "@@ -1,27 +1,28 @@\n
    // Th\n-e\n+at\n quick brown fox jumps. \n", p.toString());
    // }
    //
    // @SuppressWarnings("deprecation")
    // public void testPatchMake() {
    // LinkedList<Patch> patches;
    // patches = dmp.patch_make("", "");
    // assertEquals("patch_make: Null case.", "", dmp.patch_toText(patches));
    //
    // String text1 = "The quick brown fox jumps over the lazy dog.";
    // String text2 = "That quick brown fox jumped over a lazy dog.";
    // String expectedPatch = "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n qui\n@@ -21,17 +21,18
    // @@\n jump\n-ed\n+s\n over \n-a\n+the\n laz\n";
    // // The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to
    // rolling context.
    // patches = dmp.patch_make(text2, text1);
    // assertEquals("patch_make: Text2+Text1 inputs.", expectedPatch,
    // dmp.patch_toText(patches));
    //
    // expectedPatch = "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n quick b\n@@ -22,18 +22,17
    // @@\n jump\n-s\n+ed\n over \n-the\n+a\n laz\n";
    // patches = dmp.patch_make(text1, text2);
    // assertEquals("patch_make: Text1+Text2 inputs.", expectedPatch,
    // dmp.patch_toText(patches));
    //
    // LinkedList<Diff> diffs = dmp.diff_main(text1, text2, false);
    // patches = dmp.patch_make(diffs);
    // assertEquals("patch_make: Diff input.", expectedPatch,
    // dmp.patch_toText(patches));
    //
    // patches = dmp.patch_make(text1, diffs);
    // assertEquals("patch_make: Text1+Diff inputs.", expectedPatch,
    // dmp.patch_toText(patches));
    //
    // patches = dmp.patch_make(text1, text2, diffs);
    // assertEquals("patch_make: Text1+Text2+Diff inputs (deprecated).",
    // expectedPatch, dmp.patch_toText(patches));
    //
    // patches = dmp.patch_make("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?");
    // assertEquals("patch_toText: Character encoding.", "@@ -1,21 +1,21
    // @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n",
    // dmp.patch_toText(patches));
    //
    // diffs = diffList(new Diff(DELETE, "`1234567890-=[]\\;',./"), new Diff(INSERT,
    // "~!@#$%^&*()_+{}|:\"<>?"));
    // assertEquals("patch_fromText: Character decoding.", diffs,
    // dmp.patch_fromText("@@ -1,21 +1,21
    // @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n").get(0).diffs);
    //
    // text1 = "";
    // for (int x = 0; x < 100; x++) {
    // text1 += "abcdef";
    // }
    // text2 = text1 + "123";
    // expectedPatch = "@@ -573,28 +573,31 @@\n
    // cdefabcdefabcdefabcdefabcdef\n+123\n";
    // patches = dmp.patch_make(text1, text2);
    // assertEquals("patch_make: Long string with repeats.", expectedPatch,
    // dmp.patch_toText(patches));
    //
    // // Test null inputs.
    // try {
    // dmp.patch_make(null);
    // fail("patch_make: Null inputs.");
    // } catch (IllegalArgumentException ex) {
    // // Error expected.
    // }
    // }
    //
    private Patch makePatch(String one, String two) throws IOException {
        Diff diff = new Diff.Builder().build(Factory.wrap(one), Factory.wrap(two));
        return new Patch(diff);
    }

    private LinkedList<PatchFragment> makeFragments(String one, String two) throws IOException {
        Patch patch = makePatch(one, two);
        LinkedList<PatchFragment> frags = new LinkedList<PatchFragment>();
        for (Object frag : patch.getFragments().toArray()) {
            frags.add((PatchFragment) frag);
        }
        return frags;
    }

    @Test
    public void testPatchSplitMax1() throws Exception {
        Patch patch = new Patch();
        LinkedList<PatchFragment> frags = makeFragments("abcdefghijklmnopqrstuvwxyz01234567890",
                "XabXcdXefXghXijXklXmnXopXqrXstXuvXwxXyzX01X23X45X67X89X0");
        patch.splitMax(frags, 4);
        assertEquals(2, frags.size());
        assertEquals(
                "@@ -1,32 +1,46 @@\n+X\n ab\n+X\n cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n",
                frags.get(0).toString());
        assertEquals("@@ -25,13 +39,18 @@\n zX01\n+X\n 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n",
                frags.get(1).toString());
    }

    @Test
    public void testPatchSplitMax2() throws Exception {
        Patch patch = new Patch();
        LinkedList<PatchFragment> frags = makeFragments(
                "abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz", "abcdefuvwxyz");
        String result = "@@ -3,78 +3,8 @@\n cdef\n-1234567890123456789012345678901234567890123456789012345678901234567890\n uvwx\n";
        patch.splitMax(frags, 4);
        assertEquals(1, frags.size());
        assertEquals(result, frags.get(0).toString());
    }

    @Test
    public void testPatchSplitMax3() throws Exception {
        Patch patch = new Patch();
        LinkedList<PatchFragment> frags = makeFragments(
                "1234567890123456789012345678901234567890123456789012345678901234567890", "abc");
        patch.splitMax(frags, 4);
        assertEquals(3, frags.size());
        assertEquals("@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n", frags.get(0).toString());
        assertEquals("@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n", frags.get(1).toString());
        assertEquals("@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n", frags.get(2).toString());
    }

    @Test
    public void testPatchSplitMax4() throws Exception {
        Patch patch = new Patch();
        LinkedList<PatchFragment> frags = makeFragments(
                "abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1",
                "abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1");
        patch.splitMax(frags, 4);
        assertEquals(2, frags.size());
        assertEquals("@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n", frags.get(0).toString());
        assertEquals("@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n", frags.get(1).toString());
    }
    //
    // public void testPatchAddPadding() {
    // LinkedList<Patch> patches;
    // patches = dmp.patch_make("", "test");
    // assertEquals("patch_addPadding: Both edges full.", "@@ -0,0 +1,4
    // @@\n+test\n", dmp.patch_toText(patches));
    // dmp.patch_addPadding(patches);
    // assertEquals("patch_addPadding: Both edges full.", "@@ -1,8 +1,12 @@\n
    // %01%02%03%04\n+test\n %01%02%03%04\n", dmp.patch_toText(patches));

    @Test
    public void applyPaddingBothEdgesFull() throws IOException {

        Patch patch = makePatch("", "test");
        assertEquals("@@ -0,0 +1,4 @@\n+test\n", patch.toString());
        LinkedList<PatchFragment> lst = patch.applyPadding(paddingBuffer);
        assertEquals(1, lst.size());
        assertEquals("@@ -1,8 +1,12 @@\n %01%02%03%04\n+test\n %01%02%03%04\n", lst.get(0).toString());
    }

    //
    // patches = dmp.patch_make("XY", "XtestY");
    // assertEquals("patch_addPadding: Both edges partial.", "@@ -1,2 +1,6 @@\n
    // X\n+test\n Y\n", dmp.patch_toText(patches));
    // dmp.patch_addPadding(patches);
    // assertEquals("patch_addPadding: Both edges partial.", "@@ -2,8 +2,12 @@\n
    // %02%03%04X\n+test\n Y%01%02%03\n", dmp.patch_toText(patches));
    @Test
    public void applyPaddingBothEdgesPartial() throws IOException {

        Patch patch = makePatch("XY", "XtestY");
        assertEquals("@@ -1,2 +1,6 @@\n X\n+test\n Y\n", patch.toString());
        LinkedList<PatchFragment> lst = patch.applyPadding(paddingBuffer);
        assertEquals(1, lst.size());
        assertEquals("@@ -2,8 +2,12 @@\n %02%03%04X\n+test\n Y%01%02%03\n", lst.get(0).toString());
    }
    //
    // patches = dmp.patch_make("XXXXYYYY", "XXXXtestYYYY");
    // assertEquals("patch_addPadding: Both edges none.", "@@ -1,8 +1,12 @@\n
    // XXXX\n+test\n YYYY\n", dmp.patch_toText(patches));
    // dmp.patch_addPadding(patches);
    // assertEquals("patch_addPadding: Both edges none.", "@@ -5,8 +5,12 @@\n
    // XXXX\n+test\n YYYY\n", dmp.patch_toText(patches));
    // }

    @Test
    public void applyPaddingBothEdgesNone() throws IOException {

        Patch patch = makePatch("XXXXYYYY", "XXXXtestYYYY");
        assertEquals("@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n", patch.toString());
        LinkedList<PatchFragment> lst = patch.applyPadding(paddingBuffer);
        assertEquals(1, lst.size());
        assertEquals("@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n", lst.get(0).toString());
    }

    //
    // public void testPatchApply() {
    // dmp.Match_Distance = 1000;
    // dmp.Match_Threshold = 0.5f;
    // dmp.Patch_DeleteThreshold = 0.5f;
    // LinkedList<Patch> patches;
    // patches = dmp.patch_make("", "");
    // Object[] results = dmp.patch_apply(patches, "Hello world.");
    // boolean[] boolArray = (boolean[]) results[1];
    // String resultStr = results[0] + "\t" + boolArray.length;
    // assertEquals("patch_apply: Null case.", "Hello world.\t0", resultStr);

    @Test
    public void applyNullCase() throws IOException {
        Patch patch = makePatch("", "");
        SpanBuffer buffer = Factory.wrap("Hello world");
        ApplyResult result = patch.apply(buffer);
        assertEquals(0, result.getUsed().cardinality());
        assertEquals(buffer, result.getResult());
    }
    //
    // patches = dmp.patch_make("The quick brown fox jumps over the lazy dog.",
    // "That quick brown fox jumped over a lazy dog.");
    // results = dmp.patch_apply(patches, "The quick brown fox jumps over the lazy
    // dog.");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Exact match.", "That quick brown fox jumped over a
    // lazy dog.\ttrue\ttrue", resultStr);

    @Test
    public void applyExactMatch() throws IOException {
        Patch patch = makePatch("The quick brown fox jumps over the lazy dog.",
                "That quick brown fox jumped over a lazy dog.");
        ApplyResult result = patch.apply(Factory.wrap("The quick brown fox jumps over the lazy dog."));
        assertEquals(2, result.getUsed().cardinality());
        assertEquals("That quick brown fox jumped over a lazy dog.", result.getResult().getText());
    }

    // results = dmp.patch_apply(patches, "The quick red rabbit jumps over the tired
    // tiger.");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Partial match.", "That quick red rabbit jumped
    // over a tired tiger.\ttrue\ttrue", resultStr);
    @Test
    public void applyPartialMatch() throws IOException {
        Patch patch = makePatch("The quick brown fox jumps over the lazy dog.",
                "That quick brown fox jumped over a lazy dog.");
        ApplyResult result = patch.apply(Factory.wrap("The quick red rabbit jumps over the tired tiger."));
        assertEquals(2, result.getUsed().cardinality());
        assertEquals("That quick red rabbit jumped over a tired tiger.", result.getResult().getText());
    }

    // results = dmp.patch_apply(patches, "I am the very model of a modern major
    // general.");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Failed match.", "I am the very model of a modern
    // major general.\tfalse\tfalse", resultStr);
    @Test
    public void applyFailedMatch() throws IOException {
        Patch patch = makePatch("The quick brown fox jumps over the lazy dog.",
                "That quick brown fox jumped over a lazy dog.");
        ApplyResult result = patch.apply(Factory.wrap("I am the very model of a modern major general."));
        assertEquals(0, result.getUsed().cardinality());
        assertEquals("I am the very model of a modern major general.", result.getResult().getText());
    }

    // patches =
    // dmp.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y",
    // "xabcy");
    // results = dmp.patch_apply(patches,
    // "x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Big delete, small change.", "xabcy\ttrue\ttrue",
    // resultStr);
    @Test
    public void applyBigDeleteSmallChange() throws IOException {
        Patch patch = makePatch("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
        ApplyResult result = patch.apply(
                Factory.wrap("x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y"));
        assertEquals(2, result.getUsed().cardinality());
        assertEquals("xabcy", result.getResult().getText());
    }

    // patches =
    // dmp.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y",
    // "xabcy");
    // results = dmp.patch_apply(patches,
    // "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Big delete, big change 1.",
    // "xabc12345678901234567890---------------++++++++++---------------12345678901234567890y\tfalse\ttrue",
    // resultStr);
    @Test
    public void applyBigDeleteBigChange1() throws IOException {
        Patch patch = makePatch("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
        ApplyResult result = patch.apply(
                Factory.wrap("x12345678901234567890---------------++++++++++---------------12345678901234567890y"));
        assertEquals(1, result.getUsed().cardinality());
        assertFalse(result.getUsed().get(0));
        assertTrue(result.getUsed().get(1));
        assertEquals("xabc12345678901234567890---------------++++++++++---------------12345678901234567890y",
                result.getResult().getText());
    }

    // dmp.Patch_DeleteThreshold = 0.6f;
    // patches =
    // dmp.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y",
    // "xabcy");
    // results = dmp.patch_apply(patches,
    // "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Big delete, big change 2.", "xabcy\ttrue\ttrue",
    // resultStr);
    @Test
    public void applyBigDeleteBigChange2() throws IOException {
        Patch patch = makePatch("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
        patch.setDeleteThreshold(0.6d);
        ApplyResult result = patch.apply(
                Factory.wrap("x12345678901234567890---------------++++++++++---------------12345678901234567890y"));
        assertEquals(2, result.getUsed().cardinality());
        assertEquals("xabcy", result.getResult().getText());
    }

    // dmp.Patch_DeleteThreshold = 0.5f;
    //
    // // Compensate for failed patch.
    // dmp.Match_Threshold = 0.0f;
    // dmp.Match_Distance = 0;
    // patches =
    // dmp.patch_make("abcdefghijklmnopqrstuvwxyz--------------------1234567890",
    // "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
    // results = dmp.patch_apply(patches,
    // "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
    // assertEquals("patch_apply: Compensate for failed patch.",
    // "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue",
    // resultStr);

    @Test
    public void applyCompensateForFailedPatch() throws IOException {
        Patch patch = makePatch("abcdefghijklmnopqrstuvwxyz--------------------1234567890",
                "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
        patch.setMatchThreshold(0.0);
        patch.setMatchDistance(0);
        ApplyResult result = patch.apply(Factory.wrap("ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890"));
        assertEquals(1, result.getUsed().cardinality());
        assertFalse(result.getUsed().get(0));
        assertTrue(result.getUsed().get(1));
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890",
                result.getResult().getText());
    }

    // dmp.Match_Threshold = 0.5f;
    // dmp.Match_Distance = 1000;
    //
    // patches = dmp.patch_make("", "test");
    // String patchStr = dmp.patch_toText(patches);
    // dmp.patch_apply(patches, "");
    // assertEquals("patch_apply: No side effects.", patchStr,
    // dmp.patch_toText(patches));
    //
    @Test
    public void applyNoSideEffects() throws IOException {
        Patch patch = makePatch("", "test");
        String patchStr = patch.toString();
        patch.apply(Factory.EMPTY);
        assertEquals(patchStr, patch.toString());
    }
    // patches = dmp.patch_make("The quick brown fox jumps over the lazy dog.",
    // "Woof");
    // patchStr = dmp.patch_toText(patches);
    // dmp.patch_apply(patches, "The quick brown fox jumps over the lazy dog.");
    // assertEquals("patch_apply: No side effects with major delete.", patchStr,
    // dmp.patch_toText(patches));

    @Test
    public void applyNoSideEffectsWithMajorDelete() throws IOException {
        Patch patch = makePatch("The quick brown fox jumps over the lazy dog.", "Woof");
        String patchStr = patch.toString();
        patch.apply(Factory.wrap("The quick brown fox jumps over the lazy dog."));
        assertEquals(patchStr, patch.toString());
    }

    // patches = dmp.patch_make("", "test");
    // results = dmp.patch_apply(patches, "");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0];
    // assertEquals("patch_apply: Edge exact match.", "test\ttrue", resultStr);
    @Test
    public void applyEdgeExactMatch() throws IOException {
        Patch patch = makePatch("", "test");
        ApplyResult result = patch.apply(Factory.EMPTY);
        assertEquals(1, result.getUsed().cardinality());
        assertEquals("test", result.getResult().getText());
    }

    // patches = dmp.patch_make("XY", "XtestY");
    // results = dmp.patch_apply(patches, "XY");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0];
    // assertEquals("patch_apply: Near edge exact match.", "XtestY\ttrue",
    // resultStr);
    @Test
    public void applyNearEdgeExactMatch() throws IOException {
        Patch patch = makePatch("XY", "XtestY");
        ApplyResult result = patch.apply(Factory.wrap("XY"));
        assertEquals(1, result.getUsed().cardinality());
        assertEquals("XtestY", result.getResult().getText());
    }

    // patches = dmp.patch_make("y", "y123");
    // results = dmp.patch_apply(patches, "x");
    // boolArray = (boolean[]) results[1];
    // resultStr = results[0] + "\t" + boolArray[0];
    // assertEquals("patch_apply: Edge partial match.", "x123\ttrue", resultStr);
    // }
    @Test
    public void applyEdgePartialMatch() throws IOException {
        Patch patch = makePatch("y", "y123");
        ApplyResult result = patch.apply(Factory.wrap("x"));
        assertEquals(1, result.getUsed().cardinality());
        assertEquals("x123", result.getResult().getText());
    }
    // private void assertArrayEquals(String error_msg, Object[] a, Object[] b) {
    // List<Object> list_a = Arrays.asList(a);
    // List<Object> list_b = Arrays.asList(b);
    // assertEquals(error_msg, list_a, list_b);
    // }
    //
    // private void assertLinesToCharsResultEquals(String error_msg,
    // LinesToCharsResult a, LinesToCharsResult b) {
    // assertEquals(error_msg, a.chars1, b.chars1);
    // assertEquals(error_msg, a.chars2, b.chars2);
    // assertEquals(error_msg, a.lineArray, b.lineArray);
    // }
    //
    // // Construct the two texts which made up the diff originally.
    // private static String[] diff_rebuildtexts(LinkedList<Diff> diffs) {
    // String[] text = {"", ""};
    // for (Diff myDiff : diffs) {
    // if (myDiff.operation != diff_match_patch.Operation.INSERT) {
    // text[0] += myDiff.text;
    // }
    // if (myDiff.operation != diff_match_patch.Operation.DELETE) {
    // text[1] += myDiff.text;
    // }
    // }
    // return text;
    // }
    //
    // // Private function for quickly building lists of diffs.
    // private static LinkedList<Diff> diffList(Diff... diffs) {
    // LinkedList<Diff> myDiffList = new LinkedList<Diff>();
    // for (Diff myDiff : diffs) {
    // myDiffList.add(myDiff);
    // }
    // return myDiffList;
    // }
}