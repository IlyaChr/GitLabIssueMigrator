package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Epic;
import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;


import javax.naming.AuthenticationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.ilyachr.issuefetcher.Utils.GitLabEnum.*;


@Slf4j
public class Fetcher {

    private static Utils gitLabProperties;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            try {
                gitLabProperties = new Utils();
                if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(SSL_DISABLE))) {
                    gitLabProperties.disableSslVerification();
                }
            } catch (IOException | URISyntaxException exception) {
                log.error("Error in config file");
                return;
            }

            log.info(" ------------ GitLab Parameters ------------ ");
            for (Utils.GitLabEnum labEnum : Utils.GitLabEnum.values()) {
                log.info(" {} : {} ", labEnum, gitLabProperties.getProperty(labEnum));
            }
            log.info(" ------------------------------------------- ");

            log.info("Before procedure check config <config.properties> file");
            log.info("Type export to export issues from source project");
            log.info("Type import to import issues to destination project");
            log.info("Type erase to erase all issues in destination project (deprecated)");
            log.info("Q to Quit...");


            String line = scanner.nextLine();

            switch (line.toLowerCase()) {
                case ("export"):
                    issueExport();
                    break;
                case ("import"):
                    issueImport();
                    break;
                case ("erase"):
                    log.info("Are you sure ? Confirm this by type \"Yes\"");
                    if (scanner.nextLine().equalsIgnoreCase("yes")) {
                        eraseAll();
                    }
                    break;
                case ("q"):
                    return;
                default:
            }
        }
    }

    public static void issueExport() {
        List<Issue> fromIssueList;
        List<Epic> fromEpicList;
        try {
            Instant start = Instant.now();

            if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(EPICS_ENABLE))) {
                fromEpicList = EpicsFactory.getInstance().fetchEpics(
                        gitLabProperties.getProperty(GITLAB_FROM_PATH),
                        gitLabProperties.getProperty(GITLAB_FROM_GROUP_ID),
                        gitLabProperties.getProperty(GITLAB_FROM_TOKEN));

                EpicsFactory.getInstance().saveEpicToFile(fromEpicList);

                log.debug("Total epics fetched : {} ", fromEpicList.size());
            }

            if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(ISSUE_ENABLE))) {
                fromIssueList = IssuesFetcher.getInstance().fetchAllIssues(
                        gitLabProperties.getProperty(GITLAB_FROM_PATH),
                        gitLabProperties.getProperty(GITLAB_FROM_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_FROM_TOKEN));

                NotesFactory.getInstance().setIssueNotes(fromIssueList,
                        gitLabProperties.getProperty(GITLAB_FROM_PATH),
                        gitLabProperties.getProperty(GITLAB_FROM_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_FROM_TOKEN));

                IssuesFetcher.getInstance().saveIssueToFile(fromIssueList, gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

                IssuesFetcher.getInstance().saveIssueUploads(fromIssueList,
                        gitLabProperties.getProperty(GITLAB_FROM_LOGIN_FORM_URL),
                        gitLabProperties.getProperty(GITLAB_FROM_LOGIN_ACTION_URL),
                        gitLabProperties.getProperty(GITLAB_FROM_USERNAME),
                        gitLabProperties.getProperty(GITLAB_FROM_PASSWORD),
                        gitLabProperties.getProperty(GITLAB_FROM_PROJECT_PATH),
                        gitLabProperties.getProperty(GITLAB_PROJECT_NAME));

                log.debug("Total issues fetched : {} ", fromIssueList.size());
            }


            Instant finish = Instant.now();
            log.debug("Elapsed Time in seconds: {} ", Duration.between(start, finish).getSeconds());
        } catch (IOException e) {
            log.error("Error in export: {}", e.getMessage());
        } catch (AuthenticationException e) {
            log.error("Error in loading docs - Password or login to GitLab is incorrect - {}", e.getMessage());
        }
    }

    public static void issueImport() {
        try {
            Instant start = Instant.now();

            if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(EPICS_ENABLE))) {
                List<Epic> fromEpicList = EpicsFactory.getInstance().loadEpicsDataFromDisk();

                List<Epic> toEpicList = EpicsFactory.getInstance().fetchEpics(
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_GROUP_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                EpicsFactory.getInstance().createEpics(fromEpicList,toEpicList,
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_GROUP_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));
            }

            if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(ISSUE_ENABLE))) {
                List<Issue> fromIssueList = IssuesFetcher.getInstance().loadIssueDataFromDisk(gitLabProperties.getProperty(GITLAB_PROJECT_NAME));
                List<Issue> toIssueList = IssuesFetcher.getInstance().fetchAllIssues(
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                NotesFactory.getInstance().setIssueNotes(toIssueList,
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                Map<String, Integer> usersIds = UsersFetcher.getInstance().getAllUsers(gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                IssuesCreator.getInstance().createIssues(fromIssueList, toIssueList, usersIds,
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));
            }

            Instant finish = Instant.now();
            log.debug("Elapsed Time in seconds: {}", Duration.between(start, finish).getSeconds());

        } catch (IOException e) {
            log.error("Error in upload issues - {}", e.getMessage());
        }
    }

    public static void eraseAll() {
        try {
            if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(ISSUE_ENABLE))) {
                List<Issue> toIssueList = IssuesFetcher.getInstance().fetchAllIssues(
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                IssuesDeleter.getInstance().deleteIssues(toIssueList,
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_PROJECT_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));
            }


            if ("TRUE".equalsIgnoreCase(gitLabProperties.getProperty(EPICS_ENABLE))) {
                List<Epic> toEpicList = EpicsFactory.getInstance().fetchEpics(
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_GROUP_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));

                EpicsFactory.getInstance().deleteEpics(toEpicList,
                        gitLabProperties.getProperty(GITLAB_TO_PATH),
                        gitLabProperties.getProperty(GITLAB_TO_GROUP_ID),
                        gitLabProperties.getProperty(GITLAB_TO_TOKEN));
            }


        } catch (IOException e) {
            log.error("Error in delete issues - {}", e.getMessage());
        }
    }
}
