/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.zimperium.plugins.zScanGoCDUploadPlugin;

import com.thoughtworks.go.plugin.api.task.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CurlTaskExecutor {

    public Result downloadReport(TaskConfig taskConfig, Context context, String assessmentId, JobConsoleLogger console) {
        try {
            return runDownloadCommand(taskConfig, context, assessmentId, console);
        } catch (Exception e) {
            return new Result(false, "Failed to download report for assessment " + assessmentId + " from URL: " + taskConfig.getServerUrl(), e);
        }
    }

    /**
     * Executes a download command using `curl` to retrieve a report file from a remote server.
     * 
     * <p>This method constructs the report's filename and download URL based on the provided 
     * task configuration and context. It uses a {@link ProcessBuilder} to run a `curl` command 
     * to download the report. If the command succeeds, a success message is returned; otherwise, 
     * an error message is returned. The method also manages the process lifecycle and logs output 
     * and errors via the provided console logger.</p>
     * 
     * @param taskConfig   The {@link TaskConfig} containing the configuration for the task, including 
     *                     the report filename, format, and URL builder.
     * @param taskContext  The {@link Context} object providing environment variables and the working directory.
     * @param assessmentId The unique ID of the assessment whose report is to be downloaded.
     * @param console      The {@link JobConsoleLogger} used to print logs and read process output.
     * 
     * @return A {@link Result} object indicating the success or failure of the download operation.
     *         If the `curl` command fails (non-zero exit code), a failure result is returned with an error message.
     * 
     * @throws IOException          If an I/O error occurs when starting or communicating with the process.
     * @throws InterruptedException If the current thread is interrupted while waiting for the process to complete.
     * 
     * @see java.lang.ProcessBuilder
     * @see java.io.IOException
     * @see java.lang.InterruptedException
     */
    @SuppressWarnings("unchecked")
    private Result runDownloadCommand(TaskConfig taskConfig, Context taskContext, String assessmentId, JobConsoleLogger console) throws IOException, InterruptedException {
        // construct report filename and the URL
        String reportFileName = taskContext.getWorkingDir() + "/" + taskConfig.getReportFileName(assessmentId, taskConfig.getReportFormat());
        String reportUrl = taskConfig.getURLBuilder().getDownloadReportURL(assessmentId, taskConfig.getReportFormat());
        
        ProcessBuilder curl = createCurlCommandWithOptions(reportUrl, reportFileName, taskConfig.getAPITaskExecutor().getAuthToken());
        curl.environment().putAll(taskContext.getEnvironmentVariables());

        // WARNING: The following lines output sensitive information that should not be logged during normal operation
        // console.printEnvironment(curl.environment());
        // console.printLine("Launching command: " + curl.command());

        console.printLine("Launching curl command to download " + reportFileName + " from " + reportUrl);
        Process curlProcess = curl.start();
        console.readErrorOf(curlProcess.getErrorStream());
        console.readOutputOf(curlProcess.getInputStream());

        int exitCode = curlProcess.waitFor();
        curlProcess.destroy();

        if (exitCode != 0) {
            return new Result(false, "Error downloading file. Please check the output");
        }

        return new Result(true, "Downloaded file: " + reportFileName);
    }

    ProcessBuilder createCurlCommandWithOptions(String reportUrl, String reportFileName, String authToken) {

        List<String> command = new ArrayList<String>();
        command.add("curl");

        command.add("-H");
        command.add("Authorization: Bearer " + authToken);
        command.add("-o");
        command.add(reportFileName);
        command.add("-L");
        command.add(reportUrl);

        return new ProcessBuilder(command);
    }
}
