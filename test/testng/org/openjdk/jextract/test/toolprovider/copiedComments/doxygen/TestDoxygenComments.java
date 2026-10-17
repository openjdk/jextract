/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.jextract.test.toolprovider.copiedComments.doxygen;

import org.openjdk.jextract.test.toolprovider.copiedComments.raw.TestRawComments;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;
import testlib.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

public class TestDoxygenComments extends JextractToolRunner {
    @Test
    public void testConversions() throws IOException {
        Set<String> copiedComments = getCopiedComments("conversions.h", "conversions_h.java");
        assertEquals(copiedComments, Set.of(
            """
            <p>mdfkR</p>

            @param a DMppz
            @throws PBHox
            @throws gQggq
            @throws JlLlB
            @see rldXW
            @see KBJtl
            @deprecated jUNVH
            @since guUpf
            @author ZJYmn""",
            """
            <p>wjPaq</p>

            @return nOZxm
            @author vHYBD""",
            "@return vdHVK",
            """
            <p>hlMuF</p>
            <ul><li>jmvOH</li></ul>
            <ul><li>DVDvK</li></ul>""",

            """
            <p><b>attention</b>: zIVELv</p>
            <p><b>bug</b>: DoGkH</p>
            <p><b>copyright</b>: RfboO</p>
            <p><b>date</b>: bkWkZ</p>
            <p><b>details</b>: rsOFX</p>
            <p><b>invariant</b>: tnJBI</p>
            <p><b>note</b>: tYiZb</p>
            <p><b>post</b>: HiYkH</p>
            <p><b>pre</b>: KBAio</p>
            <p><b>remark</b>: JCPgR</p>
            <p><b>remarks</b>: mZwCi</p>
            <p><b>retval</b>: xVwZQ</p>
            <p><b>todo</b>: JOBgs</p>
            <p><b>version</b>: KIQOC</p>
            <p><b>warning</b>: NiKEQ</p>""",

            """
            <pre><code>
             GsgCz
            </code></pre>
            <pre><code>
             sQgMZ
            </code></pre>""",

            "<p><strong>XIrls</strong> <em>XSXDD</em> <em>tziIu</em> <em>oBYQL</em> <code>ThZXB</code> <code>UpCVA</code> <code>PYFvb</code></p>",

            "<p><a kDFlc=\"cWJBx\">KgNnj</a> <br/> <code>MaHPQ</code> <s>JNDRQ</s></p>",

            """
            @param a xXqcs
            @param b [in] fEpuI
            @param c [out] mPVjJ
            @param d [in,out] MaUPO
            @param e [in,out] blCUS""",

            "<p>var_Vqube</p>",
            "<p>func_pIERk</p>",
            "<p>typedef_wSJnT</p>"
        ));

        copiedComments = getCopiedComments("conversions.h", "typedef_KdWnL.java");
        assertEquals(copiedComments, Set.of(
            """
            <p>YqFta</p>

            <p><b>param</b> <code>a</code> vQxbR</p>
            <p><b>return</b>: fKPsJ</p>"""
        ));

        copiedComments = getCopiedComments("conversions.h", "struct_hcHgy.java");
        assertEquals(copiedComments, Set.of(
            "<p>struct_hcHgy</p>"
        ));

        copiedComments = getCopiedComments("conversions.h", "union_IiFeM.java");
        assertEquals(copiedComments, Set.of(
            "<p>union_IiFeM</p>"
        ));
    }

    @Test
    public void testAppendOrder() throws IOException {
        Set<String> copiedComments = getCopiedComments("appendOrder.h", "appendOrder_h.java");
        assertEquals(copiedComments, Set.of(
            """
            <p>Zeivk</p>
            <p>zqQMW</p>

            @param a KJalo""",
            """
            <p><b>note</b>: XbjrU</p>

            @param a MWrHh""",
            """
            <p>rwHNr</p>

            <p>LWSEU</p>

            @param a nQkGK"""
        ));
    }

    @Test
    public void testMangling() throws IOException {
        Set<String> copiedComments = getCopiedComments("manglingTests.h", "manglingTests_h.java");
        assertEquals(copiedComments, Set.of(
            "@param value_ nmliA",
            """
            @param a HxUwj
            @param x1 zekZd"""
        ));
    }

