package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class IssuesDeleter {

    public void deleteIssues(List<Issue> issues, String projectPath, String projectId, String token) throws IOException {
        issues.stream().parallel().forEach(Utils.throwingConsumerWrapper(i -> deleteIssue(projectPath, projectId, token, i.getIid()), IOException.class));
    }

    public void deleteIssue(String projectPath, String projectId, String token, Long issueIid) throws IOException {
        URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues/" + issueIid);
        HttpURLConnection connection = getConnectionForUrl(url, token);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT){
            System.out.println("issue: " + issueIid + " successfully deleted");
        }else{
            System.err.println("issue: " + issueIid + " not deleted - responseCode: " + connection.getResponseCode());
        }
        connection.disconnect();
    }

    public HttpURLConnection getConnectionForUrl(URL url, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

}
