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

import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.HashMap;

public class GetConfigRequest {

    public GoPluginApiResponse execute() {
        HashMap<String, Object> config = new HashMap<>();

        HashMap<String, Object> url = new HashMap<>();
        url.put("display-order", "0");
        url.put("display-name", "Endpoint");
        url.put("required", true);
        config.put(TaskPlugin.URL_PROPERTY, url);

        HashMap<String, Object> clientID = new HashMap<>();
        clientID.put("default-value", "");
        clientID.put("display-order", "1");
        clientID.put("display-name", "ClientID");
        clientID.put("required", false);
        config.put(TaskPlugin.ID_PROPERTY, clientID);

        HashMap<String, Object> clientSecret = new HashMap<>();
        clientSecret.put("default-value", "");
        clientSecret.put("display-order", "2");
        clientSecret.put("display-name", "ClientSecret");
        clientSecret.put("secret", true);
        clientSecret.put("required", false);
        config.put(TaskPlugin.SECRET_PROPERTY, clientSecret);

        HashMap<String, Object> teamName = new HashMap<>();
        teamName.put("default-value", TaskPlugin.DEFAULT_TEAM);
        teamName.put("display-order", "3");
        teamName.put("display-name", "TeamName");
        teamName.put("required", false);
        config.put(TaskPlugin.TEAM_PROPERTY, teamName);

        HashMap<String, Object> inputFile = new HashMap<>();
        inputFile.put("default-value", "");
        inputFile.put("display-order", "4");
        inputFile.put("display-name", "InputFile");
        inputFile.put("required", true);
        config.put(TaskPlugin.INPUT_PROPERTY, inputFile);

        HashMap<String, Object> reportFormat = new HashMap<>();
        reportFormat.put("default-value", TaskPlugin.DEFAULT_REPORT_FORMAT);
        reportFormat.put("display-order", "5");
        reportFormat.put("display-name", "ReportFormat");
        reportFormat.put("required", true);
        config.put(TaskPlugin.REPORT_FORMAT_PROPERTY, reportFormat);

        return DefaultGoPluginApiResponse.success(TaskPlugin.GSON.toJson(config));
    }
}
