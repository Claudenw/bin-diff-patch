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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.diffpatch.patch.PatchFragment;
import org.xenei.diffpatch.patch.ReverseVisitor;
import org.xenei.span.LongSpan;
import org.xenei.spanbuffer.SpanBuffer;
import org.xenei.spanbuffer.similarity.Bitap;
import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.NoMatchException;

/**
 * Class to represent a patch.
 *
 * A patch is a linked list of Patch.Fragment objects.
 *
 */
public class Patch implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(Patch.class);

    /**
     * The default threshold for delete and match.
     */
    public static final double DEFAULT_DELETE_THRESHOLD = 0.5d;
    /**
     * When deleting a large block of text (over 32 bytes), how close do the
     * contents have to be to match the expected contents. (0.0 = perfection, 1.0 =
     * very loose). Note that match Threshold controls how closely the end points of
     * a delete need to match.
     */
    private double deleteThreshold = Patch.DEFAULT_DELETE_THRESHOLD;

    private Bitap.Config config = new Bitap.Config();

    /**
     * ther serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default Chunk size for context length.
     */
    private static final int DEFAULT_PADDING_LENGTH = 4;

    /* list of fragments that make up the patch */
    private final LinkedList<PatchFragment> fragments;

    /**
     * Constructor. Creates an empty patch.
     */
    public Patch() {
        fragments = new LinkedList<>();
    }

    /**
     * Constructor that reads from input stream.
     * <p>
     * Note that Patches can be serialized to and from object streams.
     * </p>
     *
     * @param in The input stream to read.
     * @throws IOException On error.
     */
    public Patch(final InputStream in) throws IOException {
        this();
        read(in);
    }

    /**
     * Constructor.
     *
     * @param diff The diff to create the patch from.
     * @throws IOException ion IOError
     */
    public Patch(final Diff diff) throws IOException {
        this(Patch.verifyDiff(diff).extract(Operation.INSERT), diff);
    }

    /**
     * Constructor. The patch is created by attempting to apply the Diff changes to
     * the buffer. The default patchMargin is used.
     *
     * @param buffer The buffer to process.
     * @param diff   The diff to process.
     * @throws IOException on error.
     */
    public Patch(final SpanBuffer buffer, final Diff diff) throws IOException {
        this(buffer, diff, Patch.DEFAULT_PADDING_LENGTH);
    }

    /**
     * Constructor. The patch is created by attempting to apply the Diff changes to
     * the buffer and using the specified patchMargin.
     *
     * @param buffer      The buffer to process.
     * @param diff        The diff to process.
     * @param patchMargin the patch margin to use in comparisons.
     * @throws IOException on IO error
     */
    public Patch(final SpanBuffer buffer, final Diff diff, final int patchMargin) throws IOException {
        this();
        if ((buffer == null) || (diff == null)) {
            throw new IllegalArgumentException("Null inputs.");
        }

        if (diff.isEmpty()) {
            return; // Get rid of the null case.
        }

        int bytesRead1 = 0;
        int bytesRead2 = 0;
        /*
         * Start with text1 (prepatch_text) and apply the diffs until we arrive at text2
         * (postpatch_text). We recreate the patches one by one to determine context
         * info.
         */
        SpanBuffer prepatch = buffer;
        SpanBuffer postpatch = buffer;
        PatchFragment patchFragment = null;
        final List<DiffFragment> lst = diff.getFragments();
        final DiffFragment lastDiffFragment = DiffFragment.makeInstance(lst.get(lst.size() - 1));
        for (final Diff.Fragment diffFrag : lst) {
            final DiffFragment diffFragment = DiffFragment.makeInstance(diffFrag);
            if ((patchFragment == null) && (diffFragment.getOperation() != Operation.EQUAL)) {
                // A new patch starts here.
                patchFragment = new PatchFragment(bytesRead1, bytesRead2);
            }

            if (patchFragment != null) {

                // skip equal if last element
                if (!((diffFragment.getOperation() == Operation.EQUAL) && diffFragment.equals(lastDiffFragment))) {
                    postpatch = patchFragment.add(diffFragment, patchMargin, postpatch, bytesRead2);
                }

                if ((diffFragment.getOperation() == Operation.EQUAL)
                        && (diffFragment.getLength() >= (2L * patchMargin))) {
                    // Time for a new patch.
                    if (!patchFragment.isEmpty()) {
                        patchFragment.addContext(prepatch, patchMargin);
                        fragments.add(patchFragment);
                        patchFragment = null;
                        prepatch = postpatch;
                        bytesRead1 = bytesRead2;
                    }

                }
            }

            // Update the current character count.
            if (diffFragment.getOperation() != Operation.INSERT) {
                bytesRead1 += diffFragment.getLength();
            }
            if (diffFragment.getOperation() != Operation.DELETE) {
                bytesRead2 += diffFragment.getLength();
            }
        }
        // Pick up the leftover patch if not empty.
        if ((patchFragment != null) && !patchFragment.isEmpty()) {
            patchFragment.addContext(prepatch, patchMargin);
            fragments.add(patchFragment);
        }
    }

    /**
     * Set the delete threshold for delete patch commands.
     * <p>
     * When deleting a large buffer (over ~64 bytes), how close do the contents have
     * to be to match the expected contents. (0.0 = perfection, 1.0 = very loose).
     * </p>
     * <p>
     * Note that matchThreshold controls how closely the end points of a delete need
     * to match.
     * </p>
     *
     * @param deleteThreshold the new threshold to use.
     */
    public void setDeleteThreshold(final double deleteThreshold) {
        this.deleteThreshold = deleteThreshold;
    }

    /**
     * Set the match configuraiton.
     */
    public void setMatchConfig(Bitap.Config config) {
        this.config = new Bitap.Config(config);
    }

    /**
     * Set the match threshold for applying matches.
     * <p>
     * When determining if an imperfect match is close enough, how close do the
     * contents have to be to match the expected contents. (0.0 = perfection, 1.0 =
     * very loose).
     * </p>
     *
     * @see org.xenei.spanbuffer.MatcherI#DEFAULT_MATCH_THRESHOLD
     *
     * @param matchThreshold the new threshold to use.
     */
    public void setMatchThreshold(final double matchThreshold) {
        this.config = new Bitap.Config(this.config.getDistance(), matchThreshold);
    }

    /**
     * Set the match distance for applying matches.
     * <p>
     * When determining if an imperfect match is close enough, how close do the
     * contents have to be to match the expected contents. (0.0 = perfection, 1.0 =
     * very loose).
     * </p>
     *
     * @see org.xenei.spanbuffer.MatcherI#DEFAULT_MATCH_DISTANCE
     *
     * @param matchDistance the new distance to use.
     */
    public void setMatchDistance(final int matchDistance) {
        this.config = new Bitap.Config(matchDistance, this.config.getThreshold());
    }

    /* package private for testing */
    /* package private */void addFragment(final PatchFragment frag) {
        fragments.add(frag);
    }

    private static Diff verifyDiff(final Diff diff) {
        if (diff == null) {
            throw new IllegalArgumentException(" Diff may not be null.");
        }
        return diff;
    }

    /**
     * Creates a copy of this patch with the Left/Right buffers reversed.
     *
     * @return a new Patch that reuses the same buffers.
     */
    public Patch reverse() {
        final Patch retval = new Patch();

        final ReverseVisitor visitor = new ReverseVisitor();
        for (final PatchFragment pf : fragments) {
            pf.visitWith(visitor);
            retval.fragments.add(visitor.getResult());
        }
        return retval;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Fragment frag : fragments) {
            sb.append(frag);
        }
        return sb.toString();
    }

    /**
     * Return true if the patch is empty. (e.g. getSize() == 0)
     *
     * @return true if the patch is empty.
     */
    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    /**
     * Get the number of fragments in the patch.
     *
     * @return the number of fragments in the patch.
     */
    public int getSize() {
        return fragments.size();
    }

    /**
     * Get an stream on the fragments.
     *
     * @return An stream over the fragments
     */
    public Stream<PatchFragment> getFragments() {
        return fragments.stream();
    }

    /*
     * Method for Serialization to read the patch from an input stream. This is
     * expected to be the normal patch file format.
     *
     * @see http://stackoverflow.com/questions/987372/what-is-the-format-of-a-patch
     * -file
     */
    private void readObject(final ObjectInputStream in) throws ClassNotFoundException, IOException {
        read(in);
    }

    /**
     * Method to read a patch file from an input stream.
     * <p>
     * Note that Patches can be serialized to and from ObjectStreams.
     * </p>
     *
     * @see <a href=
     *      "http://stackoverflow.com/questions/987372/what-is-the-format-of-a-patch-file">stack
     *      overflow</a>
     * @param in The input stream to read the patch from.
     * @throws IOException on IO error.
     */
    public void read(final InputStream in) throws IOException {

        final BufferedReader buffReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        final Pattern patchHeader = Pattern.compile("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@$");
        Matcher matcher;

        String line = buffReader.readLine();

        while (line != null) {
            matcher = patchHeader.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid patch string: " + line);
            }
            final PatchFragment patchFragment = new PatchFragment(getSpan(matcher.group(1), matcher.group(2)),
                    getSpan(matcher.group(3), matcher.group(4)));

            line = parseBody(buffReader, patchFragment);
            fragments.add(patchFragment);
        }
    }

    /**
     * Get all the spans for the left side of the patch.
     *
     * @return the left spans.
     */
    public Iterator<LongSpan> getLeftSpans() {
        return fragments.stream().map(patchFrag -> patchFrag.getLeftSpan()).iterator();
    }

    /**
     * Get all the spans for the right side of the patch.
     *
     * @return the right spans.
     */
    public Iterator<LongSpan> getRightSpans() {
        return fragments.stream().map(patchFrag -> patchFrag.getRightSpan()).iterator();
    }

    private LongSpan getSpan(final String group1, final String group2) {
        final long start = Long.parseLong(group1);
        if (group2.length() == 0) {
            return LongSpan.fromLength(start - 1, 1);
        } else if (group2.equals("0")) {
            return LongSpan.fromLength(start, 0);
        } else {
            return LongSpan.fromLength(start - 1, Long.parseLong(group2));
        }
    }

    private String parseBody(final BufferedReader reader, final PatchFragment patchFragment) throws IOException, Error {
        char sign;
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                sign = line.charAt(0);
                if (sign == '@') {
                    return line;
                }
            } catch (final IndexOutOfBoundsException ex) {
                // Blank line? Whatever.
                continue;
            }
            line = line.substring(1);
            // decode would change all "+" to " "
            line = line.replace("+", "%2B");
            try {
                line = URLDecoder.decode(line, "UTF-8");
            } catch (final UnsupportedEncodingException ex) {
                // Not likely on modern system.
                throw new IllegalStateException("This system does not support UTF-8.", ex);
            } catch (final IllegalArgumentException ex) {
                // Malformed URI sequence.
                throw new IllegalArgumentException("Illegal escape in patch_fromText: " + line, ex);
            }
            final Operation oper = Operation.fromChar(sign);
            final DiffFragment diffFrag = new DiffFragment(oper, Factory.wrap(line));
            patchFragment.add(diffFrag);

        }
        return line;
    }

    /*
     * Method for serialization to write the patch file.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        writeData(out);
    }

    /**
     * Write the patch in standard format to the output stream.
     *
     * @see <a href=
     *      "http://stackoverflow.com/questions/987372/what-is-the-format-of-a-patch-file">stack
     *      overflow</a>
     * @param dout The data output stream
     * @throws IOException on IO error
     */
    public void writeData(final DataOutput dout) throws IOException {
        for (final Fragment frag : fragments) {
            dout.writeBytes(frag.toString());
        }
    }

    /**
     * Write the patch in standard format to the output stream.
     *
     * @see <a href=
     *      "http://stackoverflow.com/questions/987372/what-is-the-format-of-a-patch-file">stack
     *      overflow</a>
     * @param os The output stream
     * @throws IOException on IO error
     */
    public void write(final OutputStream os) throws IOException {
        final DataOutputStream dos = (os instanceof DataOutputStream) ? (DataOutputStream) os
                : new DataOutputStream(os);
        writeData(dos);
    }

    /**
     * Interface that defines a Patch fragment.
     *
     */
    public interface Fragment {
        /**
         * Get the span of the left document that this fragment covers.
         *
         * @return the left document span.
         */
        LongSpan getLeftSpan();

        /**
         * Get the span of the right document that this fragment covers.
         *
         * @return the right document span.
         */
        LongSpan getRightSpan();
    }

    /**
     * Add some padding on text start and end so that edges can match something.
     * Intended to be called only from within patch_apply.
     *
     * @return Fragment list with extra padding
     * @throws IOException on IO error
     */
    /* package private */LinkedList<PatchFragment> applyPadding(final SpanBuffer nullPadding) throws IOException {
        // package private for testing
        final long paddingLength = nullPadding.getLength();

        final LinkedList<PatchFragment> newFrags = new LinkedList<>();
        for (final PatchFragment frag : fragments) {

            final PatchFragment pf = new PatchFragment(frag);
            // Bump all the patches forward.
            LongSpan span = pf.getLeftSpan();
            span = LongSpan.fromLength(span.getOffset() + paddingLength, span.getLength());
            pf.setLeftSpan(span);
            span = pf.getRightSpan();
            span = LongSpan.fromLength(span.getOffset() + paddingLength, span.getLength());
            pf.setRightSpan(span);
            newFrags.add(pf);
        }

        // Add some padding on start of first diff.
        PatchFragment frag = newFrags.get(0);

        if (frag.isEmpty() || (frag.getFirst().getOperation() != Operation.EQUAL)) {
            // Add nullPadding equality.
            frag.addFirst(new DiffFragment(Operation.EQUAL, nullPadding));
            LongSpan span = frag.getLeftSpan();
            span = LongSpan.fromLength(span.getOffset() - paddingLength, span.getLength() + paddingLength);
            frag.setLeftSpan(span);
            span = frag.getRightSpan();
            span = LongSpan.fromLength(span.getOffset() - paddingLength, span.getLength() + paddingLength);
            frag.setRightSpan(span);

        } else if (paddingLength > frag.getFirst().getLength()) {
            // Grow first equality.
            final DiffFragment firstDiff = frag.getFirst();

            final long extraLength = paddingLength - firstDiff.getLength();

            frag.replaceFirst(new DiffFragment(firstDiff.getOperation(),
                    Factory.merge(nullPadding.sliceAt(firstDiff.getLength()), firstDiff.getBuffer())));

            LongSpan span = frag.getLeftSpan();
            span = LongSpan.fromLength(span.getOffset() - extraLength, span.getLength() + extraLength);
            frag.setLeftSpan(span);

            span = frag.getRightSpan();
            span = LongSpan.fromLength(span.getOffset() - extraLength, span.getLength() + extraLength);
            frag.setRightSpan(span);
        }

        // Add some padding on end of last diff.
        frag = newFrags.getLast();

        if (frag.isEmpty() || (frag.getLast().getOperation() != Operation.EQUAL)) {
            // Add nullPadding equality.
            frag.addLast(new DiffFragment(Operation.EQUAL, nullPadding));
            LongSpan span = frag.getLeftSpan();
            span = LongSpan.fromLength(span.getOffset(), span.getLength() + paddingLength);
            frag.setLeftSpan(span);
            span = frag.getRightSpan();
            span = LongSpan.fromLength(span.getOffset(), span.getLength() + paddingLength);
            frag.setRightSpan(span);
        } else if (paddingLength > frag.getLast().getLength()) {
            // Grow last equality.
            final DiffFragment lastDiff = frag.getLast();
            final long extraLength = paddingLength - lastDiff.getLength();
            frag.replaceLast(lastDiff.concat(nullPadding.head(nullPadding.getLength() - lastDiff.getLength())));
            LongSpan span = frag.getLeftSpan();
            span = LongSpan.fromLength(span.getOffset(), span.getLength() + extraLength);
            frag.setLeftSpan(span);
            span = frag.getRightSpan();
            span = LongSpan.fromLength(span.getOffset(), span.getLength() + extraLength);
            frag.setRightSpan(span);
        }

        return newFrags;

    }

    /**
     * Look through the patches and break up any which are longer than the maximum
     * limit of the match algorithm. Intended to be called only from within apply().
     * This method also drops all the equality fragments and returns only those
     * fragments+context necessary to perform the edit.
     *
     * @param frags       The linkedlist of PatchFragments
     * @param patchMargin the patch margin
     * @throws IOException on IO Error
     */
    /* package private */void splitMax(final LinkedList<PatchFragment> frags, int patchMargin) throws IOException {

        if (patchMargin >= Integer.SIZE) {
            Patch.LOG.error(String.format("PatchMargin (%s) must be less than %s", patchMargin, Integer.SIZE));
            Patch.LOG.info("Setting PatchMargin = 31");
            patchMargin = 31;
        }
        final int extraSpace = Integer.SIZE - patchMargin;

        SpanBuffer preContext;
        SpanBuffer postContext;
        PatchFragment patchFrag;
        long leftStart;
        long rightStart;
        // boolean empty;
        Operation diffType;
        SpanBuffer diffText;
        final ListIterator<PatchFragment> pointer = frags.listIterator();
        PatchFragment bigpatch = pointer.hasNext() ? pointer.next() : null;
        while (bigpatch != null) {
            /*
             * This patch algorithm supports a max patch size of 1 byte per bit in an
             * Integer so skip the shorter ones.
             */
            if (bigpatch.getLeftSpan().getLength() <= Integer.SIZE) {
                bigpatch = pointer.hasNext() ? pointer.next() : null;
                continue;
            }
            // Remove the big old patch.
            pointer.remove();

            /*
             * set up pointers to traverse the big patch and break it into smaller patches.
             */
            leftStart = bigpatch.getLeftSpan().getOffset();
            rightStart = bigpatch.getRightSpan().getOffset();
            preContext = Factory.EMPTY;

            while (!bigpatch.isEmpty()) {
                // Create one of several smaller patches.
                // at start precontext length is 0 so this will be the first
                // block

                patchFrag = new PatchFragment(
                        LongSpan.fromLength(leftStart - preContext.getLength(), preContext.getLength()),
                        LongSpan.fromLength(rightStart - preContext.getLength(), preContext.getLength()));

                /*
                 * The empty flags is used to determine when a temporary fragment should be
                 * preserved If set true the temporary fragment is considered empty (as in an
                 * equals statement) and is not preserved at the end of processing. Preserved
                 * fragments are part of the solution. Non solution fragments must be processed
                 * to ensure that proper offsets and such are calculated.
                 */
                boolean empty = true;

                // if there is text in precontext add it to the patch
                if (preContext.getLength() != 0) {
                    patchFrag.add(new DiffFragment(Operation.EQUAL, preContext));
                }

                while (!bigpatch.isEmpty() && (patchFrag.getLeftSpan().getLength() < extraSpace)) {
                    diffType = bigpatch.getFirst().getOperation();
                    diffText = bigpatch.getFirst().getBuffer();
                    if (diffType == Operation.INSERT) {
                        // Insertions are harmless -- just copy into result
                        LongSpan span = patchFrag.getRightSpan();
                        span = LongSpan.fromLength(span.getOffset(), span.getLength() + diffText.getLength());
                        patchFrag.setRightSpan(span);
                        rightStart += diffText.getLength();
                        patchFrag.addLast(bigpatch.removeFragment(0));
                        empty = false;
                    } else if ((diffType == Operation.DELETE) && (patchFrag.getFragments().size() == 1)
                            && (patchFrag.getFirst().getOperation() == Operation.EQUAL)
                            && (diffText.getLength() > (2 * Integer.SIZE))) {
                        // This is a large deletion. Let it pass in one chunk.
                        LongSpan span = patchFrag.getLeftSpan();
                        span = LongSpan.fromLength(span.getOffset(), span.getLength() + diffText.getLength());
                        patchFrag.setLeftSpan(span);
                        leftStart += diffText.getLength();
                        empty = false;
                        patchFrag.add(new DiffFragment(diffType, diffText));
                        bigpatch.removeFragment(0);
                    } else {
                        // Deletion or equality. Only take as much as we can
                        // stomach.
                        final long sliceLength = Math.min(diffText.getLength(),
                                extraSpace - patchFrag.getLeftSpan().getLength());
                        if (sliceLength < 0) {
                            throw new IllegalStateException("Slice length = " + sliceLength);
                        }
                        diffText = diffText.head(sliceLength);
                        LongSpan span = patchFrag.getLeftSpan();
                        span = LongSpan.fromEnd(span.getOffset(), span.getEnd() + diffText.getLength());
                        patchFrag.setLeftSpan(span);
                        leftStart += diffText.getLength();
                        if (diffType == Operation.EQUAL) {
                            // equal just move the pointers
                            span = patchFrag.getRightSpan();
                            span = LongSpan.fromLength(span.getOffset(), span.getLength() + diffText.getLength());
                            patchFrag.setRightSpan(span);
                            rightStart += diffText.getLength();
                        } else {
                            empty = false;
                        }
                        patchFrag.add(new DiffFragment(diffType, diffText));
                        if (SpanBuffer.Utils.equals(diffText, bigpatch.getFirst().getBuffer())) {
                            bigpatch.removeFragment(0);
                        } else {
                            // remove only the data we put in the new patch frag
                            bigpatch.replaceFirst(new DiffFragment(bigpatch.getFirst().getOperation(),
                                    bigpatch.getFirst().cut(diffText.getLength())));
                        }
                    }
                }
                // Compute the head context for the next patch.
                preContext = patchFrag.getRightBuffer();
                preContext = preContext.cut(Math.max(0, preContext.getLength() - patchMargin));

                // Append the end context for this patch.
                final SpanBuffer leftText = bigpatch.getLeftBuffer();
                if (leftText.getLength() > patchMargin) {
                    postContext = leftText.head(patchMargin);
                } else {
                    postContext = leftText;
                }
                if (postContext.getLength() != 0) {
                    LongSpan span = patchFrag.getLeftSpan();
                    span = LongSpan.fromLength(span.getOffset(), span.getLength() + postContext.getLength());
                    patchFrag.setLeftSpan(span);
                    span = patchFrag.getRightSpan();
                    span = LongSpan.fromLength(span.getOffset(), span.getLength() + postContext.getLength());
                    patchFrag.setRightSpan(span);
                    if (!patchFrag.isEmpty() && (patchFrag.getLast().getOperation() == Operation.EQUAL)) {
                        patchFrag.replaceLast(patchFrag.getLast().concat(postContext));
                    } else {
                        patchFrag.add(new DiffFragment(Operation.EQUAL, postContext));
                    }
                }
                if (!empty) {
                    pointer.add(patchFrag);
                }
            }
            bigpatch = pointer.hasNext() ? pointer.next() : null;
        }
    }

    /**
     * Apply the patch to the buffer and create a result.
     *
     * @param buffer The buffer to apply the patch to
     * @return The ApplyResult.
     * @throws IOException on error.
     */
    public ApplyResult apply(final SpanBuffer buffer) throws IOException {
        return apply(buffer, Patch.DEFAULT_PADDING_LENGTH);
    }

    /**
     * Apply a result to the buffer using the specified padding length.
     *
     * @param buffer        The buffer to apply the patch to.
     * @param paddingLength The padding length.
     * @return the ApplyResult
     * @throws IOException on error
     */
    public ApplyResult apply(final SpanBuffer buffer, final int paddingLength) throws IOException {

        if (paddingLength >= Integer.SIZE) {
            throw new IllegalArgumentException(String.format(
                    "Padding length (%s) must not be greater than or equal to %s", paddingLength, Integer.SIZE));
        }
        final BitSet bs = new BitSet(fragments.size());
        if (fragments.isEmpty()) {
            return new ApplyResult(bs, buffer);
        }

        // create the null padding
        final byte[] buff = new byte[paddingLength];
        for (byte b = 0; b < paddingLength; b++) {
            buff[b] = (byte) (b + 1);
        }
        final SpanBuffer nullPadding = Factory.wrap(buff);

        /*
         * We are adding padding to the start and end of the text so make a deep copy of
         * the fragments so we can change their offsets without impacting original
         * patch.
         *
         * We will modify the patched value until all the patches are applied or ignored
         */
        final LinkedList<PatchFragment> fragments = applyPadding(nullPadding);
        SpanBuffer patched = Factory.merge(nullPadding, buffer, nullPadding);
        splitMax(fragments, paddingLength);

        if (Patch.LOG.isDebugEnabled()) {
            for (final PatchFragment frag : fragments) {
                Patch.LOG.debug(frag.toString());
            }
        }

        int fragmentNumber = 0;
        /*
         * delta keeps track of the offset between the expected and actual location of
         * the previous patch. If there are patches expected at positions 10 and 20, but
         * the first patch was found at 12, delta is 2 and the second patch has an
         * effective expected position of 22.
         */
        long delta = 0;

        /**
         * results track which edits were applied.
         */
        final BitSet results = new BitSet(fragments.size());
        Bitap.Result result = null;
        Bitap bitap = new Bitap(config);
        for (final PatchFragment patchFrag : fragments) {
            final long expected_loc = patchFrag.getRightSpan().getOffset() + delta;
            long searchLoc;
            final SpanBuffer left = patchFrag.getLeftBuffer();
            long startLoc;
            long endLoc;
            if (!patched.contains(expected_loc)) {
                throw new IOException("Input to short");
            }
            try {
                if (left.getLength() > Integer.SIZE) {
                    /*
                     * patch_splitMax will only provide an oversized pattern in the case of a
                     * monster delete.
                     */
                    searchLoc = patched.makeAbsolute(expected_loc);
                    result = bitap.execute(patched, left.head(Integer.SIZE), searchLoc);
                    if (result == null) {
                        throw new NoMatchException();
                    }
                    startLoc = result.getAbsIndex();

                    searchLoc = patched.makeAbsolute(expected_loc + left.getLength() - Integer.SIZE);
                    result = bitap.execute(patched, left.tail(Integer.SIZE), searchLoc);
                    if (result == null) {
                        throw new NoMatchException();
                    }
                    endLoc = result.getAbsIndex();
                    if (startLoc >= endLoc) {
                        // Can't find valid trailing context. Drop this patch.
                        startLoc = -1;
                    }

                } else {

                    searchLoc = patched.makeAbsolute(expected_loc);
                    result = bitap.execute(patched, left, searchLoc);
                    if (result != null) {
                        startLoc = result.getAbsIndex();
                        endLoc = -1;
                    } else {

                        // Subtract the delta for this failed patch from subsequent
                        // patches.
                        delta -= patchFrag.getRightSpan().getLength() - patchFrag.getLeftSpan().getLength();
                        throw new NoMatchException();
                    }

                }
                results.set(fragmentNumber);
                delta = startLoc - expected_loc;
                final SpanBuffer patchedCandidate = processMatch(startLoc, endLoc, patched, patchFrag);
                patched = patchedCandidate;
            } catch (final NoMatchException accepted) {
                Patch.LOG.warn("No match found: " + accepted.getMessage());
                // Can't find match. Drop this patch.
                // No match found. :(
                results.clear(fragmentNumber);
            }

            fragmentNumber++;
        }
        // Strip the padding off.
        patched = patched.cut(nullPadding.getLength());
        patched = patched.head(patched.getLength() - nullPadding.getLength());
        return new ApplyResult(results, patched);
    }

    /*
     * processes the match by updating the patched element with the statements from
     * the patch fragment. New Spanbuffer containing updated result is returned.
     */
    private SpanBuffer processMatch(final long start_loc, final long end_loc, final SpanBuffer patched,
            final PatchFragment patchFrag) throws IOException, NoMatchException {
        // Found a match. :)
        final SpanBuffer left = patchFrag.getLeftBuffer();
        SpanBuffer patchedText;
        if (end_loc == -1) {
            patchedText = patched.sliceAt(start_loc).head(Math.min(left.getLength(), patched.getLength() - start_loc));
        } else {
            patchedText = patched.sliceAt(start_loc).trunc(Math.min(end_loc + Integer.SIZE, patched.getEnd() + 1));
        }
        if (left.equals(patchedText)) {
            // Perfect match, just shove the replacement text in.
            return Factory.merge(patched.head(start_loc), patchFrag.getDiff().extract(Operation.DELETE),
                    patched.sliceAt(start_loc + left.getLength()));

        } else {
            Patch.LOG.info(String.format("Imperfect match [%s] [%s]", left.getText(), patchedText.getText()));
            return imperfectMatch(left, patchedText, patched, patchFrag, start_loc);

        }

    }

    /*
     * This is called whenever an exact match is not found. The imperfect match
     * attempts to apply the patchfragment to the existing patched solution by first
     * performing a diff between the left and the patchedText
     */
    private SpanBuffer imperfectMatch(final SpanBuffer left, final SpanBuffer patchedText, SpanBuffer patched,
            final PatchFragment patchFrag, final long start_loc) throws IOException, NoMatchException {
        /*
         * Imperfect match. Run a diff to get a framework of equivalent indices.
         */
        final Diff.Builder builder = new Diff.Builder();
        if (Math.min(left.getLength(), patchedText.getLength()) < FileUtils.ONE_MB) {
            builder.setUnlimitedProcessTime();
        }
        final Diff diffs = builder.build(left, patchedText);
        if ((left.getLength() > Integer.SIZE) && ((diffs.levenshtein() / (float) left.getLength()) > deleteThreshold)) {
            /*
             * The end points match, but the content is unacceptably bad.
             */
            throw new NoMatchException(String.format(
                    "Can not find acceptable close match to patch starting at position %s: match (%s) > threshold (%s)",
                    start_loc, diffs.levenshtein() / (float) left.getLength(), deleteThreshold));
        }
        // diffs.cleanupSemanticLossless();
        int index1 = 0;
        for (final DiffFragment aDiff : patchFrag.getFragments()) {
            if (aDiff.getOperation() != Operation.EQUAL) {
                final long index2 = diffs.getIndex(index1);
                if (aDiff.getOperation() == Operation.INSERT) {
                    // Insertion
                    patched = Factory.merge(patched.head(start_loc + index2), aDiff.getBuffer(),
                            patched.sliceAt(start_loc + index2));
                } else if (aDiff.getOperation() == Operation.DELETE) {
                    // Deletion

                    patched = Factory.merge(patched.head(start_loc + index2),
                            patched.cut(start_loc + diffs.getIndex(index1 + aDiff.getLength())));
                }
                if (Patch.LOG.isDebugEnabled()) {
                    Patch.LOG.debug("Patched text is now: " + patched.getText());
                }
            }
            if (aDiff.getOperation() != Operation.DELETE) {
                index1 += aDiff.getLength();
            }
        }
        Patch.LOG.info("Imperfect match returning " + patched.getText());
        return patched;
    }

    /**
     * The result of applying a patch to a buffer.
     */
    public static class ApplyResult {
        private final BitSet used;
        private final SpanBuffer result;

        private ApplyResult(final BitSet used, final SpanBuffer result) {
            this.used = used;
            this.result = result;
        }

        /**
         * Get a bitset that tracks the patch fragments used in the result.
         *
         * @return A bitset that tracks the patch fragments used in the result.
         */
        public BitSet getUsed() {
            return used;
        }

        /**
         * Get the result of the patch.
         *
         * @return The span buffer that contains the result.
         */
        public SpanBuffer getResult() {
            return result;
        }
    }

    /**
     * An input stream that returns the Patch as a patch file contents.
     *
     */
    public static class PatchInputStream extends InputStream {
        private final Iterator<PatchFragment> iter;
        private ByteArrayInputStream bais;

        /**
         * Constructor.
         *
         * @param patch The patch to read.
         */
        public PatchInputStream(final Patch patch) {
            iter = patch.getFragments().iterator();
            // initialize the inputstream
            nextFragment();
        }

        private void nextFragment() {
            if (iter.hasNext()) {
                final PatchFragment fragment = iter.next();
                bais = new ByteArrayInputStream(fragment.toString().getBytes(StandardCharsets.UTF_8));
            } else {
                bais = null;
            }
        }

        @Override
        public int read() throws IOException {
            if (bais == null) {
                return -1;
            }
            int result = bais.read();
            if (result == -1) {
                nextFragment();
                result = read();
            }
            return result;
        }

    }
}
