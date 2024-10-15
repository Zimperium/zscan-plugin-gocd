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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Arrays;

@Extension
public class TaskPlugin implements GoPlugin {

    public static final String URL_PROPERTY = "Endpoint";
    public static final String ID_PROPERTY = "ClientID";
    public static final String SECRET_PROPERTY = "ClientSecret";
    public static final String TEAM_PROPERTY = "TeamName";
    public static final String DEFAULT_TEAM = "Default";
    public static final String INPUT_PROPERTY = "InputFile";
    public static final String REPORT_FORMAT_PROPERTY = "ReportFormat";
    public static final String DEFAULT_REPORT_FORMAT = "json";

    
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static Logger LOGGER = Logger.getLoggerFor(TaskPlugin.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        if ("configuration".equals(request.requestName())) {
            return new GetConfigRequest().execute();
        } else if ("validate".equals(request.requestName())) {
            return new ValidateRequest().execute(request);
        } else if ("execute".equals(request.requestName())) {
            return new ExecuteRequest().execute(request);
        } else if ("view".equals(request.requestName())) {
            return new GetViewRequest().execute();
        }
        throw new UnhandledRequestTypeException(request.requestName());
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("task", Arrays.asList("1.0"));
    }
}
