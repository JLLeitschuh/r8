// Copyright (c) 2018, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.resolution.singletarget.two;

public abstract class OtherAbstractTopClass {

  public void overriddenInTwoSubTypes() {
    System.out.println(OtherAbstractTopClass.class.getCanonicalName());
  }

  public abstract void abstractOverriddenInTwoSubTypes();

  public abstract void overridesOnDifferentLevels();
}
