/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.storage;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.storage.reports.Messages;
import com.google.jenkins.plugins.storage.reports.ProjectGcsUploadReport;

import hudson.model.Action;
import hudson.model.Job;
import jenkins.tasks.SimpleBuildStep;
/**
 * Last action to
 *
 */
public class GoogleCloudStorageLastAction
implements SimpleBuildStep.LastBuildAction {
  private final Job<?, ?> project;

  public GoogleCloudStorageLastAction(Job<?, ?> project) {
    this.project = project;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getIconFileName() {
    return "save.gif";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDisplayName() {
    return Messages.AbstractGcsUploadReport_DisplayName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUrlName() {
    /* stapler will match this URL name to our action page */
    return "gcsObjects";
  }

  @Override public Collection<? extends Action> getProjectActions() {
    return ImmutableList.of(new ProjectGcsUploadReport(project));
  }

}
