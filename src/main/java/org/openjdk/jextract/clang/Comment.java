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

package org.openjdk.jextract.clang;

import org.openjdk.jextract.clang.libclang.Index_h;

import java.lang.foreign.MemorySegment;
import java.util.Iterator;

public final class Comment extends ClangDisposable.Owned implements Iterable<Comment> {

    private final int kind0;
    private final CommentKind kind;

    Comment(MemorySegment segment, ClangDisposable owner) {
        super(segment, owner);
        kind0 = Index_h.clang_Comment_getKind(segment);
        kind = CommentKind.valueOf(kind0);
    }

    public boolean isWhitespace() {
        return Index_h.clang_Comment_isWhitespace(segment) != 0;
    }

    public boolean hasTrailingNewline() {
        return Index_h.clang_InlineContentComment_hasTrailingNewline(segment) != 0;
    }

    public int getNumChildren() {
        return Index_h.clang_Comment_getNumChildren(segment);
    }

    public Comment getChild(int childIdx) {
        var child = Index_h.clang_Comment_getChild(owner, segment, childIdx);
        return new Comment(child, owner);
    }

    @Override
    public Iterator<Comment> iterator() {
        return new Iterator<>() {
            private final int numChildren = getNumChildren();
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < numChildren;
            }

            @Override
            public Comment next() {
                return getChild(idx++);
            }
        };
    }

    public CommentKind kind() {
        return kind;
    }

    public int kind0() {
        return kind0;
    }

    // kind-specific functions

    public String getAsHTML() {
        if (kind != CommentKind.FullComment) {
            throw new IllegalCallerException();
        }
        var html = Index_h.clang_FullComment_getAsHTML(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(html);
    }

    public String getAsXML() {
        if (kind != CommentKind.FullComment) {
            throw new IllegalCallerException();
        }
        var xml = Index_h.clang_FullComment_getAsXML(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(xml);
    }

    public String getText() {
        var text = switch (kind) {
            case Text -> Index_h.clang_TextComment_getText(LibClang.STRING_ALLOCATOR, segment);
            case VerbatimLine -> Index_h.clang_VerbatimLineComment_getText(LibClang.STRING_ALLOCATOR, segment);
            case VerbatimBlockLine -> Index_h.clang_VerbatimBlockLineComment_getText(LibClang.STRING_ALLOCATOR, segment);
            default -> throw new IllegalCallerException();
        };
        return LibClang.CXStrToString(text);
    }

    public Comment getParagraph() {
        var paragraph = switch (kind) {
            case VerbatimLine, BlockCommand, VerbatimBlockCommand, ParamCommand -> Index_h.clang_BlockCommandComment_getParagraph(owner, segment);
            default -> throw new IllegalCallerException();
        };
        return new Comment(paragraph, owner);
    }

    public InlineCommandRenderKind getRenderKind() {
        if (kind != CommentKind.InlineCommand) {
            throw new IllegalCallerException();
        }
        return InlineCommandRenderKind.valueOf(Index_h.clang_InlineCommandComment_getRenderKind(segment));
    }

    public String getCommandName() {
        var name = switch (kind) {
            case InlineCommand -> Index_h.clang_InlineCommandComment_getCommandName(LibClang.STRING_ALLOCATOR, segment);
            case VerbatimLine, BlockCommand, VerbatimBlockCommand, ParamCommand -> Index_h.clang_BlockCommandComment_getCommandName(LibClang.STRING_ALLOCATOR, segment);
            default -> throw new IllegalCallerException();
        };
        return LibClang.CXStrToString(name);
    }

    public int getNumArgs() {
        return switch (kind) {
            case InlineCommand -> Index_h.clang_InlineCommandComment_getNumArgs(segment);
            case VerbatimLine, BlockCommand, VerbatimBlockCommand, ParamCommand -> Index_h.clang_BlockCommandComment_getNumArgs(segment);
            default -> throw new IllegalCallerException();
        };
    }

    public String getArgText(int argIdx) {
        var text = switch (kind) {
            case InlineCommand -> Index_h.clang_InlineCommandComment_getArgText(LibClang.STRING_ALLOCATOR, segment, argIdx);
            case VerbatimLine, BlockCommand, VerbatimBlockCommand, ParamCommand -> Index_h.clang_BlockCommandComment_getArgText(LibClang.STRING_ALLOCATOR, segment, argIdx);
            default -> throw new IllegalCallerException();
        };
        return LibClang.CXStrToString(text);
    }

    public ParamPassDirection getDirection() {
        if (kind != CommentKind.ParamCommand) {
            throw new IllegalCallerException();
        }
        return ParamPassDirection.valueOf(Index_h.clang_ParamCommandComment_getDirection(segment));
    }

    public boolean isDirectionExplicit() {
        if (kind != CommentKind.ParamCommand) {
            throw new IllegalCallerException();
        }
        return Index_h.clang_ParamCommandComment_isDirectionExplicit(segment) != 0;
    }

    public boolean isParamIndexValid() {
        if (kind != CommentKind.ParamCommand) {
            throw new IllegalCallerException();
        }
        return Index_h.clang_ParamCommandComment_isParamIndexValid(segment) != 0;
    }

    public int getParamIndex() {
        if (kind != CommentKind.ParamCommand) {
            throw new IllegalCallerException();
        }
        return Index_h.clang_ParamCommandComment_getParamIndex(segment);
    }

    public String getParamName() {
        if (kind != CommentKind.ParamCommand) {
            throw new IllegalCallerException();
        }
        var name = Index_h.clang_ParamCommandComment_getParamName(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(name);
    }

    public String getAsString() {
        if (kind != CommentKind.HTMLStartTag && kind != CommentKind.HTMLEndTag) {
            throw new IllegalCallerException();
        }
        var string = Index_h.clang_HTMLTagComment_getAsString(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(string);
    }

    public String getTagName() {
        if (kind != CommentKind.HTMLStartTag && kind != CommentKind.HTMLEndTag) {
            throw new IllegalCallerException();
        }
        var tagName = Index_h.clang_HTMLTagComment_getTagName(LibClang.STRING_ALLOCATOR, segment);
        return LibClang.CXStrToString(tagName);
    }

    public int getNumAttrs() {
        if (kind != CommentKind.HTMLStartTag) {
            throw new IllegalCallerException();
        }
        return Index_h.clang_HTMLStartTag_getNumAttrs(segment);
    }

    public boolean isSelfClosing() {
        if (kind != CommentKind.HTMLStartTag) {
            throw new IllegalCallerException();
        }
        return Index_h.clang_HTMLStartTagComment_isSelfClosing(segment) != 0;
    }

    public String getAttrName(int attrIdx) {
        if (kind != CommentKind.HTMLStartTag) {
            throw new IllegalCallerException();
        }
        var attrName = Index_h.clang_HTMLStartTag_getAttrName(LibClang.STRING_ALLOCATOR, segment, attrIdx);
        return LibClang.CXStrToString(attrName);
    }

    public String getAttrValue(int attrIdx) {
        if (kind != CommentKind.HTMLStartTag) {
            throw new IllegalCallerException();
        }
        var attrValue = Index_h.clang_HTMLStartTag_getAttrValue(LibClang.STRING_ALLOCATOR, segment, attrIdx);
        return LibClang.CXStrToString(attrValue);
    }
}
