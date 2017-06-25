/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.jenkins.plugins.storage.AbstractUploadDescriptor.GCS_SCHEME;

import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;

/**
 * A Jenkins plugin for uploading files to Google Cloud Storage (GCS).
 */
@RequiresDomain(value = StorageScopeRequirement.class)
public class GoogleCloudStorageUploader extends Recorder
    implements SimpleBuildStep {
  /**
   * Construct the GCS uploader to use the provided credentials to
   * upload build artifacts.
   *
   * @param credentialsId The credentials to utilize for authenticating with GCS
   * @param uploads The list of uploads the user has requested be done
   */
  @DataBoundConstructor
  public GoogleCloudStorageUploader(
      String credentialsId, @Nullable List<AbstractUpload> uploads) {
    this.credentialsId = checkNotNull(credentialsId);
    if (uploads == null) {
      this.uploads = ImmutableList.<AbstractUpload>of();
    } else {
      checkArgument(uploads.size() > 0);
      this.uploads = uploads;
    }
  }

  /**
   * The unique ID for the credentials we are using to
   * authenticate with GCS.
   */
  public String getCredentialsId() {
    return credentialsId;
  }
  private final String credentialsId;

  /**
   * The credentials we are using to authenticate with GCS.
   */
  public GoogleRobotCredentials getCredentials() {
    return GoogleRobotCredentials.getById(getCredentialsId());
  }

  /**
   * The set of tuples describing the artifacts to upload, and where
   * to upload them.
   */
  public Collection<AbstractUpload> getUploads() {
    return Collections.unmodifiableCollection(uploads);
  }
  private final List<AbstractUpload> uploads;

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(Run<?, ?> run, FilePath workspace,
      Launcher launcher, TaskListener listener
      ) throws InterruptedException, IOException {
    GoogleRobotCredentials credentials = getCredentials();

    // TODO(mattmoor): threadpool?
    for (AbstractUpload upload : uploads) {
      try {
        upload.perform(credentials, run, workspace, listener);
      } catch (UploadException e) {
        e.printStackTrace(listener.error(
            Messages.UploadModule_PrefixFormat(
                getDescriptor().getDisplayName(),
                Messages.GoogleCloudStorageUploader_ExceptionDuringUpload(
                    e.getMessage()))));
        run.setResult(Result.FAILURE);
      }
    }

    run.addAction(new GoogleCloudStorageLastAction(run.getParent()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * Descriptor for the extension for uploading build artifacts to
   * Google Cloud Storage.
   */
  @Extension @Symbol("google-storage")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Publisher> {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(
        @SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.GoogleCloudStorageUploader_DisplayName();
    }

    /**
     * @return the default uploads when the user configure
     *         {@link GoogleCloudStorageUploader} for the first time.
     */
    public List<AbstractUpload> getDefaultUploads() {
      return ImmutableList.<AbstractUpload>of(
          new StdoutUpload(GCS_SCHEME,
              false /* public? */, true /* for failed? */,
              false /* strip path prefix? */,
              false /* show inline? */,
              null /* path prefix */,
              null /* module */, "build-log.txt" /* log name */,
              null /* legacy arg: bucketNameWithVars */));
    }

    public List<AbstractUploadDescriptor> getUploads() {
      return AbstractUpload.all();
    }
  }
}
