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

import org.xenei.span.NumberUtils;
import org.xenei.spanbuffer.SpanBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.diffpatch.diff.DiffFragment;

import java.io.IOException;
import java.util.Arrays;

/**
 * Class to find the bisection of two buffers.
 *
 */
public class Bisect {

	private static final Logger LOG = LoggerFactory.getLogger(Bisect.class);

	/**
	 * Specified as milliseconds since the epoch.
	 */
	private long deadline;

	/**
	 * Constructor.
	 * 
	 * @param deadline the time limit by which to finish the bisection specified in
	 *                 milliseconds since the epoch.
	 */
	public Bisect(long deadline) {
		this.deadline = deadline;
	}

	/**
	 * Find the 'middle snake' of a diff, split the problem in two and return the
	 * recursively constructed diff. See Myers 1986 paper: An O(ND) Difference
	 * Algorithm and Its Variations.
	 * 
	 * @param buffer1 Old buffer to be diffed.
	 * @param buffer2 New new to be diffed.
	 * @return LinkedList of Diff objects.
	 * @throws IOException on error
	 */
	public Diff bisect(SpanBuffer buffer1, SpanBuffer buffer2) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("bisect %s %s --with-- %s %s", buffer1, buffer1.getText(), buffer2,
					buffer2.getText()));
		}
		// Cache the text lengths to prevent multiple calls.
		int buffer1Length = NumberUtils.checkIntLimit("buffer1 length", buffer1.getLength());
		int buffer2Length = NumberUtils.checkIntLimit("buffer2 length", buffer2.getLength());
		int maxD = (buffer1Length + buffer2Length + 1) / 2;
		int vOffset = maxD;
		int vLength = 2 * maxD;
		int[] v1 = new int[vLength];
		int[] v2 = new int[vLength];
		Arrays.fill(v1, -1);
		Arrays.fill(v2, -1);
		v1[vOffset + 1] = 0;
		v2[vOffset + 1] = 0;
		int delta = buffer1Length - buffer2Length;
		// If the total number of characters is odd, then the front path will
		// collide with the reverse path.
		boolean front = (delta % 2 != 0);
		// Offsets for start and end of k loop.
		// Prevents mapping of space beyond the grid.
		int k1start = 0;
		int k1end = 0;
		int k2start = 0;
		int k2end = 0;
		for (int d = 0; d < maxD; d++) {
			// Bail out if deadline is reached.
			if (System.currentTimeMillis() > deadline) {
				break;
			}

			// Walk the front path one step.
			for (int k1 = -d + k1start; k1 <= d - k1end; k1 += 2) {
				int k1Offset = vOffset + k1;
				int x1;
				if (k1 == -d || (k1 != d && v1[k1Offset - 1] < v1[k1Offset + 1])) {
					x1 = v1[k1Offset + 1];
				} else {
					x1 = v1[k1Offset - 1] + 1;
				}
				int y1 = x1 - k1;
				while (x1 < buffer1Length && y1 < buffer2Length
						&& buffer1.readRelative(x1) == buffer2.readRelative(y1)) {
					x1++;
					y1++;
				}
				v1[k1Offset] = x1;
				if (x1 > buffer1Length) {
					// Ran off the right of the graph.
					k1end += 2;
				} else if (y1 > buffer2Length) {
					// Ran off the bottom of the graph.
					k1start += 2;
				} else if (front) {
					int k2Offset = vOffset + delta - k1;
					if (k2Offset >= 0 && k2Offset < vLength && v2[k2Offset] != -1) {
						// Mirror x2 onto top-left coordinate system.
						int x2 = buffer1Length - v2[k2Offset];
						if (x1 >= x2) {
							// Overlap detected.
							return bisectSplit(buffer1, buffer2, x1, y1);
						}
					}
				}
			}

			// Walk the reverse path one step.
			for (int k2 = -d + k2start; k2 <= d - k2end; k2 += 2) {
				int k2Offset = vOffset + k2;
				int x2;
				if (k2 == -d || (k2 != d && v2[k2Offset - 1] < v2[k2Offset + 1])) {
					x2 = v2[k2Offset + 1];
				} else {
					x2 = v2[k2Offset - 1] + 1;
				}
				int y2 = x2 - k2;
				while (x2 < buffer1Length && y2 < buffer2Length
						&& buffer1.readRelative(buffer1.getLength() - x2 - 1) == buffer2
								.readRelative(buffer2.getLength() - y2 - 1)) {
					x2++;
					y2++;
				}
				v2[k2Offset] = x2;
				if (x2 > buffer1Length) {
					// Ran off the left of the graph.
					k2end += 2;
				} else if (y2 > buffer2Length) {
					// Ran off the top of the graph.
					k2start += 2;
				} else if (!front) {
					int k1Offset = vOffset + delta - k2;
					if (k1Offset >= 0 && k1Offset < vLength && v1[k1Offset] != -1) {
						int x1 = v1[k1Offset];
						int y1 = vOffset + x1 - k1Offset;
						// Mirror x2 onto top-left coordinate system.
						x2 = buffer1Length - x2;
						if (x1 >= x2) {
							// Overlap detected.
							return bisectSplit(buffer1, buffer2, x1, y1);
						}
					}
				}
			}
		}
		// Diff took too long and hit the deadline or
		// number of diffs equals number of characters, no commonality at all.
		Diff diff = new Diff();
		diff.add(new DiffFragment(Operation.DELETE, buffer1));
		diff.add(new DiffFragment(Operation.INSERT, buffer2));
		return diff;
	}

	/**
	 * Given the location of the 'middle snake', split the diff in two parts and
	 * recurse.
	 * 
	 * @param oldBuffer      Old buffer to be diffed.
	 * @param newBuffer      New buffer to be diffed.
	 * @param oldBufferSplit Index of split point in oldBuffer.
	 * @param newBufferSplit Index of split point in newBuffer.
	 * @return LinkedList of Diff objects.
	 * @throws IOException on IO error
	 */
	private Diff bisectSplit(SpanBuffer oldBuffer, SpanBuffer newBuffer, int oldBufferSplit, int newBufferSplit)
			throws IOException {
		SpanBuffer buffer1a = oldBuffer.head(oldBufferSplit);
		SpanBuffer buffer2a = newBuffer.head(newBufferSplit);
		SpanBuffer buffer1b = oldBuffer.cut(oldBufferSplit);
		SpanBuffer buffer2b = newBuffer.cut(newBufferSplit);

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("bisectSplit buffer1 %s '%s'", oldBuffer, oldBuffer.getText()));
			LOG.debug(String.format("bisectSplit buffer1a %s '%s'", buffer1a, buffer1a.getText()));
			LOG.debug(String.format("bisectSplit buffer1b %s '%s'", buffer1b, buffer1b.getText()));
			LOG.debug(String.format("bisectSplit buffer2 %s '%s'", newBuffer, newBuffer.getText()));
			LOG.debug(String.format("bisectSplit buffer2a %s '%s'", buffer2a, buffer2a.getText()));
			LOG.debug(String.format("bisectSplit buffer2b %s '%s'", buffer2b, buffer2b.getText()));
		}

		// Compute both diffs serially.
		Diff diff = new Diff(buffer1a, buffer2a, deadline);
		diff.add(new Diff(buffer1b, buffer2b, deadline));

		return diff;
	}
}
