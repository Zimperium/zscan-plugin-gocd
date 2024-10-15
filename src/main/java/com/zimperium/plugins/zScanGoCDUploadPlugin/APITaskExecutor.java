package com.zimperium.plugins.zScanGoCDUploadPlugin;

import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * APITaskExecutor encapsulates API calls to the Zimperium server. It provides public methods
 * to login/obtain auth token, refresh a token, upload a binary, assign application to a team, and check status of
 * an assessment.  Auth tokens are managed internally. Auth token is exposed to the outside through a getter function.  
 */
public class APITaskExecutor {
    public static final MediaType JSON_TYPE = MediaType.parse("application/json");
    public static final MediaType FILE_TYPE = MediaType.parse("application/octet-stream");
    public static final String toolId = "GOCD";
    public static final String toolName = "GoCD Plugin";

    private URLBuilder urlBuilder;
    JobConsoleLogger console;

    private String authToken;
    private String refreshToken;

    private final OkHttpClient client;
    private final Gson gson;

    public APITaskExecutor(URLBuilder urlBuilder, JobConsoleLogger console) {
        gson = new Gson();

        this.urlBuilder = urlBuilder;
        this.console = console;

        client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public String getAuthToken() {
        return authToken;
    }

    /**
     * Authenticates the client by sending a login request with the provided client ID and secret.
     * 
     * <p>This method constructs a JSON payload containing the client ID and secret and sends it 
     * to the login URL using an HTTP POST request. If the request is successful, it extracts the 
     * access and refresh tokens from the response body. If any exceptions occur during the process, 
     * error messages are printed to the console.</p>
     * 
     * @param clientId     The client ID used for authentication.
     * @param clientSecret The client secret used for authentication.
     * @return {@code true} if the login is successful and tokens are received; 
     *         {@code false} if the request fails or an exception occurs.
     * 
     * @throws NullPointerException if the response body does not contain the expected tokens.
     * 
     * @see okhttp3.Request
     * @see okhttp3.Response
     * @see okhttp3.RequestBody
     * @see java.io.IOException
     */
    public boolean login(String clientId, String clientSecret) {

        boolean result = false;
        String loginUrl = urlBuilder.getLoginURL();
        console.printLine("Sending login request to " + loginUrl);

        Map<String, String> loginPayload = new LinkedHashMap<>();
        loginPayload.put("clientId", clientId);
        loginPayload.put("secret", clientSecret);

        String jsonBody = gson.toJson(loginPayload);
        RequestBody loginBody = RequestBody.create(jsonBody, JSON_TYPE);
        Request request = new Request.Builder()
            .url(loginUrl)
            .post(loginBody)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                console.printLine("Unable to login: HTTP" + response.code() + " - " + response.body().string());
            }
            else {
                ResponseBody responseBody = response.body();
                try {
                    JsonObject jsonObject = JsonParser.parseString(responseBody.string()).getAsJsonObject();
                    authToken = jsonObject.get("accessToken").getAsString();
                    refreshToken = jsonObject.get("refreshToken").getAsString();

                    result = true;
                }
                catch (NullPointerException e) {
                    console.printLine("Exception parsing login response: " + e.getLocalizedMessage());
                }
            }
        }
        catch (IOException e) {
            console.printLine("Exception trying to login: " + e.getLocalizedMessage());
        }

