package org.openjdk.jextract.test.toolprovider.dumpIncludes;

import testlib.TestUtils;
import org.testng.annotations.Test;
import testlib.JextractToolRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

public class TestNormalizedPathOutput extends JextractToolRunner {
    @Test
    public void testNormalizedPathOutput() throws IOException {
        Path filterOutput = getOutputFilePath("TestNormalizedPathOutput_output");
        try {
            Files.createDirectory(filterOutput);
            Path includes = filterOutput.resolve("test.conf");
            Path filterH = getInputFilePath("test.h");
            runNoOuput("--dump-includes", includes.toString(), filterH.toString()).checkSuccess();
            List<String> includeLines = Files.readAllLines(includes);
            List<String> heading = includeLines.stream().filter(line -> line.startsWith("#### Extracted from")).toList();
            assertEquals(heading.size(), 1);
            assertTrue(heading.getFirst().endsWith("/dumpIncludes/header.h"));
            List<String> filter = includeLines.stream().filter(line -> line.startsWith("--include-constant")).toList();
            assertEquals(filter.size(), 1);
            assertTrue(filter.getFirst().endsWith("/dumpIncludes/header.h"));
        } finally {
            TestUtils.deleteDir(filterOutput);
        }
    }
}
