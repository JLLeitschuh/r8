// Copyright (c) 2016, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.utils;

// Helper for collecting timing information during execution.
// Timing t = new Timing("R8");
// A timing tree is collected by calling the following pair (nesting will create the tree):
//     t.begin("My task);
//     try { ... } finally { t.end(); }
// or alternatively:
//     t.scope("My task", () -> { ... });
// Finally a report is printed by:
//     t.report();

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class Timing {

  private final Stack<Node> stack;

  public Timing() {
    this("<no title>");
  }

  public Timing(String title) {
    stack = new Stack<>();
    stack.push(new Node("Recorded timings for " + title));
  }

  static class Node {
    final String title;

    final Map<String, Node> children = new LinkedHashMap<>();
    long duration = 0;
    long start_time;

    Node(String title) {
      this.title = title;
      this.start_time = System.nanoTime();
    }

    void restart() {
      assert start_time == -1;
      start_time = System.nanoTime();
    }

    void end() {
      duration += System.nanoTime() - start_time;
      start_time = -1;
      assert duration() >= 0;
    }

    long duration() {
      return duration;
    }

    @Override
    public String toString() {
      return title + ": " + (duration() / 1000000) + "ms.";
    }

    public String toString(Node top) {
      if (this == top) return toString();
      long percentage = duration() * 100 / top.duration();
      return toString() + " (" + percentage + "%)";
    }

    public void report(int depth, Node top) {
      assert duration() >= 0;
      if (depth > 0) {
        for (int i = 0; i < depth; i++) {
          System.out.print("  ");
        }
        System.out.print("- ");
      }
      System.out.println(toString(top));
      children.values().forEach(p -> p.report(depth + 1, top));
    }
  }


  public void begin(String title) {
    Node parent = stack.peek();
    Node child;
    if (parent.children.containsKey(title)) {
      child = parent.children.get(title);
      child.restart();
    } else {
      child = new Node(title);
      parent.children.put(title, child);
    }
    stack.push(child);
  }

  public void end() {
    stack.peek().end();  // record time.
    stack.pop();
  }

  public void report() {
    Node top = stack.peek();
    top.end();
    System.out.println();
    top.report(0, top);
  }

  public void scope(String title, TimingScope fn) {
    begin(title);
    try {
      fn.apply();
    } finally {
      end();
    }
  }

  public interface TimingScope {
    void apply();
  }
}