        console.printLine(result ? "Login successful" : "Login unsuccessful");
        return result;
    }

    /**
     * Uploads a binary file to the server using a multipart HTTP POST request.
     * 
     * <p>This method constructs a multipart request containing metadata such as 
     * the branch name, build number, and CI tool information, along with the file 
     * to be uploaded. If the request is successful, it logs the success message 
     * and the time taken for the upload. If an exception occurs during the upload, 
     * it prints an error message to the console.</p>
     * 
     * @param file    The {@link File} object representing the binary to be uploaded.
     * @param context The {@link Context} object containing environment variables 
     *                such as branch name and build number.
     * @return A {@link Response} object containing the server's response to the upload request.
     *         If an exception occurs, the method returns {@code null}.
     * 
     * @throws NullPointerException if {@code file} or {@code context} is {@code null}.
     * @throws IllegalArgumentException if the specified file does not exist.
     * 
     * @see okhttp3.Request
     * @see okhttp3.RequestBody
     * @see okhttp3.MultipartBody
     * @see okhttp3.Response
     * @see java.io.IOException
     */
    public Response uploadBinary(File file, Context context) {
        console.printLine("Uploading " + file.getAbsolutePath() + " to " + urlBuilder.getUploadURL());

        @SuppressWarnings("unchecked")
        Map<String, String> envVars = context.getEnvironmentVariables();
        
        String branchName = (envVars.get("BRANCH_NAME") != null) ? envVars.get("BRANCH_NAME") : "";
        String buildNumber = (envVars.get("BUILD_NUMBER") != null) ? envVars.get("BUILD_NUMBER") : "";

        RequestBody uploadRequestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("ciToolId", toolId)
            .addFormDataPart("ciToolName", toolName)
            .addFormDataPart("branchName", branchName)
            .addFormDataPart("buildNumber", buildNumber)
            .addFormDataPart("buildFile", file.getAbsolutePath(), RequestBody.create(file, FILE_TYPE))
            .build();
        
        Request uploadRequest = new Request.Builder()
            .header("Authorization", "BEARER " + authToken)
            .url(urlBuilder.getUploadURL())
            .post(uploadRequestBody)
            .build();
        
        Response uploadResponse = null;
        try {
            long start = System.currentTimeMillis();
            uploadResponse = client.newCall(uploadRequest).execute();
            long end = System.currentTimeMillis();

            if (uploadResponse.isSuccessful()) {
                console.printLine("Successfully uploaded " + file.getAbsolutePath() + " to " + urlBuilder.getUploadURL() + " (" + (end - start) + "ms)");
            }
        }
        catch(IOException e) {
            console.printLine("Exception uploading file: " + e.getLocalizedMessage());
        }

        return uploadResponse;
    }

    /**
     * Sends an HTTP GET request to retrieve the list of teams.
     * 
     * <p>This method constructs an HTTP GET request with an authorization header and sends it 
     * to the appropriate endpoint. If the request is successful, it prints the time taken 
     * to retrieve the response to the console. If an I/O exception occurs, an error message 
     * is printed instead.</p>
     * 
     * @return A {@link Response} object containing the HTTP response with the list of teams.
     *         If an exception occurs, the method returns {@code null}.
     * 
     * @throws NullPointerException if the authorization token is {@code null}.
     * @see okhttp3.Request
     * @see okhttp3.Response
     * @see java.io.IOException
     */
    public Response listTeams() {
        Request teamListRequest = new Request.Builder()
        .header("Authorization", "BEARER " + authToken)
        .url(urlBuilder.getTeamsURL())
        .get()
        .build();
    
        Response teamListResponse = null;
        try {
            long start = System.currentTimeMillis();
            teamListResponse = client.newCall(teamListRequest).execute();
            long end = System.currentTimeMillis();

            if (teamListResponse.isSuccessful()) {
                console.printLine("Received list of teams in " + (end - start) + "ms");
            }
        }
        catch(IOException e) {
            console.printLine("Exception getting list of teams: " + e.getLocalizedMessage());
        }

        return teamListResponse;
    }

    /**
     * Assigns an application to a team by sending an HTTP PUT request with the team ID in the payload.
     * 
     * <p>This method constructs a JSON request body containing the team ID and sends it to the 
     * appropriate URL for assigning the application to the team. If the request succeeds, the 
     * method prints a success message to the console and returns {@code true}. Otherwise, it prints 
     * an error message with the HTTP response code and body.</p>
     * 
     * @param appId  The ID of the application to be assigned to the team.
     * @param teamId The ID of the team to which the application will be assigned.
     * @return {@code true} if the application was successfully assigned to the team; 
     *         {@code false} if the request fails or an exception occurs.
     * 
     * @throws NullPointerException if {@code appId} or {@code teamId} is {@code null}.
     * 
     * @see okhttp3.Request
     * @see okhttp3.Response
     * @see okhttp3.RequestBody
     * @see java.io.IOException
     */
    public boolean assignAppToTeam(String appId, String teamId) {
        boolean result = false;

        // create payload in the json format {"teamId": ""}
        Map<String, String> teamPayload = new LinkedHashMap<>();
        teamPayload.put("teamId", teamId);

        String jsonBody = gson.toJson(teamPayload);
        RequestBody teamBody = RequestBody.create(jsonBody, JSON_TYPE);
        Request assignRequest = new Request.Builder()
            .url(urlBuilder.getCompleteUploadURL(appId))
            .header("Authorization", "BEARER " + authToken)
            .put(teamBody)
            .build();

        try {
            Response assignResponse = client.newCall(assignRequest).execute();

            if(assignResponse.isSuccessful()) {
                console.printLine("Successfully assigned application to team.");
                result = true;
            }
            else {
                console.printLine("Unable to assign this app to a team.  Please review team name setting and retry.");
                console.printLine("HTTP " + assignResponse.code() + ": " + assignResponse.body().string());
            }
        }
        catch (IOException e) {
            console.printLine("Unable to assign this app to a team. Unexpected exception: " + e.getLocalizedMessage());
        }

        return result;
    }

    /**
     * Sends an HTTP GET request to check the status of an assessment using the provided 
     * build ID which is extracted from the upload response.
     * 
     * <p>This method constructs a request with an authorization header, 
     * sends it to the status URL corresponding to the given build ID, 
     * and returns the server's response. If an I/O exception occurs during 
     * the call, it logs the exception message to the console and returns null.</p>
     * 
     * @param buildId The Build ID of the assessment whose status needs to be checked.
     *                This value is appended to the status URL for the request.
     * @return The {@link Response} object containing the HTTP response from the server.
     *         If an exception occurs during the call, the method returns {@code null}.
     * @throws NullPointerException if {@code buildId} is {@code null}.
     * 
     * @see okhttp3.Request
     * @see okhttp3.Response
     * @see java.io.IOException
     */
    public Response checkStatus(String buildId) {
        Request statusRequest = new Request.Builder()
        .header("Authorization", "BEARER " + authToken)
        .url(urlBuilder.getStatusURL(buildId))
        .get()
        .build();
    
        Response statusResponse = null;
        try {
            statusResponse = client.newCall(statusRequest).execute();
        }
        catch(IOException e) {
            console.printLine("Exception checking assessment status: " + e.getLocalizedMessage());
        }

        return statusResponse;
    }

    /**
     * Attempts to refresh the authentication token by making an HTTP POST request 
     * with the current refresh token. If the operation is successful, the method 
     * updates both the access token and the refresh token.
     * 
     * <p>This method sends a JSON payload containing the refresh token to the 
     * refresh URL. On a successful response, the new access and refresh tokens 
     * are extracted from the response body. If the current refresh token is null or empty, 
     * the method returns {@code false} immediately without making a request.</p>
     * 
     * @return {@code true} if the token was successfully refreshed and both the 
     *         access and refresh tokens were updated; {@code false} otherwise.
     *         This includes cases where the current refresh token is missing, the request fails, 
     *         or the response body cannot be parsed correctly.
     * 
     * @throws NullPointerException if the response body is missing required token fields.
     * 
     * @see okhttp3.Request
     * @see okhttp3.Response
     * @see okhttp3.RequestBody
     * @see java.io.IOException
     */
    public boolean refreshToken() {
        if(refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }

        boolean result = false;
        String refreshUrl = urlBuilder.getRefreshURL();
        console.printLine("Refreshing access token...");

        Map<String, String> refreshPayload = new LinkedHashMap<>();
        refreshPayload.put("refreshToken", refreshToken);

        String jsonBody = gson.toJson(refreshPayload);
        RequestBody refreshBody = RequestBody.create(jsonBody, JSON_TYPE);
        Request request = new Request.Builder()
            .url(refreshUrl)
            .post(refreshBody)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                console.printLine("Unable to refresh token: HTTP" + response.code() + " - " + response.body().string());
            }
            else {
                ResponseBody responseBody = response.body();
                try {
                    JsonObject jsonObject = JsonParser.parseString(responseBody.string()).getAsJsonObject();
                    authToken = jsonObject.get("accessToken").getAsString();
                    refreshToken = jsonObject.get("refreshToken").getAsString();

                    result = true;
                }
                catch (NullPointerException e) {
                    console.printLine("Exception parsing refresh token response: " + e.getLocalizedMessage());
                }
            }
        }
        catch (IOException e) {
            console.printLine("Exception trying to refresh token: " + e.getLocalizedMessage());
        }

        return result;
    }    
}
