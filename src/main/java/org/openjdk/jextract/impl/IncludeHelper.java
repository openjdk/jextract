/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import org.openjdk.jextract.Declaration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class IncludeHelper {

    public enum IncludeKind {
        CONSTANT,
        VAR,
        FUNCTION,
        TYPEDEF,
        STRUCT,
        UNION;

        public String optionName() {
            return "include-" + name().toLowerCase();
        }

        static IncludeKind fromDeclaration(Declaration d) {
            if (d instanceof Declaration.Constant) {
                return CONSTANT;
            } else if (d instanceof Declaration.Variable) {
                return VAR;
            } else if (d instanceof Declaration.Function) {
                return FUNCTION;
            } else if (d instanceof Declaration.Typedef) {
                return TYPEDEF;
            } else if (d instanceof Declaration.Scoped scoped) {
                return fromScoped(scoped);
            } else {
                throw new IllegalStateException("Cannot get here!");
            }
        }

        static IncludeKind fromScoped(Declaration.Scoped scoped) {
            return switch (scoped.kind()) {
                case STRUCT -> IncludeKind.STRUCT;
                case UNION ->  IncludeKind.UNION;
                default -> throw new IllegalStateException("Cannot get here!");
            };
        }
    }

    private final EnumMap<IncludeKind, Set<String>> includesSymbolNamesByKind = new EnumMap<>(IncludeKind.class);
    private final Set<Declaration> usedDeclarations = new HashSet<>();
    public String dumpIncludesFile;

    public void addSymbol(IncludeKind kind, String symbolName) {
        Set<String> names = includesSymbolNamesByKind.computeIfAbsent(kind, (_unused) -> new HashSet<>());
        names.add(symbolName);
    }

    public boolean isIncluded(Declaration.Variable variable) {
        return checkIncludedAndAddIfNeeded(IncludeKind.VAR, variable);
    }

    public boolean isIncluded(Declaration.Function function) {
        return checkIncludedAndAddIfNeeded(IncludeKind.FUNCTION, function);
    }

    public boolean isIncluded(Declaration.Constant constant) {
        return checkIncludedAndAddIfNeeded(IncludeKind.CONSTANT, constant);
    }

    public boolean isIncluded(Declaration.Typedef typedef) {
        return checkIncludedAndAddIfNeeded(IncludeKind.TYPEDEF, typedef);
    }

    public boolean isIncluded(Declaration.Scoped scoped) {
        return checkIncludedAndAddIfNeeded(IncludeKind.fromScoped(scoped), scoped);
    }

    private boolean checkIncludedAndAddIfNeeded(IncludeKind kind, Declaration declaration) {
        boolean included = isIncludedInternal(kind, declaration);
        if (included && dumpIncludesFile != null) {
            usedDeclarations.add(declaration);
        }
        return included;
    }

    private boolean isIncludedInternal(IncludeKind kind, Declaration declaration) {
        if (!isEnabled()) {
            return true;
        } else {
            Set<String> names = includesSymbolNamesByKind.computeIfAbsent(kind, (_unused) -> new HashSet<>());
            return names.contains(declaration.name());
        }
    }

    public boolean isEnabled() {
        return includesSymbolNamesByKind.size() > 0;
    }

    public void dumpIncludes() {
        try (var writer = Files.newBufferedWriter(Path.of(dumpIncludesFile), StandardOpenOption.CREATE)) {
            Map<Path, Set<Declaration>> declsByPath = usedDeclarations.stream()
                    .collect(Collectors.groupingBy(d -> d.pos().path(),
                            () -> new TreeMap<>(Path::compareTo),
                            Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Declaration::name)))));
            String lineSep = "";
            for (Map.Entry<Path, Set<Declaration>> pathEntries : declsByPath.entrySet()) {
                writer.append(lineSep);
                writer.append("#### Extracted from: " + pathEntries.getKey().toString() + "\n\n");
                Map<IncludeKind, List<Declaration>> declsByKind = pathEntries.getValue().stream()
                        .collect(Collectors.groupingBy(IncludeKind::fromDeclaration));
                int maxLengthOptionCol = pathEntries.getValue().stream().mapToInt(d -> d.name().length()).max().getAsInt();
                maxLengthOptionCol += 2; // --
                maxLengthOptionCol += IncludeKind.FUNCTION.optionName().length(); // max option name
                maxLengthOptionCol += 1; // space
                for (Map.Entry<IncludeKind, List<Declaration>> kindEntries : declsByKind.entrySet()) {
                    for (Declaration d : kindEntries.getValue()) {
                        writer.append(String.format("%-" + maxLengthOptionCol + "s %s",
                                "--" + kindEntries.getKey().optionName() + " " + d.name(),
                                       "# header: " + pathEntries.getKey() + "\n"));
                    }
                }
                lineSep = "\n";
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
