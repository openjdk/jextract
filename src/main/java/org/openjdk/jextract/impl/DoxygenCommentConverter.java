/*
 *  Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

import org.openjdk.jextract.JextractTool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DoxygenCommentConverter {
    private final Map<Integer, String> parameterNames;

    DoxygenCommentConverter(Map<Integer, String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    public String toJavaDoc(DoxygenCommentTree.FullCommentNode fullCommentNode) {
        JavaDocBuilder builder = new JavaDocBuilder();
        visitFullCommentNode(builder, fullCommentNode);
        return finish(builder.build());
    }

    private static String finish(String commentText) {
        return commentText
            // sanitize one final time to be safe
            .replace("\\u", "&#92;u")
            .replace("*/", "*@/")
            .lines()
            .map(line -> " * " + line + "\n")
            .collect(Collectors.joining());
    }

    private void visitFullCommentNode(JavaDocBuilder builder, DoxygenCommentTree.FullCommentNode fullCommentNode) {
        for (DoxygenCommentTree.BlockContentCommentNode block : fullCommentNode.getBlocks()) {
            switch (block) {
                case DoxygenCommentTree.ParagraphCommentNode paragraphCommentNode -> {
                    if (paragraphCommentNode.isWhitespace()) {
                        break;
                    }
                    builder.appendMain("<p>");
                    StringBuilder paragraphBuilder = new StringBuilder();
                    visitParagraphNode(paragraphBuilder, paragraphCommentNode);
                    builder.appendMain(paragraphBuilder.toString().strip());
                    builder.appendMain("</p>\n");
                }
                case DoxygenCommentTree.ParamCommandCommentNode paramCommandCommentNode -> visitParamCommandNode(builder, paramCommandCommentNode);
                case DoxygenCommentTree.VerbatimBlockCommentNode verbatimBlockCommentNode -> visitVerbatimBlockCommandNode(builder.getMain(), verbatimBlockCommentNode);
                case DoxygenCommentTree.VerbatimLineCommentNode verbatimLineCommentNode -> visitVerbatimLineNode(builder.getMain(), verbatimLineCommentNode);
                case DoxygenCommentTree.BlockCommandCommentNode blockCommandCommentNode -> visitBlockCommandNode(builder, blockCommandCommentNode);
            }
        }
    }

    private void visitParagraphNode(StringBuilder builder, DoxygenCommentTree.ParagraphCommentNode paragraphCommentNode) {
        if (paragraphCommentNode.isWhitespace()) {
            return;
        }
        List<DoxygenCommentTree.InlineContentCommentNode> content = paragraphCommentNode.getContent();
        for (DoxygenCommentTree.InlineContentCommentNode inlineContent : content) {
            switch (inlineContent) {
                case DoxygenCommentTree.HTMLStartTagCommentNode htmlStartTagCommentNode -> visitHTMLStartTagNode(builder, htmlStartTagCommentNode);
                case DoxygenCommentTree.HTMLEndTagCommentNode htmlEndTagCommentNode -> visitHTMLEndTagNode(builder, htmlEndTagCommentNode);
                case DoxygenCommentTree.InlineCommandCommentNode inlineCommandCommentNode -> visitInlineCommandNode(builder, inlineCommandCommentNode);
                case DoxygenCommentTree.TextCommentNode textCommentNode -> visitTextNode(builder, textCommentNode);
            }
        }
    }

    private void visitHTMLStartTagNode(StringBuilder builder, DoxygenCommentTree.HTMLStartTagCommentNode htmlStartTagCommentNode) {
        String tagName = upgradeHTML(htmlStartTagCommentNode.getTagName());
        builder.append("<%s".formatted(tagName));
        htmlStartTagCommentNode.getAttributes().forEach((name, value) -> {
            // value can be null when attribute has no value (e.g. binary attribute)
            String valueString = value == null ? "" : "=\"%s\"".formatted(value.replace("\"", "&quot;"));
            builder.append(" %s%s".formatted(name, valueString));
        });
        if (htmlStartTagCommentNode.isSelfClosing()) {
            builder.append("/>");
        } else {
            builder.append(">");
        }
    }

    private void visitHTMLEndTagNode(StringBuilder builder, DoxygenCommentTree.HTMLEndTagCommentNode htmlEndTagCommentNode) {
        String tagName = upgradeHTML(htmlEndTagCommentNode.getTagName());
        builder.append("</%s>".formatted(tagName));
    }

    private void visitInlineCommandNode(StringBuilder builder, DoxygenCommentTree.InlineCommandCommentNode inlineCommandCommentNode) {
        String commandName = inlineCommandCommentNode.getCommandName();
        List<String> args = inlineCommandCommentNode.getArgs();
        if (args.isEmpty()) {
            if (JextractTool.DEBUG) {
                System.out.printf("Expected at least one argument for inline command (%s)%n", commandName);
            }
            return;
        }
        String firstArg = args.getFirst();

        switch (commandName) {
            case "b" -> appendInlineCommandAsHTML(builder, "strong", firstArg);
            case "a", "e", "em" -> appendInlineCommandAsHTML(builder, "em", firstArg);
            case "c", "p" -> appendInlineCommandAsHTML(builder, "code", firstArg);
            // ref takes a second optional argument, but clang does not support inline commands with multiple arguments
            case "ref" -> appendInlineCommandAsHTML(builder, "code", firstArg);
            case "anchor" -> {}

            default -> {
                switch (inlineCommandCommentNode.getRenderKind()) {
                    case Anchor -> {}
                    case Bold -> appendInlineCommandAsHTML(builder, "strong", firstArg);
                    case Emphasized -> appendInlineCommandAsHTML(builder, "em", firstArg);
                    case Monospaced -> appendInlineCommandAsHTML(builder, "code", firstArg);
                    case Normal -> builder.append("%s".formatted(firstArg));
                }
            }
        }
    }

    private void visitTextNode(StringBuilder builder, DoxygenCommentTree.TextCommentNode textCommentNode) {
        builder.append(ClassSourceBuilder.sanitizeCommentText(textCommentNode.getText()));
        if (textCommentNode.hasTrailingNewline()) {
            builder.append("\n");
        }
    }

    private void visitParamCommandNode(JavaDocBuilder builder, DoxygenCommentTree.ParamCommandCommentNode paramCommandCommentNode) {
        boolean isParamIndexValid = paramCommandCommentNode.isParamIndexValid();
        if (!isParamIndexValid && JextractTool.DEBUG) {
            System.out.printf("Found invalid parameter command (%s)%n", paramCommandCommentNode.getParamName());
        }
        String cParamName = paramCommandCommentNode.getParamName();
        if (cParamName == null) {
            if (JextractTool.DEBUG) {
                System.out.printf("Found parameter command without name (%s)%n", paramCommandCommentNode);
            }
            return;
        }
        String javaParamName;
        if (isParamIndexValid && parameterNames != null) {
            javaParamName = parameterNames.get(paramCommandCommentNode.getParamIndex());
        } else {
            javaParamName = null;
        }
        String directionString;
        if (!paramCommandCommentNode.isDirectionExplicit()) {
            directionString = "";
        } else {
            directionString = switch (paramCommandCommentNode.getDirection()) {
                case In -> "[in] ";
                case Out -> "[out] ";
                case InOut -> "[in,out] ";
            };
        }
        StringBuilder paragraphBuilder = new StringBuilder();
        visitParagraphNode(paragraphBuilder, paramCommandCommentNode.getParagraph());
        String paragraph = paragraphBuilder.toString().strip();

        if (javaParamName != null) {
            builder.getBlockTags().append("@param %s %s%s%n".formatted(javaParamName, directionString, paragraph));
        } else {
            builder.getEndMain().append("<p><b>param</b> <code>%s</code> %s%s</p>%n".formatted(ClassSourceBuilder.sanitizeCommentText(cParamName), directionString, paragraph));
        }
    }

    private void visitVerbatimBlockCommandNode(StringBuilder builder, DoxygenCommentTree.VerbatimBlockCommentNode verbatimBlockCommentNode) {
        // use older <pre> and <code> because snippets do not support escaping
        builder.append("<pre><code>\n");
        for (DoxygenCommentTree.VerbatimBlockLineCommentNode verbatimBlockLineCommentNode : verbatimBlockCommentNode.getLines()) {
            visitVerbatimBlockLineCommentNode(builder, verbatimBlockLineCommentNode);
            builder.append("\n");
        }
        builder.append("</code></pre>\n");
    }

    private void visitVerbatimBlockLineCommentNode(StringBuilder builder, DoxygenCommentTree.VerbatimBlockLineCommentNode verbatimBlockLineCommentNode) {
        builder.append(ClassSourceBuilder.sanitizeCommentText(verbatimBlockLineCommentNode.getText()));
    }

    private void visitVerbatimLineNode(StringBuilder builder, DoxygenCommentTree.VerbatimLineCommentNode verbatimLineCommentNode) {
        String commandName = verbatimLineCommentNode.getCommandName();
        switch (commandName) {
            // these are commands that identify the declaration being documented, should not be rendered
            case "var", "fn", "typedef", "struct", "union", "enum", "def" -> {}
            default -> builder.append("<code>%s</code>%n".formatted(ClassSourceBuilder.sanitizeCommentText(verbatimLineCommentNode.getText())));
        }
    }

    private void visitBlockCommandNode(JavaDocBuilder builder, DoxygenCommentTree.BlockCommandCommentNode blockCommandCommentNode) {
        List<String> args = blockCommandCommentNode.getArgs();
        String argsString;
        if (args.isEmpty()) {
            argsString = "";
        } else {
            argsString = String.join(" ", args) + " ";
        }
        argsString = argsString.stripLeading();
        StringBuilder paragraphBuilder = new StringBuilder();
        visitParagraphNode(paragraphBuilder, blockCommandCommentNode.getParagraph());
        String paragraph = paragraphBuilder.toString().strip();

        String commandName = blockCommandCommentNode.getCommandName();
        switch (commandName) {
            case "param" -> appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), "param", argsString, paragraph);
            case "return", "returns", "result" -> appendFunctionTag(builder, "return", argsString, paragraph);
            case "throw", "throws", "exception" -> appendFunctionTag(builder, "throws", argsString, paragraph);
            case "see", "sa" -> appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), "see", argsString, paragraph);
            case "deprecated" -> appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), "deprecated", argsString, paragraph);
            case "since" -> appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), "since", argsString, paragraph);
            case "author", "authors" -> appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), "author", argsString, paragraph);
            case "noop" -> appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), "hidden", argsString, paragraph);

            case "brief", "short" -> builder.appendBeginMain("<p>%s</p>%n".formatted(paragraph));

            case "par" -> builder.appendEndMain("<p>%s</p>%n".formatted(paragraph));
            // workaround: create unordered list for each arg
            case "arg", "li" -> builder.appendEndMain("<ul><li>%s</li></ul>%n".formatted(paragraph));

            default -> builder.appendEndMain("<p><b>%s</b>: %s%s</p>%n".formatted(commandName, argsString, paragraph));
        }
    }

    private static String upgradeHTML(String htmlTag) {
        return switch (htmlTag) {
            case "tt" -> "code";
            case "strike" -> "s";
            default -> htmlTag;
        };
    }

    private void appendFunctionTag(JavaDocBuilder builder, String tag, String argsString, String paragraph) {
        if (parameterNames != null) {
            appendBlockCommandAsJavaDocBlockTag(builder.getBlockTags(), tag, argsString, paragraph);
        } else {
            builder.appendEndMain("<p><b>%s</b>: %s%s</p>%n".formatted(tag, ClassSourceBuilder.sanitizeCommentText(argsString), paragraph));
        }
    }

    private static void appendInlineCommandAsHTML(StringBuilder builder, String htmlTag, String arg) {
        builder.append("<%1$s>%2$s</%1$s>".formatted(htmlTag, ClassSourceBuilder.sanitizeCommentText(arg)));
    }

    private static void appendBlockCommandAsJavaDocBlockTag(StringBuilder builder, String javaDocTag, String argsString, String paragraph) {
        builder.append("@%s %s%s%n".formatted(javaDocTag, argsString, paragraph));
    }
}
