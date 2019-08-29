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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.span.LongSpan.ComparatorByLength;
import org.xenei.span.NumberUtils;
import org.xenei.spanbuffer.SpanBuffer;
import org.xenei.spanbuffer.Walker;
import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.NoMatchException;

/**
 * The diff class.
 * <p>
 * In most cases you should use the Builder to create the diff.
 * </p>
 */
public class Diff {
	private final LinkedList<DiffFragment> fragments;
	private static final Logger LOG = LoggerFactory.getLogger(Diff.class);

	/**
	 * Create an empty diff.
	 */
	public Diff() {
		fragments = new LinkedList<>();
	}

	/**
	 * Create a diff from a list of fragments.
	 * 
	 * @param fragments the fragments.
	 */
	public Diff(final List<DiffFragment> fragments) {
		this();
		this.fragments.addAll(fragments);
	}

	/**
	 * Find the differences between two texts. Simplifies the problem by stripping
	 * any common prefix or suffix off the texts before diffing.
	 *
	 * @param oldBuffer Old buffer to be diffed.
	 * @param newBuffer New buffer to be diffed.
	 * @param deadline  Time when the diff should be complete by. Used internally
	 *                  for recursive calls.
	 * @throws IOException on error
	 */
	/* package private */ Diff(SpanBuffer oldBuffer, SpanBuffer newBuffer, final long deadline) throws IOException {
		this();
		// Check for null inputs.
		if ((oldBuffer == null) || (newBuffer == null)) {
			throw new IllegalArgumentException("neither oldBuffer nor newBuffer may be null");
		}

		// Check for equality (speedup).

		if (oldBuffer.equals(newBuffer)) {

			if (oldBuffer.getLength() != 0) {
				add(new DiffFragment(Operation.EQUAL, oldBuffer));
			}
			return;
		}

		// Trim off common prefix (speedup).

		long commonLength = oldBuffer.commonPrefix(newBuffer);
		if (commonLength > 0) {
			final SpanBuffer commonPrefix = oldBuffer.head(commonLength);
			if (Diff.LOG.isDebugEnabled()) {
				Diff.LOG.debug("commonPrefix " + commonPrefix);
			}
			oldBuffer = oldBuffer.cut(commonLength);
			newBuffer = newBuffer.cut(commonLength);
			add(new DiffFragment(Operation.EQUAL, commonPrefix));
		}
		// Trim off common suffix (speedup).
		commonLength = oldBuffer.commonSuffix(newBuffer);
		SpanBuffer commonSuffix = null;
		if (commonLength > 0) {
			commonSuffix = oldBuffer.tail(commonLength);
			oldBuffer = oldBuffer.head(oldBuffer.getLength() - commonLength);
			newBuffer = newBuffer.head(newBuffer.getLength() - commonLength);
		}

		if (Diff.LOG.isDebugEnabled()) {
			Diff.LOG.debug("oldBuffer " + oldBuffer.toString());
			Diff.LOG.debug("newBuffer " + newBuffer.toString());
			Diff.LOG.debug("commonSuffix: " + commonSuffix);
		}
		// Compute the diff on the middle block.
		add(compute(oldBuffer, newBuffer, deadline));

		if (commonSuffix != null) {
			addLast(new DiffFragment(Operation.EQUAL, commonSuffix));
		}

		cleanupMerge();
	}

