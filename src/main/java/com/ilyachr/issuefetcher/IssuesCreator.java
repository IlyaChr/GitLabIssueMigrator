package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilyachr.issuefetcher.jackson.Issue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssuesCreator {

    public void createIssues(List<Issue> fromIssues, List<Issue> toIssues, String projectPath, String projectId, String token) throws IOException {
        IssuesUpdater issuesUpdater = new IssuesUpdater();
        UploadFile uploadFile = new UploadFile();

        fromIssues.parallelStream().filter(i -> !toIssues.contains(i)).forEach(Utils.throwingConsumerWrapper(issue -> {
            if (issue.getDocsPath() != null) {
                for (String docPath : issue.getDocsPath()) {
                    String newNewFilePath = uploadFile.uploadFile(docPath, projectPath, projectId, token);
                    issue.setDescription(getNewDescription(issue.getDescription(), newNewFilePath));
                }
            }
            URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues");
            HttpURLConnection connection = getConnectionForUrl(url, token, issue);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                if (issue.getState().equals("closed")) {
                    issuesUpdater.closeIssue(issue, projectPath, projectId, token);
                }
                System.out.println("issue: " + issue.getIid() + " successfully created");
            } else {
                System.err.println("issue: " + issue.getIid() + " not created - responseCode: " + connection.getResponseCode());
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

    private String getNewDescription(String description, String newFilePath) {
        Pattern pattern = Pattern.compile("(/uploads/.+?/(.+?).docx)");
        Matcher matcher = pattern.matcher(description);
        Matcher matcherNewFilePath = pattern.matcher(newFilePath);
        matcherNewFilePath.find();
        String fileName;
        while (matcher.find()) {
            fileName = matcher.group(2);
            if (fileName.equals(matcherNewFilePath.group(2))) {
                return description.replaceFirst("/uploads/.+?/" + fileName + ".docx", newFilePath);
            }
        }
        return description.replaceFirst("/uploads/.+?/(.+?).docx", newFilePath);
    }

}
