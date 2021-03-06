// Copyright (c) 2016, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.smali;

import static org.junit.Assert.assertEquals;

import com.android.tools.r8.smali.SmaliBuilder.MethodSignature;
import com.android.tools.r8.utils.AndroidApp;
import com.android.tools.r8.utils.StringUtils;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class JumboStringTest extends SmaliTestBase {

  @Test
  public void test() throws Exception {
    StringBuilder builder = new StringBuilder();
    StringBuilder expectedBuilder = new StringBuilder();
    builder.append(StringUtils.lines("    new-instance         v0, Ljava/lang/StringBuilder;"));
    builder.append(StringUtils.lines("    invoke-direct        { v0 }, Ljava/lang/StringBuilder;"
        + "-><init>()V"));
    for (int i = 0; i <= 0xffff + 2; i++) {
      String prefixed = StringUtils.zeroPrefix(i, 5);
      expectedBuilder.append(prefixed);
      expectedBuilder.append(StringUtils.lines(""));
      builder.append(StringUtils.lines("  const-string         v1, \"" + prefixed + "\\n\""));
      builder.append(
          StringUtils.lines("  invoke-virtual       { v0, v1 }, Ljava/lang/StringBuilder;"
              + "->append(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
    }
    builder.append(
        StringUtils.lines("    invoke-virtual       { v0 }, Ljava/lang/StringBuilder;"
            + "->toString()Ljava/lang/String;"));
    builder.append(StringUtils.lines("    move-result-object   v0"));
    builder.append(StringUtils.lines("    return-object               v0"));

    SmaliBuilder smaliBuilder = new SmaliBuilder(DEFAULT_CLASS_NAME);

    MethodSignature signature = smaliBuilder.addStaticMethod(
        "java.lang.String",
        DEFAULT_METHOD_NAME,
        ImmutableList.of(),
        2,
        builder.toString()
    );

    smaliBuilder.addMainMethod(
        2,
        "    sget-object         v0, Ljava/lang/System;->out:Ljava/io/PrintStream;",
        "    invoke-static       {}, LTest;->method()Ljava/lang/String;",
        "    move-result-object  v1",
        "    invoke-virtual      { v0, v1 }, Ljava/io/PrintStream;->print(Ljava/lang/String;)V",
        "    return-void"
    );

    AndroidApp originalApplication = buildApplication(smaliBuilder);
    AndroidApp processedApplication = processApplication(originalApplication);
    String result = runArt(processedApplication);

    assertEquals(expectedBuilder.toString(), result);
  }
}