	/**
	 * Find the differences between two texts. Assumes that the texts do not have
	 * any common prefix or suffix.
	 *
	 * @param buffer1  Old string to be diffed.
	 * @param buffer2  New string to be diffed.
	 * @param deadline Time when the diff should be complete by.
	 * @return Linked List of Diff objects.
	 * @throws IOException on IO Error
	 */
	private Diff compute(final SpanBuffer buffer1, final SpanBuffer buffer2, final long deadline) throws IOException {
		if (Diff.LOG.isDebugEnabled()) {
			Diff.LOG.debug(String.format("compute %s %s --with-- %s %s", buffer1, buffer1.getText(), buffer2,
					buffer2.getText()));
		}
		final Diff diff = new Diff();
		final Walker walker1 = buffer1.getWalker();
		if (!walker1.hasCurrent()) {
			// Just add some text (speedup).
			diff.add(new DiffFragment(Operation.INSERT, buffer2));
			return diff;
		}

		final Walker walker2 = buffer2.getWalker();
		if (!walker2.hasCurrent()) {
			// Just delete some text (speedup).
			diff.add(new DiffFragment(Operation.DELETE, buffer1));
			return diff;
		}

		final SpanBuffer haystack = (SpanBuffer) ComparatorByLength.longest(buffer1, buffer2);
		final SpanBuffer needle = (SpanBuffer) ComparatorByLength.shortest(buffer1, buffer2);
		try {
			final long position = haystack.positionOf(needle);

			// Shorter text is inside the longer text (speedup).
			final Operation op = (buffer1.getLength() > buffer2.getLength()) ? Operation.DELETE : Operation.INSERT;

			final SpanBuffer bb = haystack.trunc(position);

			diff.add(new DiffFragment(op, bb));
			diff.add(new DiffFragment(Operation.EQUAL, needle));
			diff.add(new DiffFragment(op, haystack.sliceAt(position + needle.getLength())));
			return diff;
		} catch (final NoMatchException expected) {
			// do nothing
		}

		if (needle.getLength() == 1) {
			// Single character string.
			// After the previous speedup, the character can't be an equality.
			diff.add(new DiffFragment(Operation.DELETE, buffer1));
			diff.add(new DiffFragment(Operation.INSERT, buffer2));
			return diff;
		}

		// Check to see if the problem can be split in two.
		HalfMatch.Result result = null;
		/*
		 * Don't risk returning a non-optimal diff if we have unlimited time.
		 */
		if (deadline != Long.MAX_VALUE) {
			// this method may return null
			result = new HalfMatch().halfMatch(buffer1, buffer2);
		}
		if (result != null) {
			// A half-match was found, sort out the return data.
			// final SpanBuffer text1A = hm[0];
			// final SpanBuffer text1B = hm[1];
			// final SpanBuffer text2A = hm[2];
			// final SpanBuffer text2B = hm[3];
			// final SpanBuffer commonMid = hm[4];

			if (Diff.LOG.isDebugEnabled()) {
				Diff.LOG.debug(String.format("Split %s %s", buffer1, buffer1.getText()));
				Diff.LOG.debug(String.format("Split %s %s", buffer2, buffer2.getText()));
				Diff.LOG.debug(String.format("Common %s %s", result.getCommonMid(), result.getCommonMid().getText()));
				// Send both pairs off for separate processing.
				Diff.LOG.debug(String.format("Adding %s %s", result.getText1A(), result.getText1A().getText()));
				Diff.LOG.debug(String.format("Adding %s %s", result.getText2A(), result.getText2A().getText()));
			}
			diff.add(new Diff(result.getText1A(), result.getText2A(), deadline));
			diff.add(new DiffFragment(Operation.EQUAL, result.getCommonMid()));
			if (Diff.LOG.isDebugEnabled()) {
				Diff.LOG.debug(String.format("Adding %s %s", result.getText1B(), result.getText1B().getText()));
				Diff.LOG.debug(String.format("Adding %s %s", result.getText2B(), result.getText2B().getText()));
			}
			diff.add(new Diff(result.getText1B(), result.getText2B(), deadline));

			return diff;
		}

		return new Bisect(deadline).bisect(buffer1, buffer2);
	}

	/**
	 * Add a fragment to this diff.
	 * 
	 * @param fragment the fragment to add.
	 */
	public void add(final DiffFragment fragment) {
		fragments.add(fragment);
	}

	/**
	 * Add all the fragments from the other Diff.
	 * 
	 * @param diff the other diff.
	 */
	public void add(final Diff diff) {
		fragments.addAll(diff.fragments);
	}

	/**
	 * Add as the first fragment.
	 * 
	 * @param fragment the fragment to add.
	 */
	public void addFirst(final DiffFragment fragment) {
		fragments.addFirst(fragment);
	}

	/**
	 * Add as the last fragment
	 * 
	 * @param fragment the fragment to add.
	 */
	public void addLast(final DiffFragment fragment) {
		fragments.addLast(fragment);
	}