    @Test
    public void testSanitization() throws IOException {
        Set<String> copiedComments = getCopiedComments("sanitizationTests.h", "sanitizationTests_h.java");
        assertEquals(copiedComments, Set.of(
            "<p>*@/</p>",
            """
            <pre><code>
             &#92;u002a&#92;u002f
            </code></pre>""",
            """
            <pre><code>
             C:&#92;users\\name
            </code></pre>""",
            "<p>Compare a &lt; b &amp; c.</p>",
            """
            <pre><code>
             if (value) {
            </code></pre>""",
            """
            <pre><code>
             &lt;/code&gt;&lt;/pre&gt;
            </code></pre>""",
            "<p><a title=\"&quot;java&quot;\" href=\"https://dev.java/\">Java</a></p>",
            "<p><a href>Java</a></p>",
            ""
        ));
    }

    @Test
    public void integrationTests() throws IOException {
        Set<String> copiedComments = getCopiedComments("integrationTests.h", "integrationTests_h.java");
        assertEquals(copiedComments, Set.of(
            """
            <p>Array of character pointers.</p>

            <p>Holds <em>five</em> pointers to character data.</p>
            <p>Each entry may point to a <code>null</code>-terminated string.</p>

            <p><b>details</b>: The array contains exactly <code>5</code> pointer elements.</p>
            <p><b>warning</b>: The pointed-to strings are not owned by the array itself.</p>

            @see signal""",
            "<p><em>Clubs.</em></p>",
            "<p><strong>Diamonds.</strong></p>",
            "<p>Hearts.</p>",
            "<p><code>Spades.</code></p>",
            """
            <p>Sets a signal handler.</p>

            <p><b>retval</b>: NULL No previous handler.</p>

            @param sig [in] Signal number.
            @param func Pointer to the new handler.
            @return Pointer to the previous signal handler.
            @see process""",
            "FOO",
            """
            <p>Performs <i>processing</i> operation.</p>

            <p><b>warning</b>: Negative values may be unsupported.</p>

            @param a [in] Value to process.""",
            """
            <p>Opaque pointer to a <code>Foo</code> structure.</p>

            <ul><li>The structure's implementation may be hidden.</li></ul>
            <ul><li>The structure's implementation may not be hidden.</li></ul>""",
            """
            <p>Pointer to character data.</p>

            <p>Data may be <em>null.</em></p>
            <p>Data has a static lifetime.</p>

            <p><b>attention</b>: May be <code>NULL.</code></p>"""
        ));

        copiedComments = getCopiedComments("integrationTests.h", "NestedAnon.java");
        assertEquals(copiedComments, Set.of(
            """
            <p>Contains an anonymous nested structure.</p>

            <pre><code>
             NestedAnon value = {
                 .u = {
                     .l = 10,
                     .h = 100LL
                 }
             };
            </code></pre>""",
            "<p>Lower value.</p>",
            "<p>Higher value.</p>",
            "<p>Nested data.</p>"
        ));

        copiedComments = getCopiedComments("integrationTests.h", "example.java");
        assertEquals(copiedComments, Set.of(
            """
            <p>A callback description.</p>

            <p><b>param</b> <code>x</code> Callback input.</p>
            <p><b>return</b>: Callback output.</p>"""
        ));
    }

    private Set<String> getCopiedComments(String header, String outputFile)
            throws IOException {
        var output = getOutputFilePath("TestDoxygenComments-parse-" + header);
        var outputH = getInputFilePath(header);
        run(output, outputH.toString(), "--copy-comments", "doxygen");
        try {
            return TestRawComments.findCopiedComments(Files.readString(output.resolve(outputFile)));
        } finally {
            TestUtils.deleteDir(output);
        }
    }

    private static void assertEquals(Set<String> found, Set<String> expected) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(found);
        Set<String> extra = new HashSet<>(found);
        extra.removeAll(expected);
        missing.addAll(extra);

        if (!missing.isEmpty()) {
            System.out.printf("%s not equal to %s (difference: %s)%n", found, expected, missing);
            System.exit(1);
        }
    }
}
