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

import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

public class TaskConfig {
    // Plugin Config Options
    private final String serverUrl;
    private final String clientId;
    private final String clientSecret;
    private final String teamName;
    private final String inputFileName;
    private final String reportFormat;

    // managed internally for now
    private final boolean waitForReport = true;
    private final String reportFileName = "report";

    private URLBuilder urlBuilder;
    private APITaskExecutor apiTaskExecutor;

    public TaskConfig(Map<String, Object> config, Context context, JobConsoleLogger console) {
        @SuppressWarnings("unchecked")
        StringSubstitutor sub = new StringSubstitutor(context.getEnvironmentVariables());

        serverUrl = getValue(config, sub, TaskPlugin.URL_PROPERTY);
        clientId = getValue(config, sub, TaskPlugin.ID_PROPERTY);
        clientSecret = getValue(config, sub, TaskPlugin.SECRET_PROPERTY);
        teamName = getValue(config, sub, TaskPlugin.TEAM_PROPERTY);
        inputFileName = getValue(config, sub, TaskPlugin.INPUT_PROPERTY);
        reportFormat = getValue(config, sub, TaskPlugin.REPORT_FORMAT_PROPERTY);

        urlBuilder = new URLBuilder(serverUrl);
        apiTaskExecutor = new APITaskExecutor(urlBuilder, console);
    }

    private String getValue(Map<String, Object> config, StringSubstitutor sub, String property) {
        @SuppressWarnings("rawtypes")
        String value = (String) ((Map) config.get(property)).get("value");
        return sub.replace(value);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public String getReportFileName(String assessmentId, String reportFormat) {
        return reportFileName + "-" + assessmentId + "-" + reportFormat + ".json";
    }

    public URLBuilder getURLBuilder() {
        return urlBuilder;
    }

    public APITaskExecutor getAPITaskExecutor() {
        return apiTaskExecutor;
    } 

    public boolean shouldWaitForReport() {
        return waitForReport;
    }
}