	/**
	 * Reorder and merge like edit sections. Merge equalities. Any edit section can
	 * move as long as it doesn't cross an equality.
	 *
	 * @throws IOException on error
	 */
	public void cleanupMerge() throws IOException {
		/*
		 * Add a dummy entry at the end.
		 */
		fragments.add(new DiffFragment(Operation.EQUAL, Factory.EMPTY));
		final ListIterator<DiffFragment> pointer = fragments.listIterator();
		int deleteCount = 0;
		int insertCount = 0;
		SpanBuffer deletedText = Factory.EMPTY;
		SpanBuffer insertedText = Factory.EMPTY;
		DiffFragment thisDiff = pointer.next();
		DiffFragment prevEqual = null;
		int commonlength;
		while (thisDiff != null) {
			switch (thisDiff.getOperation()) {
			case INSERT:
				insertCount++;
				insertedText = Factory.merge(insertedText, thisDiff);
				prevEqual = null;
				break;
			case DELETE:
				deleteCount++;
				deletedText = Factory.merge(deletedText, thisDiff);
				prevEqual = null;
				break;
			case EQUAL:
				if ((deleteCount + insertCount) > 1) {
					final boolean bothTypes = (deleteCount != 0) && (insertCount != 0);
					// Delete the offending records.
					pointer.previous(); // Reverse direction.
					while (deleteCount-- > 0) {
						pointer.previous();
						pointer.remove();
					}
					while (insertCount-- > 0) {
						pointer.previous();
						pointer.remove();
					}
					if (bothTypes) {
						// Factor out any common prefixies.
						commonlength = NumberUtils.checkIntLimit("common prefix",
								insertedText.commonPrefix(deletedText));
						if (commonlength != 0) {
							if (pointer.hasPrevious()) {
								thisDiff = pointer.previous();
								if (thisDiff.getOperation() != Operation.EQUAL) {
									throw new IllegalStateException("Previous diff should have been an equality.");
								}

								thisDiff = new DiffFragment(thisDiff.getOperation(),
										Factory.merge(thisDiff, insertedText.head(commonlength)));
								// replace the old thisDiff
								pointer.remove();
								pointer.add(thisDiff);
								// pointer.next();
							} else {
								pointer.add(new DiffFragment(Operation.EQUAL, insertedText.head(commonlength)));
							}
							insertedText = insertedText.sliceAt(commonlength);
							deletedText = deletedText.sliceAt(commonlength);
						}
						// Factor out any common suffixies.
						commonlength = NumberUtils.checkIntLimit("common suffix",
								insertedText.commonSuffix(deletedText));
						if (commonlength != 0) {
							thisDiff = pointer.next();
							thisDiff = new DiffFragment(thisDiff.getOperation(),
									Factory.merge(insertedText.cut(insertedText.getLength() - commonlength), thisDiff));
							// replace the old thisDiff
							pointer.remove();
							pointer.add(thisDiff);
							insertedText = insertedText.head(insertedText.getLength() - commonlength);
							deletedText = deletedText.head(deletedText.getLength() - commonlength);
							pointer.previous();
						}
					}
					// Insert the merged records.
					if (deletedText.getLength() != 0) {
						pointer.add(new DiffFragment(Operation.DELETE, deletedText));
					}
					if (insertedText.getLength() != 0) {
						pointer.add(new DiffFragment(Operation.INSERT, insertedText));
					}
					// Step forward to the equality.
					thisDiff = pointer.hasNext() ? pointer.next() : null;
				} else if (prevEqual != null) {
					// Merge this equality with the previous one.
					pointer.remove();
					pointer.previous();
					pointer.remove();
					prevEqual = new DiffFragment(Operation.EQUAL, Factory.merge(prevEqual, thisDiff));
					pointer.add(prevEqual);

					// reset the pointer
					thisDiff = pointer.previous();
					pointer.next(); // Forward direction
				}
				insertCount = 0;
				deleteCount = 0;
				deletedText = Factory.EMPTY;
				insertedText = Factory.EMPTY;
				prevEqual = thisDiff;
				break;
			default:
				throw new IllegalStateException("Unexpected operation: " + thisDiff.getOperation());
			}
			thisDiff = pointer.hasNext() ? pointer.next() : null;
		}
		if (fragments.getLast().getLength() == 0) {
			fragments.removeLast(); // Remove the dummy entry at the end.
		}

		cleanupMergePhase2();
	}

	// remove three previous entries.
	private void removeThree(final ListIterator<DiffFragment> pointer) {
		pointer.previous(); // Walk past nextDiff.
		pointer.remove();
		pointer.previous(); // Walk past thisDiff.
		pointer.remove();
		pointer.previous(); // Walk past prevDiff.
		pointer.remove();
	}

