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
