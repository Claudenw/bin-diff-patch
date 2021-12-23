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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.xenei.diffpatch.diff.DiffFragment;
import org.xenei.spanbuffer.Factory;

public class BisectTest {

    @Test
    public void bisectTestNormal() throws Exception {
        Diff diff = new Bisect(Long.MAX_VALUE).bisect(Factory.wrap("cat"), Factory.wrap("map"));
        List<DiffFragment> lst = diff.getFragments();
        assertEquals(5, lst.size());
        assertEquals(new DiffFragment(Operation.DELETE, Factory.wrap("c")), lst.get(0));
        assertEquals(new DiffFragment(Operation.INSERT, Factory.wrap("m")), lst.get(1));
        assertEquals(new DiffFragment(Operation.EQUAL, Factory.wrap("a")), lst.get(2));
        assertEquals(new DiffFragment(Operation.DELETE, Factory.wrap("t")), lst.get(3));
        assertEquals(new DiffFragment(Operation.INSERT, Factory.wrap("p")), lst.get(4));
    }

    @Test
    public void bisectTestTimeout() throws Exception {
        Diff diff = new Bisect(0).bisect(Factory.wrap("cat"), Factory.wrap("map"));
        List<DiffFragment> lst = diff.getFragments();
        assertEquals(2, lst.size());
        assertEquals(new DiffFragment(Operation.DELETE, Factory.wrap("cat")), lst.get(0));
        assertEquals(new DiffFragment(Operation.INSERT, Factory.wrap("map")), lst.get(1));
    }

}
