package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
public class IssuesUpdater {

    public void changeIssueState(Issue issue, String projectPath, String projectId, String token) throws IOException {
        URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues/" + issue.getIid());
        HttpURLConnection connection = getConnectionForUrl(url, token, true);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            log.info("issue: {} successfully closed", issue.getIid());
        } else {
            log.error("issue: {}  not closed - responseCode: {} ", issue.getIid(), connection.getResponseCode());
        }
        connection.disconnect();
    }

    public HttpURLConnection getConnectionForUrl(URL url, String token, boolean isClosed) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            String stateEvent = isClosed ? "close" : "reopen";
            String closeRequest = "{\"state_event\": \"" + stateEvent + "\" }";
            byte[] input = closeRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return connection;
    }

}
