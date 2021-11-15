package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;

import static com.ilyachr.issuefetcher.Utils.GitLabEnum.*;

public class Fetcher {

    private static Utils gitLabProperties;
    private static IssuesFetcher issuesFetcher = new IssuesFetcher();
    private static IssuesDeleter issuesDeleter = new IssuesDeleter();
    private static IssuesCreator issuesCreator = new IssuesCreator();

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.println("Select loading or unloading mode");
            System.out.println("Before this config <config.properties> file");
            System.out.println("Press L to load issues from source project");
            System.out.println("Press U to unload issues to destination project");
            System.out.println("Press D to erase all issues in destination project");
            System.out.println("Q to Quit...");

            try {
                gitLabProperties = new Utils();
            } catch (IOException exception) {
                continue;
            }

            String line = scanner.nextLine();

            switch (line.toLowerCase()) {
                case ("l"):
                    loadIssues();
                    return;
                case ("u"):
                    unloadIssues();
                    return;
                case ("d"):
                    System.out.println("Are you sure ? Confirm this by type \"Yes\"");
                    if (scanner.nextLine().equalsIgnoreCase("yes")) {
                        deleteIssues();
                        return;
                    }
                    break;
                case ("q"):
                    return;
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
                    gitLabProperties.getProperty(GITLAB_FROM_TOKEN),
                    gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

            issuesFetcher.fetchAllUploadsIssues(fromIssueList,
                    gitLabProperties.getProperty(GITLAB_FROM_LOGIN_FORM_URL),
                    gitLabProperties.getProperty(GITLAB_FROM_LOGIN_ACTION_URL),
                    gitLabProperties.getProperty(GITLAB_FROM_USERNAME),
                    gitLabProperties.getProperty(GITLAB_FROM_PASSWORD),
                    gitLabProperties.getProperty(GITLAB_FROM_PROJECT_PATH),
                    gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

            Instant finish = Instant.now();
            System.out.println("Elapsed Time in seconds: " + Duration.between(start, finish).getSeconds());
            System.out.println("Total issues fetched : " + fromIssueList.size());
        } catch (IOException e) {
            System.err.println("Error in loading issues");
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
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN),
                    gitLabProperties.getProperty(GITLAB_PROJECT_NAME));


            issuesCreator.createIssues(fromIssueList, toIssueList,
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));

            Instant finish = Instant.now();
            System.out.println("Elapsed Time in seconds: " + Duration.between(start, finish).getSeconds());

        } catch (IOException e) {
            System.err.println("Error in upload issues");
            e.printStackTrace();
        }
    }

    public static void deleteIssues() {
        try {
            List<Issue> toIssueList = issuesFetcher.fetchAllIssues(
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN),
                    gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

            issuesDeleter.deleteIssues(toIssueList,
                    gitLabProperties.getProperty(GITLAB_TO_PATH),
                    gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                    gitLabProperties.getProperty(GITLAB_TO_TOKEN));
        } catch (IOException e) {
            System.err.println("Error in delete issues");
            e.printStackTrace();
        }
    }
}
