package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Assignee;
import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ilyachr.issuefetcher.FieldUpdater.*;

@Slf4j
public class IssuesCreator extends RestApi<Issue> {

    @Getter
    private static final IssuesCreator instance = new IssuesCreator();

    @Getter
    private AtomicInteger countUpdated = new AtomicInteger(0);
    @Getter
    private AtomicInteger countCreated = new AtomicInteger(0);

    private IssuesCreator() {
        super(Issue.class);
    }

    public void createIssues(List<Issue> fromIssues, List<Issue> toIssues, Map<String, Integer> usersIds, String projectPath, String projectId, String token) {

        Map<Long, Issue> toIssuesMap = toIssues.stream().collect(Collectors.toMap(Issue::getIid, issue -> issue));

        fromIssues.parallelStream().forEach(Utils.throwingConsumerWrapper(issue -> {

            //создаем issue если такого issue не было
            if (!toIssuesMap.containsKey(issue.getIid())) {
                createNewIssue(issue, usersIds, projectPath, projectId, token);
            } else {
                // иначе обновляем существующую
                updateIssue(issue, toIssuesMap.get(issue.getIid()), usersIds, projectPath, projectId, token);
            }

        }, IOException.class));

        log.info("Total created issue : " + countCreated.get());
        log.info("Total updated issue : " + countUpdated.get());
    }

    @Override
    public URL getUrl(RestQueryParam queryParam) throws MalformedURLException {
        return queryParam.getIssueIid() == null ?
                new URL(queryParam.getProjectPath() + "/api/v4/projects/" + queryParam.getProjectId() + "/issues") :
                new URL(queryParam.getProjectPath() + "/api/v4/projects/" + queryParam.getProjectId() + "/issues/" + queryParam.getIssueIid());
    }

    private void createNewIssue(Issue issue, Map<String, Integer> usersIds, String projectPath, String projectId, String token) throws IOException {

        //загружаем вложения и обновляем на них ссылки
        updateDescription(issue, projectPath, projectId, token);

        createPostRequest(issue,
                RestQueryParam.builder().projectPath(projectPath).projectId(projectId).token(token).build(),
                Utils.throwingConsumerWrapper(i -> {

                    RestQueryParam queryParam = RestQueryParam.builder().projectPath(projectPath).projectId(projectId).token(token).issueIid(i.getIid().toString()).build();

                    NotesFactory.getInstance().createIssueNotes(issue, projectPath, projectId, token);
                    List<Integer> newAssigneeIds = getNewAssigneeIdsForIssue(issue, usersIds);
                    FIELD_UPDATER.update(
                            getUrl(queryParam), queryParam,
                            ParamForUpdate.builder().
                                    issueIid(issue.getIid()).
                                    assigneeIds(newAssigneeIds).
                                    state(IssueState.getIssueState(issue.getState())).
                                    updatedAt(issue.getUpdated_at())
                                    .build());

                    getCountCreated().incrementAndGet();

                }, IOException.class));
    }

    private void updateIssue(Issue issue, Issue oldIssue, Map<String, Integer> usersIds, String projectPath, String projectId, String token) throws IOException {

        LocalDateTime newIssueUpdatedTime = LocalDateTime.parse(issue.getUpdated_at(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDateTime oldIssueUpdatedTime = LocalDateTime.parse(oldIssue.getUpdated_at(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        if (newIssueUpdatedTime.isAfter(oldIssueUpdatedTime)) {

            //загружаем вложения и обновляем на них ссылки
            updateDescription(issue, projectPath, projectId, token);

            NotesFactory.getInstance().updateIssueNotes(issue, oldIssue, projectPath, projectId, token);
            List<Integer> newAssigneeIds = getNewAssigneeIdsForIssue(issue, usersIds);

            RestQueryParam queryParam = RestQueryParam.builder().projectPath(projectPath).projectId(projectId).token(token).issueIid(issue.getIid().toString()).build();

            FIELD_UPDATER.update(
                    getUrl(queryParam), queryParam,
                    ParamForUpdate.builder().
                            issueIid(issue.getIid()).
                            assigneeIds(newAssigneeIds).
                            state(IssueState.getIssueState(issue.getState())).
                            description(issue.getDescription()).
                            title(issue.getTitle()).
                            labels(Arrays.asList(issue.getLabels())).
                            updatedAt(issue.getUpdated_at()).
                            build());

            getCountUpdated().incrementAndGet();
        }
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
