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
package org.openjdk.jextract.test.toolprovider;

import org.testng.annotations.Test;
import testlib.JextractToolRunner;
import testlib.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

public class VlaFunctionParameterTest extends JextractToolRunner {

    private static final Pattern INNER_CLASS_MATCHER = Pattern.compile("(?s) static class \\w+ \\{(.*?)}");
    private static final Pattern DESCRIPTOR_MATCHER = Pattern.compile("(?s) static final FunctionDescriptor DESC = FunctionDescriptor\\.of\\((.*?)\\)");

    @Test
    public void testVlaFunctionParameter() throws IOException {
        Set<String> descriptors = getDescriptors("vlaFunctionParameter.h", "vlaFunctionParameter_h.java");
        assertEquals(1, descriptors.size());
        String descriptor = descriptors.iterator().next();
        assertEquals("vlaFunctionParameter_h.C_INT, vlaFunctionParameter_h.C_INT, vlaFunctionParameter_h.C_POINTER", descriptor);
    }

    private Set<String> getDescriptors(String header, String outputFile) throws IOException {
        var output = getOutputFilePath("VlaFunctionParameterTest-parse-" + header);
        var outputH = getInputFilePath(header);
        run(output, outputH.toString());
        try {
            return findDescriptors(Files.readString(output.resolve(outputFile)));
        } finally {
            TestUtils.deleteDir(output);
        }
    }

    // find function descriptors from the given the source content
    private static Set<String> findDescriptors(String content) {
        var innerClassMatcher = INNER_CLASS_MATCHER.matcher(content);
        Set<String> strings = new HashSet<>();
        while (innerClassMatcher.find()) {
            // inner class text is matched in group 1
            String rawInnerClass = innerClassMatcher.group(1);
            Matcher descriptorMatcher = DESCRIPTOR_MATCHER.matcher(rawInnerClass);
            while (descriptorMatcher.find()) {
                String rawDescriptor = descriptorMatcher.group(1);
                strings.add(rawDescriptor.strip().replaceAll(",\\s*", ", "));
            }
        }
        return strings;
    }
}
