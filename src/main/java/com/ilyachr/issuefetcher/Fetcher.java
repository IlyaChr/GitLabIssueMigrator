package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;
import com.ilyachr.issuefetcher.jackson.User;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;

import static com.ilyachr.issuefetcher.Utils.GitLabEnum.*;


@Slf4j
public class Fetcher {

    private static Utils gitLabProperties;
    private static final IssuesFetcher issuesFetcher = new IssuesFetcher();
    private static final IssuesDeleter issuesDeleter = new IssuesDeleter();
    private static final IssuesCreator issuesCreator = new IssuesCreator();
    private static final UsersFetcher usersFetcher = new UsersFetcher();

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            log.info("Before procedure check config <config.properties> file");
            log.info("Press L to load issues from source project");
            log.info("Press U to unload issues to destination project");
            log.info("Press D to erase all issues in destination project");
            log.info("Q to Quit...");

            try {
                gitLabProperties = new Utils();
                gitLabProperties.disableSslVerification();
            } catch (IOException exception) {
                continue;
            }

            String line = scanner.nextLine();

            switch (line.toLowerCase()) {
                case ("l"):
                    loadIssues();
                    break;
                case ("u"):
                    unloadIssues();
                    break;
                case ("d"):
                    log.info("Are you sure ? Confirm this by type \"Yes\"");
                    if (scanner.nextLine().equalsIgnoreCase("yes")) {
                        deleteIssues();
                    }
                    break;
                case ("q"):
                    return;
                default:
            }
        }
    }

    public static void loadIssues() {
        List<Issue> fromIssueList;
        try {
            Instant start = Instant.now();

            fromIssueList = issuesFetcher.fetchAllIssues(
                    gitLabProperties.getProperty(GITLAB_FROM_PATH),
                    gitLabProperties.getProperty(GITLAB_FROM_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_FROM_TOKEN));

            issuesFetcher.saveIssueToFile(fromIssueList, gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

            issuesFetcher.saveIssueUploads(fromIssueList,
                    gitLabProperties.getProperty(GITLAB_FROM_LOGIN_FORM_URL),
                    gitLabProperties.getProperty(GITLAB_FROM_LOGIN_ACTION_URL),
                    gitLabProperties.getProperty(GITLAB_FROM_USERNAME),
                    gitLabProperties.getProperty(GITLAB_FROM_PASSWORD),
                    gitLabProperties.getProperty(GITLAB_FROM_PROJECT_PATH),
                    gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

            Instant finish = Instant.now();
            log.info("Elapsed Time in seconds: {} ", Duration.between(start, finish).getSeconds());
            log.info("Total issues fetched : {} ", fromIssueList.size());
        } catch (IOException e) {
            log.error("Error in loading issues");
            e.printStackTrace();
        }
    }

    public static void unloadIssues() {
        try {
            Instant start = Instant.now();

            List<Issue> fromIssueList = issuesFetcher.loadIssueDataFromDisk(gitLabProperties.getProperty(GITLAB_PROJECT_NAME));
            List<Issue> toIssueList = issuesFetcher.fetchAllIssues(
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));

            List<User> toUsersList = usersFetcher.getAllUsers(gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));

            issuesCreator.createIssues(fromIssueList, toIssueList, toUsersList,
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));

            Instant finish = Instant.now();
            log.info("Elapsed Time in seconds: {}", Duration.between(start, finish).getSeconds());

        } catch (IOException e) {
            log.error("Error in upload issues");
            e.printStackTrace();
        }
    }

    public static void deleteIssues() {
        try {
            List<Issue> toIssueList = issuesFetcher.fetchAllIssues(
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));

            issuesDeleter.deleteIssues(toIssueList,
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));
        } catch (IOException e) {
            log.error("Error in delete issues");
            e.printStackTrace();
        }
    }
}
