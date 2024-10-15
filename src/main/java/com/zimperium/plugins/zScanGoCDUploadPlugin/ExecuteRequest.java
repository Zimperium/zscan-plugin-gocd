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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.Map;
import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.filefilter.WildcardFileFilter;

public class ExecuteRequest {
    public final static long checkInterval = 30;
    public final static long reportTimeout = 1200;
    public final static long maxFiles = 5;

    /**
     * Main method of the plugin.  It performs steps to upload specified binaries to Zimperium for analysis, assign
     * applications to teams (if necessary), wait for assessments to complete, and download reports in either 
     * JSON or SARIF formats.  The parameters and return values follow GoCD plugin conventions. 
     * 
     * @param request Information about the job being run by GoCD 
     * @return  Result of the execution in the form of GoPluginApiResponse object, with the response code set to
     *          DefaultGoApiResponse.SUCCESS_RESPONSE_CODE or DefaultGoApiResponse.INTERNAL_ERROR based on whether
     *          the upload was successful.
     */
    public GoPluginApiResponse execute(GoPluginApiRequest request) {
        // prepare infrastructure
        Result result = new Result(false, "Unspecified error executing plugin.");
        JobConsoleLogger console = JobConsoleLogger.getConsoleLogger();
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, Object> executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, Object> config = (Map) executionRequest.get("config");
        @SuppressWarnings("rawtypes")
        Context context = new Context((Map) executionRequest.get("context"));

        try {
            TaskConfig taskConfig = new TaskConfig(config, context, console);
            APITaskExecutor apiTaskExecutor = taskConfig.getAPITaskExecutor();

            // Upload the binaries
            File directory = new File(context.getWorkingDir());

            // Ensure the directory exists and is a directory
            if (!directory.exists() || !directory.isDirectory()) {
                throw new IllegalArgumentException("The provided path is not a valid directory: " + context.getWorkingDir());
            }
    
            // Create a wildcard file filter based on the pattern
            FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(taskConfig.getInputFileName()).get();
    
            // Find matching files
            File[] files = directory.listFiles(fileFilter);
            console.printLine("Found " + files.length + " matching files to upload.");
            if(files.length > maxFiles) {
                throw new IllegalArgumentException("The provided pattern matched too many files. No more than " + maxFiles + " files are allowed.");
            }
        
            // Do we have anything to upload?
            if(files.length < 1) {
                result = new Result(true, "No files matched the provided pattern.");
            }
            // We do; login and get a token
            else if(apiTaskExecutor.login(taskConfig.getClientId(), taskConfig.getClientSecret())) {
                // Iterate over matching files
                int successCount = 0;
                for(File binary : files) {
                    if(!binary.exists() || binary.isDirectory()) {
                        console.printLine(binary.getAbsolutePath() + " does not exist or a directory. Skipping.");
                        continue;
                    }
                    Response uploadResponse = apiTaskExecutor.uploadBinary(binary, context);
                    if(uploadResponse.isSuccessful()) {
                        // Assign to a team if necessary
                        ResponseBody uploadResponseBody = uploadResponse.body();
                        JsonObject jsonObject = JsonParser.parseString(uploadResponseBody.string()).getAsJsonObject();

                        // Extract the appId needed for team assignment, buildId to check report status, and the current team 
                        String zdevAppId = (jsonObject.get("zdevAppId").isJsonNull()) ? "" : jsonObject.get("zdevAppId").getAsString();
                        String teamId = (jsonObject.get("teamId").isJsonNull()) ? "" : jsonObject.get("teamId").getAsString();
                        String buildId = (jsonObject.get("buildId").isJsonNull()) ? "" : jsonObject.get("buildId").getAsString();

                        // If teamID is empty, find the correct team id by name
                        if(teamId.isEmpty()) {
                            console.printLine("Application " + zdevAppId + " does not belong to a team. Assigning it to the " + taskConfig.getTeamName() + " team.");

                            // need to wait a bit; otherwise we can get 404
                            synchronized(this) {
                                wait(checkInterval * 1000);
                            }

                            try {
                                // get list of teams from the server
                                Response listTeamsResponse = apiTaskExecutor.listTeams();
                                // extract list of teams from the response
                                JsonObject teamsObject = JsonParser.parseString(listTeamsResponse.body().string()).getAsJsonObject();
                                if(!teamsObject.isJsonNull() && !teamsObject.isEmpty() && teamsObject.get("content").isJsonArray()) {
                                    JsonArray teamArray = teamsObject.get("content").getAsJsonArray();
                                    console.printLine("Found " + teamArray.size() + " teams");
                                    for (JsonElement teamElement : teamArray) {
                                        String name = teamElement.getAsJsonObject().get("name").getAsString();
                                        // log(console, "Team " + name);
                                        if(name.equals(taskConfig.getTeamName())){
                                            teamId = teamElement.getAsJsonObject().get("id").getAsString();
                                            //log(console, "Found team with ID: " + teamId);
                                            break;
                                        }
                                    }
                                            
                                    // if we did not find the specified team, try 'Default'
                                    if(teamId.isEmpty() && !taskConfig.getTeamName().equals("Default")) {
                                        console.printLine("Team " + taskConfig.getTeamName() + " not found.  Trying the 'Default' team.");
                                        for (JsonElement teamElement : teamArray) {
                                            String name = teamElement.getAsJsonObject().get("name").getAsString();
                                            // log(console, "Team " + name);
                                            if(name.equals("Default")){
                                                teamId = teamElement.getAsJsonObject().get("id").getAsString();
                                                console.printLine("Found team with ID: " + teamId);
                                                break;
                                            }
                                        }
                                    }

                                    // Assign the app to the team
                                    if(!teamId.isEmpty()) {
                                        apiTaskExecutor.assignAppToTeam(zdevAppId, teamId);
                                    }
                                    else {
                                        console.printLine("Unable to assign this app to a team.  Unexpected response from the server.");
                                        if(listTeamsResponse.body() != null) {
                                            console.printLine("HTTP " + listTeamsResponse.code() + ": " + listTeamsResponse.body().string());
                                        }
                                    }
                                }
                                else {
                                    console.printLine("Unable to assign this app to a team.  Please review team name setting and credentials, and retry.");
                                    if(listTeamsResponse.body() != null) {
                                        console.printLine("HTTP " + listTeamsResponse.code() + ": " + listTeamsResponse.body().string());
                                    }
                                }
                            }
                            catch(RuntimeException e) {
                                console.printLine("Unexpected runtime exception: " + e.getLocalizedMessage());
                                throw e;
                            }
                            catch(Exception e) {
                                console.printLine("Error processing team list: " + e.getLocalizedMessage());
                            }
                        }
                        else {
                            console.printLine("Application " + zdevAppId + " already belongs to team " + teamId);
                        }

                        // upload may have taken a long time; refresh the access token
                        apiTaskExecutor.refreshToken();

                        String assessmentId = "";
                        if(taskConfig.shouldWaitForReport()) {
                            // wait for report
                            synchronized(this) {
                                long start = System.currentTimeMillis();
                                long end = start + reportTimeout * 1000;
                                while( System.currentTimeMillis() < end ) {
                                    Response statusResponse = apiTaskExecutor.checkStatus(buildId);
                                    if(statusResponse.isSuccessful()) {
                                        try(ResponseBody statusBody = statusResponse.body()) {
                                            // we're inside the try() block; exceptions will be caught
                                            JsonObject statusObject = JsonParser.parseString(statusBody.string()).getAsJsonObject();
                                            String scanStatus = statusObject.getAsJsonObject("zdevMetadata").get("analysis").getAsString();
                                            console.printLine("Scan status = " + scanStatus);

                                            if(scanStatus.equals("Done")) {
                                                assessmentId = statusObject.get("id").getAsString();
                                                // need to pause before continuing to make sure reports are available
                                                console.printLine("Waiting for the report to become available...");
                                                wait(checkInterval * 1000);
                                                break;
                                            }
                                        }
                                        catch(Exception e) {
                                            console.printLine("Unexpected exception: " + e.getLocalizedMessage());
                                            break;
                                        }
                                    }
                                    else if (statusResponse.code() != 404) {
                                        console.printLine("Unable to get assessment report. Please check credentials and try again.");
                                        if(statusResponse.body() != null) {
                                            console.printLine("HTTP " + statusResponse.code() + ": " + statusResponse.body().string());
                                        }
                                        // move on to the next one
                                        break;
                                    }
                                    
                                    wait(checkInterval * 1000);
                                }
                            }

                            // report may have taken a long time; refresh the access token
                            apiTaskExecutor.refreshToken();

                            // Download report;
                            console.printLine("Downloading report...");

                            CurlTaskExecutor executor = new CurlTaskExecutor();
                            result = executor.downloadReport(taskConfig, context, assessmentId, console);
                            if(result.responseCode() == DefaultGoApiResponse.SUCCESS_RESPONSE_CODE) {
                                successCount++;
                            }
                        }
                    }
                    else {
                        console.printLine("Error uploading " + binary.getAbsolutePath() + ": ");
                        console.printLine("HTTP" + uploadResponse.code() + ": " + uploadResponse.body().string());
                    }
                }   

                console.printLine("Successfully uploaded " + successCount + " binaries for analysis.");
                result = new Result(true, "Successfully uploaded " + successCount + " binaries for analysis.");
            }
            // Login unsuccessful
            else {
                result = new Result(false, "Error logging in to Zimperium server.");
            }
        }
        catch (Exception e) {
            console.printLine("Exception: " + e.getLocalizedMessage());
            result = new Result(false, "Exception:" + e.getLocalizedMessage());
        }
        
        // return result to the agent
        return new DefaultGoPluginApiResponse(result.responseCode(), TaskPlugin.GSON.toJson(result.toMap()));
    }
}
