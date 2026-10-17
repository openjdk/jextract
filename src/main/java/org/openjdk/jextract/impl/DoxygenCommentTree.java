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

import org.openjdk.jextract.clang.Comment;
import org.openjdk.jextract.clang.CommentKind;
import org.openjdk.jextract.clang.InlineCommandRenderKind;
import org.openjdk.jextract.clang.ParamPassDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DoxygenCommentTree {
    private DoxygenCommentTree() {
    }

    public static FullCommentNode parse(Comment comment) {
        if (comment.kind() == CommentKind.FullComment) {
            return visitFullComment(comment);
        }
        throw new IllegalArgumentException("Expected FullComment, got " + comment.kind());
    }

    private static FullCommentNode visitFullComment(Comment fullComment) {
        List<BlockContentCommentNode> blocks = new ArrayList<>(fullComment.getNumChildren());

        for (Comment child : fullComment) {
            BlockContentCommentNode block = switch (child.kind()) {
                case BlockCommand -> visitBlockCommand(child);
                case Paragraph -> visitParagraph(child);
                case ParamCommand -> visitParamCommand(child);
                case VerbatimBlockCommand -> visitVerbatimBlockCommand(child);
                case VerbatimLine -> visitVerbatimLine(child);
                default -> throw new AssertionError("Expected block content, got " + child.kind());
            };
            blocks.add(block);
        }

        return new FullCommentNode(blocks);
    }

    private static BlockCommandCommentNode visitBlockCommand(Comment blockCommand) {
        String commandName = blockCommand.getCommandName();
        ParagraphCommentNode paragraph = visitParagraph(blockCommand.getParagraph());

        int numArgs = blockCommand.getNumArgs();
        List<String> args = new ArrayList<>(numArgs);
        for (int i = 0; i < numArgs; i++) {
            args.add(blockCommand.getArgText(i));
        }

        return new BlockCommandCommentNode(commandName, args, paragraph);
    }

    private static ParagraphCommentNode visitParagraph(Comment paragraph) {
        List<InlineContentCommentNode> content = new ArrayList<>(paragraph.getNumChildren());

        for (Comment child : paragraph) {
            InlineContentCommentNode inlineContent = switch (child.kind()) {
                case HTMLEndTag -> visitHTMLEndTag(child);
                case HTMLStartTag -> visitHTMLStartTag(child);
                case InlineCommand -> visitInlineCommand(child);
                case Text -> visitText(child);
                default -> throw new AssertionError("Expected inline content, got " + child.kind());
            };
            content.add(inlineContent);
        }

        return new ParagraphCommentNode(content, paragraph.isWhitespace());
    }

    private static HTMLEndTagCommentNode visitHTMLEndTag(Comment htmlEndTag) {
        boolean hasTrailingNewline = htmlEndTag.hasTrailingNewline();
        String tagName = htmlEndTag.getTagName();

        return new HTMLEndTagCommentNode(hasTrailingNewline, tagName);
    }

    private static HTMLStartTagCommentNode visitHTMLStartTag(Comment htmlStartTag) {
        boolean hasTrailingNewline = htmlStartTag.hasTrailingNewline();
        String tagName = htmlStartTag.getTagName();
        boolean isSelfClosing = htmlStartTag.isSelfClosing();

        int numAttrs = htmlStartTag.getNumAttrs();
        Map<String, String> attributes = new HashMap<>(numAttrs);
        for (int i = 0; i < numAttrs; i++) {
            String attrName = htmlStartTag.getAttrName(i);
            String attrValue = htmlStartTag.getAttrValue(i);
            attributes.put(attrName, attrValue);
        }

        return new HTMLStartTagCommentNode(hasTrailingNewline, tagName, attributes, isSelfClosing);
    }

    private static InlineCommandCommentNode visitInlineCommand(Comment inlineCommand) {
        boolean hasTrailingNewline = inlineCommand.hasTrailingNewline();
        String commandName = inlineCommand.getCommandName();
        InlineCommandRenderKind renderKind = inlineCommand.getRenderKind();

        int numArgs = inlineCommand.getNumArgs();
        List<String> args = new ArrayList<>(numArgs);
        for (int i = 0; i < numArgs; i++) {
            args.add(inlineCommand.getArgText(i));
        }

        return new InlineCommandCommentNode(hasTrailingNewline, commandName, renderKind, args);
    }

    private static TextCommentNode visitText(Comment text) {
        boolean hasTrailingNewline = text.hasTrailingNewline();
        String textText = text.getText();

        return new TextCommentNode(hasTrailingNewline, textText);
    }

    private static ParamCommandCommentNode visitParamCommand(Comment paramCommand) {
        String commandName = paramCommand.getCommandName();
        ParagraphCommentNode paragraph = visitParagraph(paramCommand.getParagraph());
        ParamPassDirection direction = paramCommand.getDirection();
        boolean isDirectionExplicit = paramCommand.isDirectionExplicit();
        String paramName = paramCommand.getParamName();
        int paramIndex = paramCommand.getParamIndex();
        boolean isParamIndexValid = paramCommand.isParamIndexValid();

        int numArgs = paramCommand.getNumArgs();
        List<String> args = new ArrayList<>(numArgs);
        for (int i = 0; i < numArgs; i++) {
            args.add(paramCommand.getArgText(i));
        }

        return new ParamCommandCommentNode(commandName, args, paragraph, direction, isDirectionExplicit, paramName, paramIndex, isParamIndexValid);
    }

    private static VerbatimBlockCommentNode visitVerbatimBlockCommand(Comment verbatimBlockCommand) {
        String commandName = verbatimBlockCommand.getCommandName();
        ParagraphCommentNode paragraph = visitParagraph(verbatimBlockCommand.getParagraph());

        int numArgs = verbatimBlockCommand.getNumArgs();
        List<String> args = new ArrayList<>(numArgs);
        for (int i = 0; i < numArgs; i++) {
            args.add(verbatimBlockCommand.getArgText(i));
        }

        List<VerbatimBlockLineCommentNode> lines = new ArrayList<>(verbatimBlockCommand.getNumChildren());
        for (Comment child : verbatimBlockCommand) {
            if (child.kind() != CommentKind.VerbatimBlockLine) {
                throw new AssertionError();
            }
            lines.add(visitVerbatimBlockLine(child));
        }

        return new VerbatimBlockCommentNode(commandName, args, paragraph, lines);
    }

    private static VerbatimBlockLineCommentNode visitVerbatimBlockLine(Comment verbatimBlockLine) {
        String text = verbatimBlockLine.getText();

        return new VerbatimBlockLineCommentNode(text);
    }

    private static VerbatimLineCommentNode visitVerbatimLine(Comment verbatimLine) {
        String commandName = verbatimLine.getCommandName();
        ParagraphCommentNode paragraph = visitParagraph(verbatimLine.getParagraph());
        String text = verbatimLine.getText();

        int numArgs = verbatimLine.getNumArgs();
        List<String> args = new ArrayList<>(numArgs);
        for (int i = 0; i < numArgs; i++) {
            args.add(verbatimLine.getArgText(i));
        }

        return new VerbatimLineCommentNode(commandName, args, paragraph, text);
    }

    /*
     * Follows the inheritance structure of `clang::comments::Comment`. This is the true
     * structure; the C API provides a simplified view. The below code essentially rebuilds
     * the C++ structure from the C API.
     */
    public sealed abstract static class CommentNode {
    }

    public sealed abstract static class BlockContentCommentNode extends CommentNode {
    }

    public sealed static class BlockCommandCommentNode extends BlockContentCommentNode {
        private final String commandName;
        private final List<String> args;
        private final ParagraphCommentNode paragraph;

        public BlockCommandCommentNode(String commandName, List<String> args, ParagraphCommentNode paragraph) {
            this.commandName = commandName;
            this.args = args;
            this.paragraph = paragraph;
        }

        public String getCommandName() {
            return commandName;
        }

        public List<String> getArgs() {
            return Collections.unmodifiableList(args);
        }

        public ParagraphCommentNode getParagraph() {
            return paragraph;
        }

        @Override
        public String toString() {
            return "BlockCommandCommentNode[commandName=%s, args=%s, paragraph=%s]".formatted(commandName, args, paragraph);
        }
    }

    public static final class ParamCommandCommentNode extends BlockCommandCommentNode {
        private final ParamPassDirection direction;
        private final boolean isDirectionExplicit;
        private final String paramName;
        private final int paramIndex;
        private final boolean isParamIndexValid;

        public ParamCommandCommentNode(String commandName, List<String> args, ParagraphCommentNode paragraph, ParamPassDirection direction, boolean isDirectionExplicit, String paramName, int paramIndex, boolean isParamIndexValid) {
            super(commandName, args, paragraph);
            this.direction = direction;
            this.isDirectionExplicit = isDirectionExplicit;
            this.paramName = paramName;
            this.paramIndex = paramIndex;
            this.isParamIndexValid = isParamIndexValid;
        }

        public ParamPassDirection getDirection() {
            return direction;
        }

        public boolean isDirectionExplicit() {
            return isDirectionExplicit;
        }

        public String getParamName() {
            return paramName;
        }

        public int getParamIndex() {
            return paramIndex;
        }

        public boolean isParamIndexValid() {
            return isParamIndexValid;
        }

        @Override
        public String toString() {
            return "ParamCommandCommentNode[commandName=%s, args=%s, paragraph=%s, direction=%s, isDirectionExplicit=%s, paramName=%s, paramIndex=%s, isParamIndexValid=%s]".formatted(super.commandName, super.args, super.paragraph, direction, isDirectionExplicit, paramName, paramIndex, isParamIndexValid);
        }
    }

    public static final class VerbatimBlockCommentNode extends BlockCommandCommentNode {
        private final List<VerbatimBlockLineCommentNode> lines;

        public VerbatimBlockCommentNode(String commandName, List<String> args, ParagraphCommentNode paragraph, List<VerbatimBlockLineCommentNode> lines) {
            super(commandName, args, paragraph);
            this.lines = lines;
        }

        public List<VerbatimBlockLineCommentNode> getLines() {
            return Collections.unmodifiableList(lines);
        }

        @Override
        public String toString() {
            return "VerbatimBlockCommentNode[commandName=%s, args=%s, paragraph=%s, lines=%s]".formatted(super.commandName, super.args, super.paragraph, lines);
        }
    }

    public static final class VerbatimLineCommentNode extends BlockCommandCommentNode {
        private final String text;

        public VerbatimLineCommentNode(String commandName, List<String> args, ParagraphCommentNode paragraph, String text) {
            super(commandName, args, paragraph);
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "VerbatimLineCommentNode[commandName=%s, args=%s, paragraph=%s, text=%s]".formatted(super.commandName, super.args, super.paragraph, text);
        }
    }

    public static final class ParagraphCommentNode extends BlockContentCommentNode {
        private final List<InlineContentCommentNode> content;
        private final boolean isWhitespace;

        public ParagraphCommentNode(List<InlineContentCommentNode> content, boolean isWhitespace) {
            this.content = content;
            this.isWhitespace = isWhitespace;
        }

        public List<InlineContentCommentNode> getContent() {
            return Collections.unmodifiableList(content);
        }

        public boolean isWhitespace() {
            return isWhitespace;
        }

        @Override
        public String toString() {
            return "ParagraphCommentNode[content=%s, isWhitespace=%s]".formatted(content, isWhitespace);
        }
    }

    public static final class FullCommentNode extends CommentNode {
        private final List<BlockContentCommentNode> blocks;

        public FullCommentNode(List<BlockContentCommentNode> blocks) {
            this.blocks = blocks;
        }

        public List<BlockContentCommentNode> getBlocks() {
            return Collections.unmodifiableList(blocks);
        }

        @Override
        public String toString() {
            return "FullCommentNode[blocks=%s]".formatted(blocks);
        }
    }

    public sealed abstract static class InlineContentCommentNode extends CommentNode {
        private final boolean hasTrailingNewline;

        public InlineContentCommentNode(boolean hasTrailingNewline) {
            this.hasTrailingNewline = hasTrailingNewline;
        }

        public boolean hasTrailingNewline() {
            return hasTrailingNewline;
        }

        @Override
        public String toString() {
            return "InlineContentCommentNode[hasTrailingNewline=%s]".formatted(hasTrailingNewline);
        }
    }

    public sealed abstract static class HTMLTagCommentNode extends InlineContentCommentNode {
        private final String tagName;

        public HTMLTagCommentNode(boolean hasTrailingNewline, String tagName) {
            super(hasTrailingNewline);
            this.tagName = tagName;
        }

        public String getTagName() {
            return tagName;
        }

        @Override
        public String toString() {
            return "HTMLTagCommentNode[hasTrailingNewline=%s, tagName=%s]".formatted(super.hasTrailingNewline, tagName);
        }
    }

    public static final class HTMLStartTagCommentNode extends HTMLTagCommentNode {
        private final Map<String, String> attributes;
        private final boolean isSelfClosing;

        public HTMLStartTagCommentNode(boolean hasTrailingNewline, String tagName, Map<String, String> attributes, boolean isSelfClosing) {
            super(hasTrailingNewline, tagName);
            this.attributes = attributes;
            this.isSelfClosing = isSelfClosing;
        }

        public Map<String, String> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }

        public boolean isSelfClosing() {
            return isSelfClosing;
        }

        @Override
        public String toString() {
            return "HTMLStartTagCommentNode[hasTrailingNewline=%s, tagName=%s, attributes=%s, isSelfClosing=%s]".formatted(hasTrailingNewline(), super.tagName, attributes, isSelfClosing);
        }
    }

    public static final class HTMLEndTagCommentNode extends HTMLTagCommentNode {
        public HTMLEndTagCommentNode(boolean hasTrailingNewline, String tagName) {
            super(hasTrailingNewline, tagName);
        }

        @Override
        public String toString() {
            return "HTMLEndTagCommentNode[hasTrailingNewline=%s, tagName=%s]".formatted(hasTrailingNewline(), super.tagName);
        }
    }

    public static final class InlineCommandCommentNode extends InlineContentCommentNode {
        private final String commandName;
        private final InlineCommandRenderKind renderKind;
        private final List<String> args;

        public InlineCommandCommentNode(boolean hasTrailingNewline, String commandName, InlineCommandRenderKind renderKind, List<String> args) {
            super(hasTrailingNewline);
            this.commandName = commandName;
            this.renderKind = renderKind;
            this.args = args;
        }

        public String getCommandName() {
            return commandName;
        }

        public InlineCommandRenderKind getRenderKind() {
            return renderKind;
        }

        public List<String> getArgs() {
            return Collections.unmodifiableList(args);
        }

        @Override
        public String toString() {
            return "InlineCommandCommentNode[hasTrailingNewline=%s, commandName=%s, renderKind=%s, args=%s]".formatted(super.hasTrailingNewline, commandName, renderKind, args);
        }
    }

    public static final class TextCommentNode extends InlineContentCommentNode {
        private final String text;

        public TextCommentNode(boolean hasTrailingNewline, String text) {
            super(hasTrailingNewline);
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "TextCommentNode[hasTrailingNewline=%s, text=%s]".formatted(super.hasTrailingNewline, text);
        }
    }

    public static final class VerbatimBlockLineCommentNode extends CommentNode {
        private final String text;

        public VerbatimBlockLineCommentNode(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "VerbatimBlockLineCommentNode[text=%s]".formatted(text);
        }
    }
}
