package org.xenei.diffpatch.patch;

/**
 * Implements the visitor pattern for patch fragments.
 */
public interface PatchFragmentVisitor {

	/**
	 * Visit the patch fragment.
	 * 
	 * @param fragment the fragment to visit.
	 */
	void visit(PatchFragment fragment);

}
