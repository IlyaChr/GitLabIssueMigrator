package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Slf4j
public class IssuesDeleter extends RestApi<Issue> {

    @Getter
    private static final IssuesDeleter instance = new IssuesDeleter();

    private IssuesDeleter() {
        super(Issue.class);
    }

    @Override
    public URL getUrl(RestQueryParam queryParam) throws MalformedURLException {
        return new URL(queryParam.getProjectPath() + "/api/v4/projects/" + queryParam.getProjectId() + "/issues/" + queryParam.getIssueIid());
    }

    public void deleteIssues(List<Issue> issues, String projectPath, String projectId, String token) {
        issues.stream().parallel().forEach(issue -> createDeleteRequest(RestQueryParam.builder().
                projectPath(projectPath).
                projectId(projectId).
                issueIid(issue.getIid().toString()).
                token(token).
                build()));
    }
}
