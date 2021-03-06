// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Copyright (c) 2016, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.graph;

import com.android.tools.r8.DataResourceProvider;
import com.android.tools.r8.dex.ApplicationReader.ProgramClassConflictResolver;
import com.android.tools.r8.naming.ClassNameMapper;
import com.android.tools.r8.utils.ClasspathClassCollection;
import com.android.tools.r8.utils.LibraryClassCollection;
import com.android.tools.r8.utils.ProgramClassCollection;
import com.android.tools.r8.utils.Timing;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class LazyLoadedDexApplication extends DexApplication {

  private final ProgramClassCollection programClasses;
  private final ClasspathClassCollection classpathClasses;
  private final LibraryClassCollection libraryClasses;

  /** Constructor should only be invoked by the DexApplication.Builder. */
  private LazyLoadedDexApplication(
      ClassNameMapper proguardMap,
      ProgramClassCollection programClasses,
      ImmutableList<DataResourceProvider> dataResourceProviders,
      ClasspathClassCollection classpathClasses,
      LibraryClassCollection libraryClasses,
      ImmutableSet<DexType> mainDexList,
      String deadCode,
      DexItemFactory dexItemFactory,
      DexString highestSortingString,
      Timing timing) {
    super(
        proguardMap,
        dataResourceProviders,
        mainDexList,
        deadCode,
        dexItemFactory,
        highestSortingString,
        timing);
    this.programClasses = programClasses;
    this.classpathClasses = classpathClasses;
    this.libraryClasses = libraryClasses;
  }

  @Override
  List<DexProgramClass> programClasses() {
    programClasses.forceLoad(t -> true);
    return programClasses.getAllClasses();
  }

  @Override
  public DexClass definitionFor(DexType type) {
    assert type.isClassType() : "Cannot lookup definition for type: " + type;
    DexClass clazz = programClasses.get(type);
    if (clazz == null && classpathClasses != null) {
      clazz = classpathClasses.get(type);
    }
    if (clazz == null && libraryClasses != null) {
      clazz = libraryClasses.get(type);
    }
    return clazz;
  }

  @Override
  public DexProgramClass programDefinitionFor(DexType type) {
    assert type.isClassType() : "Cannot lookup definition for type: " + type;
    return programClasses.get(type);
  }

  static class AllClasses {

    // Mapping of all types to their definitions.
    private final Map<DexType, DexClass> allClasses;
    // Collections of the three different types for iteration.
    private final ImmutableList<DexProgramClass> programClasses;
    private final ImmutableList<DexClasspathClass> classpathClasses;
    private final ImmutableList<DexLibraryClass> libraryClasses;

    AllClasses(
        LibraryClassCollection libraryClassesLoader,
        ClasspathClassCollection classpathClassesLoader,
        ProgramClassCollection programClassesLoader) {
      // Collect loaded classes in the precedence order program classes, class path classes and
      // library classes.
      // TODO(b/120884788): Change library priority.
      assert programClassesLoader != null;
      // Program classes are supposed to be loaded, but force-loading them is no-op.
      programClassesLoader.forceLoad(type -> true);
      Map<DexType, DexProgramClass> allProgramClasses = programClassesLoader.getAllClassesInMap();
      int expectedMaxSize = allProgramClasses.size();
      programClasses = ImmutableList.copyOf(allProgramClasses.values());

      Map<DexType, DexClasspathClass> allClasspathClasses = null;
      if (classpathClassesLoader != null) {
        classpathClassesLoader.forceLoad(type -> true);
        allClasspathClasses = classpathClassesLoader.getAllClassesInMap();
        expectedMaxSize += allClasspathClasses.size();
      }

      Map<DexType, DexLibraryClass> allLibraryClasses = null;
      if (libraryClassesLoader != null) {
        libraryClassesLoader.forceLoad(type -> true);
        allLibraryClasses = libraryClassesLoader.getAllClassesInMap();
        expectedMaxSize += allLibraryClasses.size();
      }

      // Note: using hash map for building as the immutable builder does not support contains.
      Map<DexType, DexClass> prioritizedClasses = new IdentityHashMap<>(expectedMaxSize);
      prioritizedClasses.putAll(allProgramClasses);

      if (allClasspathClasses != null) {
        ImmutableList.Builder<DexClasspathClass> builder = ImmutableList.builder();
        allClasspathClasses.forEach(
            (type, clazz) -> {
              if (!prioritizedClasses.containsKey(type)) {
                prioritizedClasses.put(type, clazz);
                builder.add(clazz);
              }
            });
        classpathClasses = builder.build();
      } else {
        classpathClasses = ImmutableList.of();
      }

      if (allLibraryClasses != null) {
        ImmutableList.Builder<DexLibraryClass> builder = ImmutableList.builder();
        allLibraryClasses.forEach(
            (type, clazz) -> {
              if (!prioritizedClasses.containsKey(type)) {
                prioritizedClasses.put(type, clazz);
                builder.add(clazz);
              }
            });
        libraryClasses = builder.build();
      } else {
        libraryClasses = ImmutableList.of();
      }
      allClasses = Collections.unmodifiableMap(prioritizedClasses);
    }

    public Map<DexType, DexClass> getAllClasses() {
      return allClasses;
    }

    public ImmutableList<DexProgramClass> getProgramClasses() {
      return programClasses;
    }

    public ImmutableList<DexClasspathClass> getClasspathClasses() {
      return classpathClasses;
    }

    public ImmutableList<DexLibraryClass> getLibraryClasses() {
      return libraryClasses;
    }
  }

  /**
   * Force load all classes and return type -> class map containing all the classes.
   */
  public AllClasses loadAllClasses() {
    return new AllClasses(libraryClasses, classpathClasses, programClasses);
  }

  public static class Builder extends DexApplication.Builder<Builder> {

    private ClasspathClassCollection classpathClasses;
    private LibraryClassCollection libraryClasses;
    private final ProgramClassConflictResolver resolver;

    Builder(ProgramClassConflictResolver resolver, DexItemFactory dexItemFactory, Timing timing) {
      super(dexItemFactory, timing);
      this.resolver = resolver;
      this.classpathClasses = null;
      this.libraryClasses = null;
    }

    private Builder(LazyLoadedDexApplication application) {
      super(application);
      this.resolver = ProgramClassCollection::resolveClassConflictImpl;
      this.classpathClasses = application.classpathClasses;
      this.libraryClasses = application.libraryClasses;
    }

    @Override
    Builder self() {
      return this;
    }

    public Builder setClasspathClassCollection(ClasspathClassCollection classes) {
      this.classpathClasses = classes;
      return this;
    }

    public Builder setLibraryClassCollection(LibraryClassCollection classes) {
      this.libraryClasses = classes;
      return this;
    }

    @Override
    public LazyLoadedDexApplication build() {
      return new LazyLoadedDexApplication(
          proguardMap,
          ProgramClassCollection.create(programClasses, resolver),
          ImmutableList.copyOf(dataResourceProviders),
          classpathClasses,
          libraryClasses,
          ImmutableSet.copyOf(mainDexList),
          deadCode,
          dexItemFactory,
          highestSortingString,
          timing);
    }
  }

  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Override
  public DirectMappedDexApplication toDirect() {
    return new DirectMappedDexApplication.Builder(this).build().asDirect();
  }

  @Override
  public String toString() {
    return "Application (" + programClasses + "; " + classpathClasses + "; " + libraryClasses
        + ")";
  }
}
