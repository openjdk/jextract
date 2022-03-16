/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

struct PluginCodec_H323AudioG7231AnnexC {
  unsigned char maxAl_sduAudioFrames;
  int silenceSuppression:1;
  int highRateMode0:6; // INTEGER (27..78), -- units octets
  int highRateMode1:6; // INTEGER (27..78), -- units octets
  int lowRateMode0:6; // INTEGER (23..66), -- units octets
  int lowRateMode1:6; // INTEGER (23..66), -- units octets
  int sidMode0:4; // INTEGER (6..17), -- units octets
  int sidMode1:4; // INTEGER (6..17), -- units octets
};
