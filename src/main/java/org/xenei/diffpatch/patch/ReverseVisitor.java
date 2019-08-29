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

import org.xenei.diffpatch.Operation;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.span.LongSpan;

/**
 * Reverses the patch fragment values.
 * 
 * Generally patch is applied to the left file to yield the right file. This
 * visitor changes the patch so that applying it to the right file will yield
 * the left file.
 */
public class ReverseVisitor implements PatchFragmentVisitor {

	private long offset;
	private PatchFragment result;

	/**
	 * Constructor.
	 */
	public ReverseVisitor() {
		offset = 0;
	}

	/**
	 * Get the result of the visit.
	 * 
	 * @return the result of the last visit command.
	 */
	public PatchFragment getResult() {
		return result;
	}

	@Override
	public void visit(PatchFragment fragment) {

		LongSpan right = fragment.getRightSpan();
		right = LongSpan.fromLength(right.getOffset() + offset, right.getLength());
		LongSpan left = fragment.getLeftSpan();
		left = LongSpan.fromLength(left.getOffset() + offset, left.getLength());
		result = new PatchFragment(right, left);
		for (DiffFragment df : fragment.getDiff().getFragments()) {
			DiffFragment df2 = df;

			switch (df.getOperation()) {
			case DELETE:
				df2 = new DiffFragment(Operation.INSERT, df);
				offset += df.getLength();
				break;
			case INSERT:
				df2 = new DiffFragment(Operation.DELETE, df);
				offset -= df.getLength();
				break;
			default:
				// do nothing
				break;
			}
			result.add(df2);
		}
	}

}