	/*
	 * Second pass: look for single edits surrounded on both sides by equalities
	 * which can be shifted sideways to eliminate an equality. e.g: A<ins>BA</ins>C
	 * -> <ins>AB</ins>AC
	 */
	/* package private for test */ void cleanupMergePhase2() throws IOException {
		boolean changes = false;
		final ListIterator<DiffFragment> pointer = fragments.listIterator();
		DiffFragment prevDiff = pointer.hasNext() ? pointer.next() : null;
		DiffFragment thisDiff = pointer.hasNext() ? pointer.next() : null;
		DiffFragment nextDiff = pointer.hasNext() ? pointer.next() : null;
		// Intentionally ignore the first and last element (don't need
		// checking).
		while (nextDiff != null) {
			if ((prevDiff.getOperation() == Operation.EQUAL) && (nextDiff.getOperation() == Operation.EQUAL)) {

				// This is a single edit surrounded by equalities.
				if (thisDiff.endsWith(prevDiff)) {
					// remove the three candidates
					removeThree(pointer);

					// Shift the edit over the previous equality.
					thisDiff = new DiffFragment(thisDiff.getOperation(),
							Factory.merge(prevDiff, thisDiff.head(thisDiff.getLength() - prevDiff.getLength())));
					pointer.add(thisDiff); // add modified thisDiff
					nextDiff = new DiffFragment(nextDiff.getOperation(), Factory.merge(prevDiff, nextDiff));
					pointer.add(nextDiff); // add modified nextdiff
					nextDiff = pointer.hasNext() ? pointer.next() : null;
					changes = true;
				} else if (thisDiff.startsWith(nextDiff)) {
					removeThree(pointer);
					// Shift the edit over the next equality.
					prevDiff = new DiffFragment(prevDiff.getOperation(), Factory.merge(prevDiff, nextDiff));
					pointer.add(prevDiff); // add modified prevDiff

					thisDiff = new DiffFragment(thisDiff.getOperation(),
							Factory.merge(thisDiff.cut(nextDiff.getLength()), nextDiff));
					pointer.add(thisDiff);

					nextDiff = pointer.hasNext() ? pointer.next() : null;
					changes = true;
				}
			}
			prevDiff = thisDiff;
			thisDiff = nextDiff;
			nextDiff = pointer.hasNext() ? pointer.next() : null;
		}
		// If shifts were made, the diff needs reordering and another shift
		// sweep.
		if (changes) {
			cleanupMerge();
		}

	}

	/**
	 * Return true if the fragment list is empty.
	 * 
	 * @return true if the fragment list is empty.
	 */
	public boolean isEmpty() {
		return fragments.isEmpty();
	}

	/**
	 * List the fragments.
	 * 
	 * @return
	 */
	public List<DiffFragment> getFragments() {
		return Collections.unmodifiableList(fragments);
	}

	/**
	 * Get the first fragment.
	 * 
	 * @return the first fragment.
	 */
	public DiffFragment getFirst() {
		return fragments.get(0);
	}

	/**
	 * Get the last fragment.
	 * 
	 * @return the last fragment
	 */
	public DiffFragment getLast() {
		return fragments.get(fragments.size() - 1);
	}

	/**
	 * Compute and return the source text excluding the operation type.
	 *
	 * @param ignore the operation type to ignore (may be null)
	 * @return Source text.
	 * @throws IOException ion error
	 */
	public SpanBuffer extract(final Operation ignore) throws IOException {

		final Stream<SpanBuffer> bufStream = fragments.stream().filter(diffFrag -> diffFrag.getOperation() != ignore)
				.map(diffFrag -> (SpanBuffer) diffFrag);

		return Factory.merge(bufStream);
	}

	/**
	 * Compute the Levenshtein distance; the number of inserted, deleted or
	 * substituted characters.
	 *
	 * @return Number of changes.
	 */
	public int levenshtein() {
		int levenshtein = 0;
		int insertions = 0;
		int deletions = 0;
		for (final DiffFragment fragment : fragments) {
			switch (fragment.getOperation()) {
			case INSERT:
				insertions += fragment.getLength();
				break;
			case DELETE:
				deletions += fragment.getLength();
				break;
			case EQUAL:
				// A deletion and an insertion is one substitution.
				levenshtein += Math.max(insertions, deletions);
				insertions = 0;
				deletions = 0;
				break;
			default:
				throw new IllegalStateException("Unexpected operation: " + fragment.getOperation());

			}
		}
		levenshtein += Math.max(insertions, deletions);
		return levenshtein;
	}

