package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilyachr.issuefetcher.jackson.Assignee;
import com.ilyachr.issuefetcher.jackson.Issue;
import com.ilyachr.issuefetcher.jackson.User;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class IssuesCreator {
    public void createIssues(List<Issue> fromIssues, List<Issue> toIssues, List<User> toUsersList, String projectPath, String projectId, String token) {
        IssuesUpdater issuesUpdater = new IssuesUpdater();
        UploadFile uploadFile = new UploadFile();

        fromIssues.parallelStream().filter(i -> !toIssues.contains(i)).forEach(Utils.throwingConsumerWrapper(issue -> {
            if (issue.getDocsPath() != null) {

                for (int i = 0; i < issue.getDocsPath().size(); i++) {
                    //String newFileName = issue.getIid().toString() + "_" + i + ".docx";
                    String fileName = issue.getDocsPath().get(i);
                    String newNewFilePath = uploadFile.uploadFile(fileName, issue.getDocsPath().get(i), projectPath, projectId, token);
                    issue.setDescription(getNewDescription(issue.getDescription(), issue.getDocsPath().get(i), newNewFilePath));
                }
            }
            URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues");
            HttpURLConnection connection = getConnectionForUrl(url, token, issue);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                log.info("issue: {} successfully created", issue.getIid());
                List<User> newAssignees = getNewAssigneeForIssue(issue, toUsersList);
                boolean isClosed = issue.getState().equals("closed");
                if (!newAssignees.isEmpty() || isClosed) {
                    issuesUpdater.updateIssue(issue, newAssignees, issue.getState().equals("closed"), projectPath, projectId, token);
                }
            } else {
                log.error("issue: {}  not created - responseCode: {}", issue.getIid(), connection.getResponseCode());
            }

            connection.disconnect();

        }, IOException.class));


    }

    public HttpURLConnection getConnectionForUrl(URL url, String token, Issue issue) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        ObjectMapper objectMapper = new ObjectMapper();

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsBytes(issue);
            os.write(input, 0, input.length);
        }

        return connection;
    }

    private List<User> getNewAssigneeForIssue(Issue issue, List<User> toUsersList) {
        List<User> users = new ArrayList<>();
        for (User userFromList : toUsersList) {
            for (Assignee assignee : issue.getAssignees()) {
                if (assignee.getName() != null &&
                        userFromList.getName() != null &&
                        userFromList.getName().equals(assignee.getName())) {
                    users.add(userFromList);
                }
            }
        }
        return users;
    }


    private String getNewDescription(String description, String oldFilePath, String newFilePath) {

        Pattern patternFilePath = Pattern.compile(".+[/\\\\](.+)[/\\\\](.+)");
        Matcher fileOldPathMatcher = patternFilePath.matcher(oldFilePath);
        Matcher fileNewPathMatcher = patternFilePath.matcher(newFilePath);

        if (fileOldPathMatcher.find() && fileNewPathMatcher.find()) {
            return description.replaceFirst("/uploads/(.+)/" + fileOldPathMatcher.group(2),
                    "/uploads/" + fileNewPathMatcher.group(1) + "/" + fileNewPathMatcher.group(2));
        }

        return description;
    }

}
