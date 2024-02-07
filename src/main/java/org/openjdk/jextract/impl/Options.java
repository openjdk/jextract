/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jextract.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Options {

    // The args for parsing C
    public final List<String> clangArgs;
    // The list of library names
    public final List<Library> libraries;
    // target package
    public final String targetPackage;
    // output directory
    public final String outputDir;
    public final IncludeHelper includeHelper;

    private Options(List<String> clangArgs, List<Library> libraries,
                    String targetPackage, String outputDir, IncludeHelper includeHelper) {
        this.clangArgs = clangArgs;
        this.libraries = libraries;
        this.targetPackage = targetPackage;
        this.outputDir = outputDir;
        this.includeHelper = includeHelper;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> clangArgs;
        private final List<Library> libraries;
        private String targetPackage;
        private String outputDir;
        private final IncludeHelper includeHelper = new IncludeHelper();

        public Builder() {
            this.clangArgs = new ArrayList<>();
            this.libraries = new ArrayList<>();
            this.targetPackage = "";
            this.outputDir = ".";
        }

        public Options build() {
            return new Options(
                    Collections.unmodifiableList(clangArgs),
                    Collections.unmodifiableList(libraries),
                    targetPackage, outputDir, includeHelper
            );
        }

        public void addClangArg(String arg) {
            clangArgs.add(arg);
        }

        public void addLibrary(Library library) {
            libraries.add(library);
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }

        public void setTargetPackage(String pkg) {
            this.targetPackage = pkg;
        }

        public void setDumpIncludeFile(String dumpIncludesFile) {
            includeHelper.dumpIncludesFile = dumpIncludesFile;
        }

        public void addIncludeSymbol(IncludeHelper.IncludeKind kind, String symbolName) {
            includeHelper.addSymbol(kind, symbolName);
        }
    }

    /**
     * A record describing a shared library.
     *
     * @param libSpec the library specification (either a name or a path, see below)
     * @param specKind the library specification kind (e.g. a name or a path)
     * @param lookupKind the lookup associated with the library (e.g. libraryLookup vs. loaderLookup)
     */
    public record Library(String libSpec, SpecKind specKind, LookupKind lookupKind) {

        enum SpecKind {
            NAME,
            PATH;

            static SpecKind parse(String value) {
                return switch (value) {
                    case "name" -> NAME;
                    case "path" -> PATH;
                    default -> throw new IllegalArgumentException();
                };
            }
        }
        enum LookupKind {
            LOADER_LOOKUP,
            LIBRARY_LOOKUP;

            static LookupKind parse(String value) {
                return switch (value) {
                    case "loaderLookup" -> LOADER_LOOKUP;
                    case "libraryLookup" -> LIBRARY_LOOKUP;
                    default -> throw new IllegalArgumentException();
                };
            }
        }

        public static Library parse(String optionString) {
            String[] parts = optionString.split(",");
            String libSpec = parts[0];
            SpecKind specKind = null;
            LookupKind lookupKind = null;
            if (parts.length > 1) {
                for (int i = 1 ; i < parts.length ; i++) {
                    String[] keyVal = parts[i].split("=");
                    if (keyVal.length != 2) {
                        throw new IllegalArgumentException();
                    }
                    String key = keyVal[0];
                    String val = keyVal[1];
                    if (specKind == null && key.equals("spec")) {
                        specKind = SpecKind.parse(val);
                    } else if (lookupKind == null && key.equals("lookup")) {
                        lookupKind = LookupKind.parse(val);
                    } else {
                        // wrong or repeated key
                        throw new IllegalArgumentException();
                    }
                }
            }
            if (specKind == null) {
                specKind = SpecKind.NAME;
            }
            if (lookupKind == null) {
                lookupKind = LookupKind.LIBRARY_LOOKUP;
            }
            return new Library(
                    libSpec, specKind == null ? SpecKind.NAME : specKind,
                    lookupKind == null ? LookupKind.LIBRARY_LOOKUP : lookupKind
            );
        }

        String toQuotedName() {
            return libSpec().replace("\\", "\\\\"); // double up slashes
        }
    }
}
