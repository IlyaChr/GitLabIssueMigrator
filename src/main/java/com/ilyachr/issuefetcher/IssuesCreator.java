package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilyachr.issuefetcher.jackson.Assignee;
import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ilyachr.issuefetcher.IssuesUpdater.*;
import static com.ilyachr.issuefetcher.NotesFetcher.NOTES_FETCHER;

@Slf4j
public class IssuesCreator {
    public void createIssues(List<Issue> fromIssues, List<Issue> toIssues, Map<String, Integer> usersIds, String projectPath, String projectId, String token) {

        Map<Long, Issue> toIssuesMap = toIssues.stream().collect(Collectors.toMap(Issue::getIid, issue -> issue));

        fromIssues.parallelStream().forEach(Utils.throwingConsumerWrapper(issue -> {

            //загружаем вложения и обновляем на них ссылки
            updateDescription(issue, projectPath, projectId, token);

            //создаем issue если такого issue не было
            if (!toIssuesMap.containsKey(issue.getIid())) {
                createNewIssue(issue, usersIds, projectPath, projectId, token);
            } else {
                // иначе обновляем существующую
                updateIssue(issue, toIssuesMap.get(issue.getIid()), usersIds, projectPath, projectId, token);
            }

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

    private List<Integer> getNewAssigneeIdsForIssue(Issue issue, Map<String, Integer> usersIds) {
        List<Integer> newAssigneeIds = new ArrayList<>();
        for (Assignee assignee : issue.getAssignees() != null ? issue.getAssignees() : new Assignee[0]) {
            if (assignee.getName() != null && usersIds.containsKey(assignee.getName())) {
                newAssigneeIds.add(usersIds.get(assignee.getName()));
            }
        }
        return newAssigneeIds;
    }


    private void updateIssue(Issue issue, Issue oldIssue, Map<String, Integer> usersIds, String projectPath, String projectId, String token) throws IOException {
        if (LocalDateTime.parse(issue.getUpdated_at(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .isAfter(LocalDateTime.parse(oldIssue.getUpdated_at(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))) {
            NOTES_FETCHER.updateIssueNotes(issue, oldIssue, projectPath, projectId, token);
            List<Integer> newAssigneeIds = getNewAssigneeIdsForIssue(issue, usersIds);
            ISSUES_UPDATER.updateIssue(
                    issue,
                    ParamForUpdate.builder().
                            issueIid(issue.getIid()).
                            assigneeIds(newAssigneeIds).
                            state(IssueState.getIssueState(issue.getState())).
                            description(issue.getDescription()).
                            title(issue.getTitle()).
                            labels(Arrays.asList(issue.getLabels())).
                            updatedAt(issue.getUpdated_at()).
                            build(),
                    projectPath,
                    projectId,
                    token);
        }
    }

    private void createNewIssue(Issue issue, Map<String, Integer> usersIds, String projectPath, String projectId, String token) throws IOException {
        URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues");
        HttpURLConnection connection = getConnectionForUrl(url, token, issue);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            log.info("issue: {} successfully created", issue.getIid());
            NOTES_FETCHER.createIssueNotes(issue, projectPath, projectId, token);
            List<Integer> newAssigneeIds = getNewAssigneeIdsForIssue(issue, usersIds);
            ISSUES_UPDATER.updateIssue(
                    issue,
                    ParamForUpdate.builder().
                            issueIid(issue.getIid()).
                            assigneeIds(newAssigneeIds).
                            state(IssueState.getIssueState(issue.getState())).
                            updatedAt(issue.getUpdated_at())
                            .build(),
                    projectPath,
                    projectId,
                    token
            );
        } else {
            log.error("issue: {}  not created - responseCode: {}", issue.getIid(), connection.getResponseCode());
        }
        connection.disconnect();
    }


    private void updateDescription(Issue issue, String projectPath, String projectId, String token) throws IOException {
        if (issue.getDocsPath() != null) {
            for (int i = 0; i < issue.getDocsPath().size(); i++) {
                String fileName = issue.getDocsPath().get(i);
                String newNewFilePath = UploadFile.UPLOAD_FILE.uploadFile(fileName, issue.getDocsPath().get(i), projectPath, projectId, token);
                issue.setDescription(getNewDescription(issue.getDescription(), issue.getDocsPath().get(i), newNewFilePath));
            }
        }
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
