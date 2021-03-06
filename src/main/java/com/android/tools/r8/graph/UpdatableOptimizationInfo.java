// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.graph;

import com.android.tools.r8.graph.DexEncodedMethod.ClassInlinerEligibility;
import com.android.tools.r8.graph.DexEncodedMethod.TrivialInitializer;
import java.util.BitSet;
import java.util.Set;

public interface UpdatableOptimizationInfo extends OptimizationInfo {

  void markInitializesClassesOnNormalExit(Set<DexType> initializedClasses);

  void markReturnsArgument(int argument);

  void markReturnsConstantNumber(long value);

  void markReturnsConstantString(DexString value);

  void markMayNotHaveSideEffects();

  void markNeverReturnsNull();

  void markNeverReturnsNormally();

  void markCheckNullReceiverBeforeAnySideEffect(boolean mark);

  void markTriggerClassInitBeforeAnySideEffect(boolean mark);

  void setClassInlinerEligibility(ClassInlinerEligibility eligibility);

  void setTrivialInitializer(TrivialInitializer info);

  void setInitializerEnablingJavaAssertions();

  void setParameterUsages(ParameterUsagesInfo parameterUsagesInfo);

  void setNonNullParamOrThrow(BitSet facts);

  void setNonNullParamOnNormalExits(BitSet facts);

  void setReachabilitySensitive(boolean reachabilitySensitive);

  void markUseIdentifierNameString();

  void markForceInline();

  void unsetForceInline();

  void markNeverInline();
}
