// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.bazel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.BlazeVersionInfo;
import com.google.devtools.build.lib.runtime.BlazeModule;
import com.google.devtools.build.lib.runtime.BlazeRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * The main class.
 */
public final class BazelMain {
  private static final String BUILD_DATA_PROPERTIES = "/build-data.properties";

  /**
   * The list of modules to load. Note that the order is important: In case multiple modules provide
   * strategies for the same things, the last module wins and its strategy becomes the default.
   *
   * <p>Example: To make the "standalone" execution strategy the default for spawns, put it after
   * all the other modules that provider spawn strategies (e.g. WorkerModule and SandboxModule).
   */
  public static final List<Class<? extends BlazeModule>> BAZEL_MODULES =
      ImmutableList.of(
          com.google.devtools.build.lib.bazel.BazelShutdownLoggerModule.class,
          com.google.devtools.build.lib.bazel.BazelWorkspaceStatusModule.class,
          com.google.devtools.build.lib.bazel.BazelDiffAwarenessModule.class,
          com.google.devtools.build.lib.bazel.BazelRepositoryModule.class,
          com.google.devtools.build.lib.bazel.dash.DashModule.class,
          com.google.devtools.build.lib.bazel.rules.BazelRulesModule.class,
          com.google.devtools.build.lib.worker.WorkerModule.class,
          com.google.devtools.build.lib.remote.RemoteModule.class,
          com.google.devtools.build.lib.standalone.StandaloneModule.class,
          com.google.devtools.build.lib.sandbox.SandboxModule.class,
          com.google.devtools.build.lib.runtime.BuildSummaryStatsModule.class);

  public static void main(String[] args) {
    BlazeVersionInfo.setBuildInfo(tryGetBuildInfo());
    BlazeRuntime.main(BAZEL_MODULES, args);
  }

  /**
   * Builds the standard build info map from the loaded properties. The returned value is the list
   * of "build.*" properties from the build-data.properties file. The final key is the original one
   * striped, dot replaced with a space and with first letter capitalized. If the file fails to
   * load the returned map is empty.
   */
  private static ImmutableMap<String, String> tryGetBuildInfo() {
    try (InputStream in = BazelMain.class.getResourceAsStream(BUILD_DATA_PROPERTIES)) {
      if (in == null) {
        return ImmutableMap.of();
      }
      Properties props = new Properties();
      props.load(in);
      ImmutableMap.Builder<String, String> buildData = ImmutableMap.builder();
      for (Object key : props.keySet()) {
        String stringKey = key.toString();
        if (stringKey.startsWith("build.")) {
          // build.label -> Build label, build.timestamp.as.int -> Build timestamp as int
          String buildDataKey = "B" + stringKey.substring(1).replace('.', ' ');
          buildData.put(buildDataKey, props.getProperty(stringKey, ""));
        }
      }
      return buildData.build();
    } catch (IOException ignored) {
      return ImmutableMap.of();
    }
  }
}
