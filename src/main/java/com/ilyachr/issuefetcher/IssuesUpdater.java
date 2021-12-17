package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class IssuesUpdater {

    public static final String UPDATE_ATTRIBUTE = "state_event";
    public static final String ASSIGNEE_ATTRIBUTE = "assignee_ids";

    public void updateIssue(Issue issue, List<Integer> newAssigneeIds, boolean isClosed, String projectPath, String projectId, String token) throws IOException {
        URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues/" + issue.getIid());

        HttpURLConnection connection = getConnectionForUrl(url, token, newAssigneeIds, isClosed);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            log.info("issue: {} successfully updated", issue.getIid());
        } else {
            log.error("issue: {}  not updated - responseCode: {} ", issue.getIid(), connection.getResponseCode());
        }
        connection.disconnect();
    }

    public HttpURLConnection getConnectionForUrl(URL url, String token, List<Integer> newAssigneeIds, boolean isClosed) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            String stateEvent = isClosed ? "close" : "reopen";
            StringBuilder updateRequest = new StringBuilder();
            if (isClosed) {
                updateRequest.append(getAttributeQuery(UPDATE_ATTRIBUTE, stateEvent));
            }
            if (!newAssigneeIds.isEmpty()) {
                if (updateRequest.length() != 0) {
                    updateRequest.append(",\n");
                }
                updateRequest.append((getAttributeQuery(ASSIGNEE_ATTRIBUTE, newAssigneeIds)));
            }
            updateRequest.insert(0, "{");
            updateRequest.append("}");
            byte[] input = updateRequest.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return connection;
    }

    private String getAttributeQuery(String key, String value) {
        return ("\"" + key + "\" : \"" + value + "\"");
    }

    private String getAttributeQuery(String key, List<Integer> values) {
        StringBuilder query = new StringBuilder("\"" + key + "\" : [");
        for (int i = 0; i < values.size(); i++) {
            query.append(values.get(i));
            if (i != values.size() - 1) {
                query.append(",");
            }
        }
        query.append("]");
        return query.toString();
    }
}