	/**
	 * loc is a location in text1, compute and return the equivalent location in
	 * text2. e.g. "The cat" vs "The big cat", 1-&gt;1, 5-&gt;8
	 *
	 * @param loc Location within left text.
	 * @return Location within right text.
	 */
	public long getIndex(final long loc) {
		long chars1 = 0;
		long chars2 = 0;
		long lastChars1 = 0;
		long lastChars2 = 0;
		DiffFragment lastDiff = null;
		for (final DiffFragment aDiff : fragments) {
			if (aDiff.getOperation() != Operation.INSERT) {
				// Equality or deletion.
				chars1 += aDiff.getLength();
			}
			if (aDiff.getOperation() != Operation.DELETE) {
				// Equality or insertion.
				chars2 += aDiff.getLength();
			}
			if (chars1 > loc) {
				// Overshot the location.
				lastDiff = aDiff;
				break;
			}
			lastChars1 = chars1;
			lastChars2 = chars2;
		}
		if ((lastDiff != null) && (lastDiff.getOperation() == Operation.DELETE)) {
			// The location was deleted.
			return lastChars2;
		}
		// Add the remaining character length.
		return lastChars2 + (loc - lastChars1);
	}

	/**
	 * A Diff fragment. This is a SpanBuffer with an Operation.
	 *
	 */
	public interface Fragment extends SpanBuffer {
		/**
		 * Get the operation for the fragment.
		 * 
		 * @return the operation.
		 */
		public Operation getOperation();
	}

	/**
	 * class to build a Diff.
	 *
	 */
	public static class Builder {
		private Long maxProcessTime;

		/**
		 * Default constructor.
		 * <p>
		 * Uses umlimited time.
		 * </p>
		 */
		public Builder() {
			maxProcessTime = Long.valueOf(-1);
		}

		/**
		 * Set the maximum number of minutes for processing a diff.
		 * 
		 * @param minutes the maximum number of minutes ot process.
		 * @return the Builder for chaining
		 */
		public Builder setProcessMinutes(final float minutes) {
			maxProcessTime = (long) (minutes * DateUtils.MILLIS_PER_MINUTE);
			return this;
		}

		/**
		 * Set the maximum number of hours for processing a diff.
		 * 
		 * @param hours the maximum number of hours.
		 * @return the Builder for chaining
		 */
		public Builder setProcessHours(final float hours) {
			maxProcessTime = (long) (hours * DateUtils.MILLIS_PER_HOUR);
			return this;
		}

		/**
		 * Set the maximum number of seconds for processing a diff.
		 * 
		 * @param seconds the maximum number of seconds.
		 * @return the Builder for chaining
		 */
		public Builder setProcessSeconds(final float seconds) {
			maxProcessTime = (long) (seconds * DateUtils.MILLIS_PER_SECOND);
			return this;
		}

		/**
		 * Set the maximum number of milliseconds for processing a diff.
		 * 
		 * @param milliseconds the maximum number of milliseconds.
		 * @return the Builder for chaining
		 */
		public Builder setProcessTime(final long milliseconds) {
			maxProcessTime = milliseconds;
			return this;
		}

		/**
		 * Use unlimited time for processing the diff.
		 * 
		 * @return the Builder for chaining
		 */
		public Builder setUnlimitedProcessTime() {
			maxProcessTime = -1L;
			return this;
		}

		/**
		 * Perform the fastest diff possible. May not perform clean up that simpleifies
		 * the result.
		 * 
		 * @return the Builder for chaining
		 */
		public Builder setSkipDetail() {
			maxProcessTime = 0L;
			return this;
		}

		/**
		 * Build a Diff instance that will diff buffer1 and buffer2.
		 *
		 * @param oldBuffer the Old buffer to be diffed
		 * @param newBuffer the New buffer to be diffed
		 * @return A Diff that contains the differences between the two buffers.
		 * @throws IOException on IO error
		 */
		public Diff build(final SpanBuffer oldBuffer, final SpanBuffer newBuffer) throws IOException {
			// unlimited time
			if (maxProcessTime < 0) {
				return new Diff(oldBuffer, newBuffer, Long.MAX_VALUE);
			} else {
				return new Diff(oldBuffer, newBuffer, System.currentTimeMillis() + maxProcessTime);
			}
		}
	}
}
