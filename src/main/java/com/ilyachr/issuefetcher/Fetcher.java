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

    public static void main(String[] args) {
        IssuesFetcher issuesFetcher = new IssuesFetcher();
        IssuesDeleter issuesDeleter = new IssuesDeleter();
        IssuesCreator issuesCreator = new IssuesCreator();

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.println("Select loading or unloading mode");
            System.out.println("Before this config <config.properties> file");
            System.out.println("Press L to load or U to unload issues...");

            try {
                gitLabProperties = new Utils();
            } catch (IOException exception) {
                continue;
            }

            String line = scanner.nextLine();

            if (line.equals("L")) {
                List<Issue> kazanGitlabIssueList;
                try {
                    Instant start = Instant.now();

                    kazanGitlabIssueList = issuesFetcher.fetchAllIssues(
                            gitLabProperties.getProperty(GITLAB_FROM_PATH),
                            gitLabProperties.getProperty(GITLAB_FROM_PROJECT_ID),
                            gitLabProperties.getProperty(GITLAB_FROM_TOKEN));

                    issuesFetcher.fetchAllUploadsIssues(kazanGitlabIssueList,
                            gitLabProperties.getProperty(GITLAB_FROM_LOGIN_FORM_URL),
                            gitLabProperties.getProperty(GITLAB_FROM_LOGIN_ACTION_URL),
                            gitLabProperties.getProperty(GITLAB_FROM_USERNAME),
                            gitLabProperties.getProperty(GITLAB_FROM_PASSWORD),
                            gitLabProperties.getProperty(GITLAB_FROM_PROJECT_PATH));

                    Instant finish = Instant.now();
                    System.out.println("Elapsed Time in seconds: " + Duration.between(start, finish).getSeconds());
                    System.out.println("Total issues fetched : " + kazanGitlabIssueList.size());
                } catch (IOException e) {
                    System.err.println("Error in loading issues");
                    e.printStackTrace();
                }
                return;

            } else if (line.equals("U")) {
                try {
                    Instant start = Instant.now();

                    List<Issue> fromIssueList = issuesFetcher.loadIssueDataFromDisk();
                    List<Issue> toIssueList = issuesFetcher.fetchAllIssues(
                            gitLabProperties.getProperty(GITLAB_TO_PATH),
                            gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                            gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                    //To clean all issues (apply carefully!)
                    /*if (!toIssueList.isEmpty()) {
                        issuesDeleter.deleteIssues(toIssueList,
                                gitLabProperties.getProperty(GITLAB_TO_PATH),
                                gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                                gitLabProperties.getProperty(GITLAB_TO_TOKEN));
                    }*/

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
                return;
            }


        }

    }

}
