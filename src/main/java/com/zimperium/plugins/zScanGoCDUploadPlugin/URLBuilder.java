package com.zimperium.plugins.zScanGoCDUploadPlugin;

public class URLBuilder {

    // API endpoints 
    private final String login_url = "/api/auth/v1/api_keys/login";
    private final String refresh_url = "/api/auth/v1/api_keys/access";
    private final String upload_url = "/api/zdev-upload/public/v1/uploads/build";
    private final String status_url = "/api/zdev-app/public/v1/assessments/status?buildId=";
    private final String teams_url = "/api/auth/public/v1/teams";
    private final String complete_upload_url = "/api/zdev-app/public/v1/apps";
    private final String download_assessment_url = "/api/zdev-app/public/v1/assessments";

    private String baseUrl;

    public URLBuilder(String inBaseUrl) {
        // make sure the last character is not a /
        baseUrl = inBaseUrl.trim().replaceAll("/$", "");
    }

    public String getLoginURL() {
        return baseUrl.concat(login_url); 
    }

    public String getRefreshURL() {
        return baseUrl.concat(refresh_url);
    }

    public String getUploadURL() {
        return baseUrl.concat(upload_url);
    }

    public String getStatusURL(String buildId) {
        return baseUrl.concat(status_url).concat(buildId);
    }

    public String getTeamsURL() {
        return baseUrl.concat(teams_url);
    }

    public String getCompleteUploadURL(String appId) {
        return baseUrl.concat(complete_upload_url + "/" + appId + "/upload");
    }

    public String getDownloadReportURL(String assessmentId, String reportFormat) {
        return baseUrl.concat(download_assessment_url + "/" + assessmentId + "/" + reportFormat);
    }
}
