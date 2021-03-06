// Copyright (c) 2018, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.shaking.examples;

import com.android.tools.r8.shaking.TreeShakingTest;
import com.android.tools.r8.utils.codeinspector.ClassSubject;
import com.android.tools.r8.utils.codeinspector.CodeInspector;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TreeShaking9Test extends TreeShakingTest {

  @Parameters(name = "mode:{0}-{1} minify:{2}")
  public static Collection<Object[]> data() {
    List<Object[]> parameters = new ArrayList<>();
    for (MinifyMode minify : MinifyMode.values()) {
      parameters.add(new Object[] {Frontend.JAR, Backend.CF, minify});
      parameters.add(new Object[] {Frontend.JAR, Backend.DEX, minify});
      parameters.add(new Object[] {Frontend.DEX, Backend.DEX, minify});
    }
    return parameters;
  }

  public TreeShaking9Test(Frontend frontend, Backend backend, MinifyMode minify) {
    super("examples/shaking9", "shaking9.Shaking", frontend, backend, minify);
  }

  @Test
  public void testKeeprules() throws Exception {
    runTest(
        TreeShaking9Test::shaking9OnlySuperMethodsKept,
        null,
        null,
        ImmutableList.of("src/test/examples/shaking9/keep-rules.txt"));
  }

  @Test
  public void testKeeprulesprintusage() throws Exception {
    runTest(
        null, null, null, ImmutableList.of("src/test/examples/shaking9/keep-rules-printusage.txt"));
  }

  private static void shaking9OnlySuperMethodsKept(CodeInspector inspector) {
    ClassSubject superclass = inspector.clazz("shaking9.Superclass");
    Assert.assertTrue(superclass.isAbstract());
    Assert.assertTrue(superclass.method("void", "aMethod", ImmutableList.of()).isPresent());
    ClassSubject subclass = inspector.clazz("shaking9.Subclass");
    Assert.assertFalse(subclass.method("void", "aMethod", ImmutableList.of()).isPresent());
  }
}
