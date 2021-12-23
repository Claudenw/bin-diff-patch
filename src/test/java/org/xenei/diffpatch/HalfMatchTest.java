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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.xenei.spanbuffer.Factory;

public class HalfMatchTest {
    private HalfMatch halfMatch = new HalfMatch();

    private void assertResultEquals(String title, String[] expected, HalfMatch.Result result) {
        assertNotNull(title + " Result was null", result);
        for (int i = 0; i < 4; i++) {
            assertEquals(title + " Error at item " + i, Factory.wrap(expected[i]), result.get(i));
        }
    }

    @Test
    public void noMatchTest() throws Exception {

        // test no match
        assertNull("No match #1", halfMatch.halfMatch(Factory.wrap("1234567890"), Factory.wrap("abcdef")));
        assertNull("No match #2", halfMatch.halfMatch(Factory.wrap("12345"), Factory.wrap("23")));

    }

    @Test
    public void singleMatchTest() throws Exception {

        // test single match
        HalfMatch.Result result = halfMatch.halfMatch(Factory.wrap("1234567890"), Factory.wrap("a345678z"));
        assertResultEquals("Single Match #1", new String[] { "12", "90", "a", "z", "345678" }, result);

        result = halfMatch.halfMatch(Factory.wrap("a345678z"), Factory.wrap("1234567890"));
        assertResultEquals("Single Match #2", new String[] { "a", "z", "12", "90", "345678" }, result);

        result = halfMatch.halfMatch(Factory.wrap("abc56789z"), Factory.wrap("1234567890"));
        assertResultEquals("Single Match #3", new String[] { "abc", "z", "1234", "0", "56789" }, result);

        result = halfMatch.halfMatch(Factory.wrap("a23456xyz"), Factory.wrap("1234567890"));
        assertResultEquals("Single Match #4", new String[] { "a", "xyz", "1", "7890", "23456" }, result);
    }

    @Test
    public void multipleMatchTest() throws Exception {

        // test multiple match
        HalfMatch.Result result = halfMatch.halfMatch(Factory.wrap("121231234123451234123121"),
                Factory.wrap("a1234123451234z"));
        assertResultEquals("Multiple Matchs #1", new String[] { "12123", "123121", "a", "z", "1234123451234" }, result);

        result = halfMatch.halfMatch(Factory.wrap("x-=-=-=-=-=-=-=-=-=-=-=-="), Factory.wrap("xx-=-=-=-=-=-=-="));
        assertResultEquals("Multiple Matchs #2", new String[] { "", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-=" }, result);

        result = halfMatch.halfMatch(Factory.wrap("-=-=-=-=-=-=-=-=-=-=-=-=y"), Factory.wrap("-=-=-=-=-=-=-=yy"));
        assertResultEquals("Multiple Matchs #3", new String[] { "-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y" }, result);

    }

    @Test
    public void nonOptimalHalfMatchTest() throws Exception {
        // Optimal diff would be -q+x=H-i+e=lloHe+Hu=llo-Hew+y not
        // -qHillo+x=HelloHe-w+Hulloy
        HalfMatch.Result result = halfMatch.halfMatch(Factory.wrap("qHilloHelloHew"), Factory.wrap("xHelloHeHulloy"));
        assertResultEquals("Non-optimal halfmatch", new String[] { "qHillo", "w", "x", "Hulloy", "HelloHe" }, result);
    }

    //
    // assertArrayEquals("diff_halfMatch: Single Match #1.", new String[]{"12",
    // "90", "a", "z", "345678"}, dmp.diff_halfMatch("1234567890", "a345678z"));
    //
    // assertArrayEquals("diff_halfMatch: Single Match #2.", new String[]{"a", "z",
    // "12", "90", "345678"}, dmp.diff_halfMatch("a345678z", "1234567890"));
    //
    // assertArrayEquals("diff_halfMatch: Single Match #3.", new String[]{"abc",
    // "z", "1234", "0", "56789"}, dmp.diff_halfMatch("abc56789z", "1234567890"));
    //
    // assertArrayEquals("diff_halfMatch: Single Match #4.", new String[]{"a",
    // "xyz", "1", "7890", "23456"}, dmp.diff_halfMatch("a23456xyz", "1234567890"));
    //
    // assertArrayEquals("diff_halfMatch: Multiple Matches #1.", new
    // String[]{"12123", "123121", "a", "z", "1234123451234"},
    // dmp.diff_halfMatch("121231234123451234123121", "a1234123451234z"));
    //
    // assertArrayEquals("diff_halfMatch: Multiple Matches #2.", new String[]{"",
    // "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-="},
    // dmp.diff_halfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="));
    //
    // assertArrayEquals("diff_halfMatch: Multiple Matches #3.", new
    // String[]{"-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y"},
    // dmp.diff_halfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"));
    //
    // // Optimal diff would be -q+x=H-i+e=lloHe+Hu=llo-Hew+y not
    // -qHillo+x=HelloHe-w+Hulloy
    // assertArrayEquals("diff_halfMatch: Non-optimal halfmatch.", new
    // String[]{"qHillo", "w", "x", "Hulloy", "HelloHe"},
    // dmp.diff_halfMatch("qHilloHelloHew", "xHelloHeHulloy"));
    //
    // dmp.Diff_Timeout = 0;
    // assertNull("diff_halfMatch: Optimal no halfmatch.",
    // dmp.diff_halfMatch("qHilloHelloHew", "xHelloHeHulloy"));

}
