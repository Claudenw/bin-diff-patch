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
package org.xenei.diffpatch.patch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.diffpatch.Diff;
import org.xenei.diffpatch.Operation;
import org.xenei.diffpatch.Patch;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.span.LongSpan;
import org.xenei.spanbuffer.SpanBuffer;
import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.NoMatchException;

/**
 * Class representing one patch operation. A patch operation comprises a list of
 * Diff.Fragment objects and two spans across the objects one for each of the
 * two input files that created the patch.
 */
public class PatchFragment implements Patch.Fragment {
    private final LinkedList<DiffFragment> diffFrags;
    private LongSpan left;
    private LongSpan right;
    private static final Logger LOG = LoggerFactory.getLogger(PatchFragment.class);

    /**
     * Create a patch fragment from another patch fragment.
     * <p>
     * This is a deep copy constructor.
     * </p>
     *
     * @param other the other patch to copy from.
     */
    public PatchFragment(final PatchFragment other) {
        diffFrags = new LinkedList<>();
        left = other.left;
        right = other.right;
        diffFrags.addAll(other.diffFrags);
    }

    /**
     * Create a fragment from two positions.
     *
     * @param leftPos  the left position,
     * @param rightPos the right position.
     */
    public PatchFragment(final long leftPos, final long rightPos) {
        /*
         * span is 0 index origin, bytes read is 1 index origin
         */
        this(LongSpan.fromLength(leftPos, 0), LongSpan.fromLength(rightPos, 0));
    }

    /**
     * Create a patch fragment from 2 spans.
     *
     * @param left  the left span.
     * @param right the right span.
     */
    public PatchFragment(final LongSpan left, final LongSpan right) {
        diffFrags = new LinkedList<>();
        this.left = left;
        this.right = right;
    }

    /**
     * Remove the specified diff fragment.
     *
     * @param num the fragment to remove.
     * @return the diff fragment that was removed.
     */
    public DiffFragment removeFragment(final int num) {
        return diffFrags.remove(num);
    }

    @Override
    public LongSpan getLeftSpan() {
        return left;
    }

    /**
     * Get the span buffer that comprises the left buffer. This is the buffer buffer
     * that contains the left input.
     *
     * @return the left buffer.
     * @throws IOException on IO error
     */
    public SpanBuffer getLeftBuffer() throws IOException {
        return getDiff().extract(Operation.INSERT).duplicate(left.getOffset());
    }

    /**
     * Get the span buffer that comprises the right buffer. This is the buffer
     * buffer that contains the right input.
     *
     * @return the right buffer.
     * @throws IOException on IO error
     */
    public SpanBuffer getRightBuffer() throws IOException {
        return getDiff().extract(Operation.DELETE).duplicate(right.getOffset());
    }

    /**
     * Set the left span.
     *
     * @param left the left span for this fragment.
     */
    public void setLeftSpan(final LongSpan left) {
        this.left = left;
    }

    @Override
    public LongSpan getRightSpan() {
        return right;
    }

    /**
     * Set the right span for this fragment.
     *
     * @param right the right span.
     */
    public void setRightSpan(final LongSpan right) {
        this.right = right;
    }

