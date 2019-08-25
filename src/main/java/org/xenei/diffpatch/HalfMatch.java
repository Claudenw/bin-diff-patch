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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.span.LongSpan.ComparatorByLength;
import org.xenei.spanbuffer.SpanBuffer;
import org.xenei.spanbuffer.Factory;
import org.xenei.spanbuffer.NoMatchException;

/**
 * Class to determine if two buffers share a common buffer which is at least
 * half the length of the longer buffer. This speedup can produce non-minimal
 * diffs.
 *
 */
public class HalfMatch {
	private static final Logger LOG = LoggerFactory.getLogger(HalfMatch.class);

	/**
	 * Constructor.
	 */
	public HalfMatch() {
	}

	/**
	 * Do the two texts share a substring which is at least half the length of the
	 * longer text? This speedup can produce non-minimal diffs.
	 *
	 * @param buffer1 First string.
	 * @param buffer2 Second string.
	 * @return a result object or null if there was no match
	 * @throws IOException on IO error
	 */
	public Result halfMatch(final SpanBuffer buffer1, final SpanBuffer buffer2) throws IOException {

		final SpanBuffer longBuffer = (SpanBuffer) ComparatorByLength.longest(buffer1, buffer2);
		final SpanBuffer shortBuffer = (SpanBuffer) ComparatorByLength.shortest(buffer1, buffer2);
		if ((longBuffer.getLength() < 4) || ((shortBuffer.getLength() * 2) < longBuffer.getLength())) {
			return null; // Pointless.
		}
		final long b1Len = buffer1.getLength();
		final long b2Len = buffer2.getLength();
		// First check if the second quarter is the seed for a half-match.
		final Result hm1 = halfMatch(longBuffer, shortBuffer, (longBuffer.getLength() + 3) / 4);
		// Check again based on the third quarter.
		final Result hm2 = halfMatch(longBuffer, shortBuffer, (longBuffer.getLength() + 1) / 2);
		Result hm;
		if ((hm1 == null) && (hm2 == null)) {
			return null;
		} else if (hm2 == null) {
			hm = hm1;
		} else if (hm1 == null) {
			hm = hm2;
		} else {
			// Both matched. Select the longest.
			hm = hm1.commonMid.getLength() > hm2.commonMid.getLength() ? hm1 : hm2;
		}

		// A half-match was found, sort out the return data.
		if (b1Len <= b2Len) {
			hm.swap();
		}
		return hm;

	}

	/**
	 * Determine if a portion of shortBuffer exist within longBuffer such that the
	 * buffer is at least half the length of longBuffer.
	 * <p>
	 * Note that longBufferOffset is not relative to longBuffer.getOffset().
	 * </p>
	 * 
	 * @param longBuffer       Longer string.
	 * @param shortBuffer      Shorter string.
	 * @param longBufferOffset Start index (absolute) of quarter length substring
	 *                         within longBuffer..
	 * @return Five element String array, containing the prefix of longBuffer, the
	 *         suffix of longBuffer, the prefix of shortBuffer, the suffix of
	 *         shortBuffer and the common middle. Or null if there was no match.
	 * @throws IOException on IO error
	 */
	private Result halfMatch(final SpanBuffer longBuffer, final SpanBuffer shortBuffer, final long longBufferOffset)
			throws IOException {

		// Start with a 1/4 length substring at position i as a seed.
		// new SpanImpl(longBuffer.getStart()+i, longBuffer.getLength() / 4));
		final SpanBuffer seed = longBuffer.cut(longBufferOffset).head(longBuffer.getLength() / 4);
		if (HalfMatch.LOG.isDebugEnabled()) {
			HalfMatch.LOG.debug(String.format("seed: %s '%s' ", seed, seed.getText()));
		}
		Result result = new Result();
		try {
			long shortBufferPos = shortBuffer.getOffset() - 1;
			while (true) {

				shortBufferPos = shortBuffer.positionOf(seed, shortBufferPos + 1);

				final long prefixLength = longBuffer.cut(longBufferOffset)
						.commonPrefix(shortBuffer.sliceAt(shortBufferPos));

				final long suffixLength = longBuffer.head(longBufferOffset)
						.commonSuffix(shortBuffer.trunc(shortBufferPos));

				if (result.commonMid.getLength() < (suffixLength + prefixLength)) {
					result.commonMid = shortBuffer.sliceAt(shortBufferPos - suffixLength).trunc(shortBufferPos)
							.concat(shortBuffer.sliceAt(shortBufferPos).trunc(shortBufferPos + prefixLength));

					result.text1A = longBuffer.head(longBufferOffset - suffixLength);
					result.text1B = longBuffer.cut(longBufferOffset + prefixLength);
					result.text2A = shortBuffer.trunc(shortBufferPos - suffixLength);
					result.text2B = shortBuffer.sliceAt(shortBufferPos + prefixLength);

				}

			}
		} catch (final NoMatchException expected) {
			// do nothing
		} catch (final IOException ex) {
			HalfMatch.LOG.error("Error reading data in halfMatch", ex);
		}

		if ((result.commonMid.getLength() * 2) >= longBuffer.getLength()) {
			return result;
		} else {
			return null;
		}
	}

	/**
	 * A result of a half match check.
	 *
	 */
	public static class Result {
		private SpanBuffer text1A = Factory.EMPTY; // hm[0];
		private SpanBuffer text1B = Factory.EMPTY; // hm[1];
		private SpanBuffer text2A = Factory.EMPTY; // hm[2];
		private SpanBuffer text2B = Factory.EMPTY; // hm[3];
		private SpanBuffer commonMid = Factory.EMPTY; // hm[4];

		/**
		 * get the first portion of the first buffer.
		 * 
		 * @return the first half of the first buffer.
		 */
		public final SpanBuffer getText1A() {
			return text1A;
		}

		/**
		 * Get the last portion of the first buffer.
		 * 
		 * @return the last portion of the first buffer
		 */
		public final SpanBuffer getText1B() {
			return text1B;
		}

		/**
		 * get the first portion of the second buffer.
		 * 
		 * @return the first half of the second buffer.
		 */
		public final SpanBuffer getText2A() {
			return text2A;
		}

		/**
		 * Get the last portion of the second buffer.
		 * 
		 * @return the last portion of the second buffer
		 */
		public final SpanBuffer getText2B() {
			return text2B;
		}

		/**
		 * Get the common portion of the two buffers.
		 * 
		 * @return the common portion of the two buffers.
		 */
		public final SpanBuffer getCommonMid() {
			return commonMid;
		}

		/**
		 * for testing.
		 * 
		 * @param idx the index to get.
		 * @return the span buffer at the index
		 */
		/* package private */public final SpanBuffer get(int idx) {
			SpanBuffer[] buffs = { text1A, text1B, text2A, text2B, commonMid };
			return buffs[idx];
		}

		private final void swap() {
			SpanBuffer tmp = text2A;
			text2A = text1A;
			text1A = tmp;

			tmp = text2B;
			text2B = text1B;
			text1B = tmp;
		}
	}
}
