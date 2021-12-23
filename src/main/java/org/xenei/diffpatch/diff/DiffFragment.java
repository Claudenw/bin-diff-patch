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
package org.xenei.diffpatch.diff;

import java.io.IOException;

import org.xenei.diffpatch.Diff;
import org.xenei.diffpatch.Operation;

import org.xenei.spanbuffer.SpanBuffer;
import org.xenei.spanbuffer.WrappedSpanBuffer;

/**
 * Class representing one diff operation. Comprises a Spanbuffer and an
 * Operation.
 */
public class DiffFragment extends WrappedSpanBuffer implements Diff.Fragment {
    /**
     * One of: INSERT, DELETE or EQUAL.
     */
    private final Operation operation;

    /**
     * Create an instance of DiffFragment from a Diff.Fragment.
     *
     * @param frag if frag is not a DiffFragment create it otherwise just return it.
     * @return the DiffFragment.
     */
    public static DiffFragment makeInstance(final Diff.Fragment frag) {
        if (frag instanceof DiffFragment) {
            return (DiffFragment) frag;
        } else {
            return new DiffFragment(frag.getOperation(), frag.duplicate(0));
        }
    }

    /**
     * Constructor. Initializes the diff with the provided values.
     *
     * @param operation One of INSERT, DELETE or EQUAL.
     * @param buffer1   The buffer being applied.
     */
    public DiffFragment(final Operation operation, final SpanBuffer buffer1) {
        super(buffer1);
        this.operation = operation;
    }

    @Override
    public Operation getOperation() {
        return operation;
    }

    @Override
    public String toString() {

        String str;
        try {
            str = String.format("[%s]", super.getText());
        } catch (final IOException ex) {
            str = super.toString();
        }
        return String.format("Diff(%s, %s )", operation, str);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = (operation == null) ? 0 : operation.hashCode();
        result += prime * SpanBuffer.Utils.hashCode(this);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        final DiffFragment other = (DiffFragment) obj;
        if (operation != other.operation) {
            return false;
        }
        return SpanBuffer.Utils.equals(this, other);
    }

    @Override
    public DiffFragment duplicate(final long newOffset) {

        final SpanBuffer newBuff = super.getBuffer().duplicate(newOffset);
        if (newBuff == super.getBuffer()) {
            return this;
        }
        return new DiffFragment(operation, newBuff);
    }

    @Override
    public DiffFragment sliceAt(final long position) {
        return new DiffFragment(operation, super.getBuffer().sliceAt(position));
    }

    @Override
    public DiffFragment tail(final long position) {
        return new DiffFragment(operation, super.getBuffer().tail(position));
    }

    @Override
    public DiffFragment safeTail(final long position) {
        return new DiffFragment(operation, super.getBuffer().safeTail(position));
    }

    @Override
    public DiffFragment trunc(final long position) {
        return new DiffFragment(operation, super.getBuffer().trunc(position));
    }

    @Override
    public DiffFragment head(final long byteCount) {
        return new DiffFragment(operation, super.getBuffer().head(byteCount));
    }

    @Override
    public DiffFragment concat(final SpanBuffer otherBuffer) {
        return new DiffFragment(operation, super.getBuffer().concat(otherBuffer));
    }

    @Override
    public DiffFragment cut(final long byteCount) {
        return new DiffFragment(operation, super.getBuffer().cut(byteCount));
    }

}