    /**
     * Visit this fragment with a PatchFragmentVisitor.
     *
     * @param visitor the visitor to visit with.
     */
    public void visitWith(final PatchFragmentVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Encode the text for the diff.
     *
     * @param text the text to encode
     * @return the encoded string
     * @throws IOException if the UTF-8 encoder is not available.
     */
    public static String encodeText(final String text) throws IOException {
        return URLEncoder.encode(text, "UTF-8").replace('+', ' ').replace("%21", "!").replace("%7E", "~")
                .replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%3B", ";").replace("%2F", "/")
                .replace("%3F", "?").replace("%3A", ":").replace("%40", "@").replace("%26", "&").replace("%3D", "=")
                .replace("%2B", "+").replace("%24", "$").replace("%2C", ",").replace("%23", "#");

    }

    /**
     * Emmulate GNU diff's format. Header: @@ -382,8 +481,9 @@ Indicies are printed
     * as 1-based, not 0-based.
     *
     * @return The GNU diff string.
     */
    @Override
    public String toString() {

        final StringBuilder text = new StringBuilder();
        text.append("@@ -").append(getCoords(left)).append(" +").append(getCoords(right)).append(" @@\n");
        // Escape the body of the patch with %xx notation.
        try {
            for (final Diff.Fragment diffFrag : diffFrags) {
                text.append(diffFrag.getOperation().getChar()).append(PatchFragment.encodeText(diffFrag.getText()))
                        .append("\n");
            }
        } catch (final UnsupportedEncodingException ex) {
            // Not likely on modern system.
            throw new IllegalStateException("This system does not support UTF-8.", ex);
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to read buffer", ex);
        }

        return text.toString();
    }

    /**
     * Get the diff for the diff fragments this patch contains.
     *
     * @return The Diff object.
     */
    public Diff getDiff() {
        return new Diff(diffFrags);
    }

    /**
     * Return true if this fragment is empty (has not diff fragments).
     *
     * @return true if this fragment is empty.
     */
    public boolean isEmpty() {
        return diffFrags.isEmpty();
    }

    /**
     * Add a diff fragment to this patch.
     * <p>
     * The postPatch buffer contains the patch data thus far. Adding the diff
     * fragment will alter the post patch.
     * </p>
     *
     * @param diffFrag    The diff fragment to add
     * @param patchMargin The patch margin to detect small equality patch
     * @param postPatch   the current post patch state buffer.
     * @param patchLoc    the location within postPatch that the change is to occur
     *                    at.
     * @return the modified post patch.
     * @throws IOException on IO error
     */
    public SpanBuffer add(final DiffFragment diffFrag, final int patchMargin, final SpanBuffer postPatch,
            final int patchLoc) throws IOException {
        SpanBuffer retval = postPatch;
        switch (diffFrag.getOperation()) {
        case INSERT:
            diffFrags.add(diffFrag);
            // insert means the right length is now longer.
            right = LongSpan.fromLength(right.getOffset(), right.getLength() + diffFrag.getLength());
            // insert the diffFragment into post patch at the patchLoc.
            retval = Factory.merge(postPatch.head(patchLoc), diffFrag, postPatch.cut(patchLoc));
            break;
        case DELETE:
            diffFrags.add(diffFrag);
            // delete means left length is now longer.
            left = LongSpan.fromLength(left.getOffset(), left.getLength() + diffFrag.getLength());
            // remove the bytes from the post patch buffer.
            retval = Factory.merge(postPatch.head(patchLoc), postPatch.cut(patchLoc + diffFrag.getLength()));
            break;
        case EQUAL:
            if ((diffFrag.getLength() <= (2L * patchMargin)) && !isEmpty() && (diffFrag != diffFrags.getLast())) {
                // Small equality inside a patch.
                diffFrags.add(diffFrag);
                left = LongSpan.fromLength(left.getOffset(), left.getLength() + diffFrag.getLength());
                right = LongSpan.fromLength(right.getOffset(), right.getLength() + diffFrag.getLength());
            }
            break;
        default:
            throw new IllegalArgumentException("Unexpected operation: " + diffFrag.getOperation());
        }
        if (PatchFragment.LOG.isDebugEnabled()) {
            PatchFragment.LOG.debug(String.format("Post patch %s [%s]", retval, retval.getText()));
        }
        return retval;
    }

    /**
     * Add a diff fragment to this patch fragment. same as addLast()
     *
     * @param diffFrag the diff fragment to add
     */
    public void add(final DiffFragment diffFrag) {
        diffFrags.add(diffFrag);
    }

    /**
     * Get an unmodifiable list of all the diff fragments in this patch fragment.
     *
     * @return the list of fragments in this patch fragment.
     */
    public List<DiffFragment> getFragments() {
        return Collections.unmodifiableList(diffFrags);
    }

    /**
     * Get the first diff fragment from this patch fragment.
     *
     * @return the first diff fragment.
     */
    public DiffFragment getFirst() {
        return diffFrags.getFirst();
    }

    /**
     * Get the last diff fragment from this patch fragment.
     *
     * @return the last diff fragment.
     */
    public DiffFragment getLast() {
        return diffFrags.getLast();
    }

    /**
     * Add a diff fragment as the end of this patch fragment.
     *
     * @param diffFrag the diff fragmetn to add.
     */
    public void addLast(final DiffFragment diffFrag) {
        diffFrags.addLast(diffFrag);
    }

    public void replaceLast(final DiffFragment diffFrag) {
        diffFrags.removeLast();
        diffFrags.addLast(diffFrag);
    }

    /**
     * Insert diff fragment at the start of this patch fragment.
     *
     * @param diffFrag the diff fragmetn to add..
     */
    public void addFirst(final DiffFragment diffFrag) {
        diffFrags.addFirst(diffFrag);
    }

    /**
     * Replace the first fragment with the specified fragment.
     *
     * @param diffFrag the fragment to replace the current first fragment.
     */
    public void replaceFirst(final DiffFragment diffFrag) {
        diffFrags.removeFirst();
        diffFrags.addFirst(diffFrag);
    }

    /**
     * Add the context to this patch.
     * <p>
     * This places margin bytes around the patch from the text provided.
     * </p>
     *
     * @param text   The buffer that has the completed output.
     * @param margin the number of bytes to place around the patch to provide
     *               context.
     */
    public void addContext(final SpanBuffer text, final int margin) {
        if (text.getLength() == 0) {
            return;
        }

        SpanBuffer pattern = text.sliceAt(right.getOffset()).head(left.getLength());
        if (PatchFragment.LOG.isDebugEnabled()) {
            try {
                PatchFragment.LOG.debug(String.format("pattern %s[%s]", pattern, pattern.getText()));
            } catch (final IOException ex) {
                // do nothing
            }
        }
        int padding = 0;

        /*
         * Look for the first and last matches of pattern in text. If two different
         * matches are found, increase the pattern length.
         */
        try {
            while ((text.positionOf(pattern) != text.lastPositionOf(pattern))
                    && (pattern.getLength() < (Integer.SIZE - margin - margin))) {
                padding += margin;
                pattern = text.sliceAt(Math.max(0, right.getOffset() - padding))
                        .trunc(Math.min(text.getLength(), right.getOffset() + left.getLength() + padding));
                if (PatchFragment.LOG.isDebugEnabled()) {
                    try {
                        PatchFragment.LOG.debug(String.format("pattern now %s[%s]", pattern, pattern.getText()));
                    } catch (final IOException ex) {
                        // do nothing
                    }
                }

            }
        } catch (final NoMatchException expected) {
            // do nothing
        }
        // Add one chunk for good luck.
        padding += margin;

        final SpanBuffer prefix = addPrefix(padding, text);

        final SpanBuffer suffix = addSuffix(padding, text);

        // Roll back the start points and extends lengths
        left = LongSpan.fromEnd(left.getOffset() - prefix.getLength(), left.getEnd() + suffix.getLength());
        right = LongSpan.fromEnd(right.getOffset() - prefix.getLength(), right.getEnd() + suffix.getLength());
    }

    /**
     * Prefix this fragment with some text.
     * <p>
     * The text is inserted as a diff fragment operation EQUAL as the first diff
     * fragment in this patch.
     * </p>
     *
     * @param padding the number of bytes to add as a prefix.
     * @param text    the buffer to take the padding bytes from.
     * @return The prefix that was added
     */
    private SpanBuffer addPrefix(final int padding, final SpanBuffer text) {
        // Add the prefix.
        if (PatchFragment.LOG.isDebugEnabled()) {
            try {
                PatchFragment.LOG.debug(String.format("addPrefix( %s, %s[%s]", padding, text, text.getText()));
                PatchFragment.LOG.debug(String.format("right: %s", right));
            } catch (final IOException ex) {
                // do nothing
            }
        }
        /*
         * Remove the padding from the text
         */
        final SpanBuffer prefix = text.sliceAt(Math.max(0, right.getOffset() - padding)).trunc(right.getOffset());
        if (prefix.getLength() != 0) {
            diffFrags.addFirst(new DiffFragment(Operation.EQUAL, prefix));
        }
        if (PatchFragment.LOG.isDebugEnabled()) {
            try {
                PatchFragment.LOG.debug(String.format("returning( %s[%s]", prefix, prefix.getText()));
            } catch (final IOException ex) { // do nothing
            }
        }
        return prefix;
    }

    /**
     * Add a suffix to this fragment.
     * <p>
     * The text is inserted as a diff fragment operation EQUAL as the last diff
     * fragment in this patch.
     * </p>
     * The suffix is extracted from the text at the right.start+left.length
     * position.
     *
     * @param padding the number of bytes to add as a suffix.
     * @param text    the buffer to take the padding bytes from.
     * @return The prefix that was added
     */
    private SpanBuffer addSuffix(final int padding, final SpanBuffer text) {
        if (PatchFragment.LOG.isDebugEnabled()) {
            try {
                PatchFragment.LOG.debug(String.format("addSuffix( %s, %s[%s]", padding, text, text.getText()));
                PatchFragment.LOG.debug(String.format("right: %s", right));
                PatchFragment.LOG.debug(String.format("left: %s", left));
            } catch (final IOException ex) {
                // do nothing
            }
        }
        // Add the suffix.
        SpanBuffer suffix = text.sliceAt(right.getOffset() + left.getLength());
        suffix = suffix.head(Math.min(suffix.getLength(), padding));

        if (suffix.getLength() != 0) {
            diffFrags.addLast(new DiffFragment(Operation.EQUAL, suffix));
        }
        if (PatchFragment.LOG.isDebugEnabled()) {
            try {
                PatchFragment.LOG.debug(String.format("returning( %s[%s]", suffix, suffix.getText()));
            } catch (final IOException ex) { // do nothing
            }
        }
        return suffix;
    }

    /**
     * Get the coordinates of the patch for the patch output.
     *
     * @param span the span to get coordinates for.
     * @return the string with the span coordinates.
     */
    public String getCoords(final LongSpan span) {
        if (span.getLength() == 0) {
            return span.getOffset() + ",0";
        } else if (span.getLength() == 1) {
            return Long.toString(span.getOffset() + 1);
        } else {
            return (span.getOffset() + 1) + "," + span.getLength();
        }
    }
}