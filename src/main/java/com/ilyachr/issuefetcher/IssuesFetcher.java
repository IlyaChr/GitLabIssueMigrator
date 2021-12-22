package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class IssuesFetcher extends Fetching<Issue> {

    public IssuesFetcher() {
        super(Issue.class);
    }

    public List<Issue> fetchAllIssues(String projectPath, String projectId, String token) throws IOException {
        return fetchAll(projectPath, projectId, token);
    }

    @Override
    public URL getMainUrl(String projectPath, String projectId) throws MalformedURLException {
        return new URL(projectPath + "/api/v4/projects/" + projectId + "/issues?state=all");
    }

    /**
     * Saving issue to file using Issue API
     */
    public void saveIssueToFile(List<Issue> issues, String projectName) {
        issues.stream().parallel().forEach(Utils.throwingConsumerWrapper(issue -> {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(MessageFormat.format(Utils.ISSUE_PATH_TEMPLATE, projectName, String.valueOf(issue.getIid()), String.valueOf(issue.getIid())) + ".json");
            if (file.getParentFile().mkdirs()) {
                log.debug("Directory {} was created", file.getPath());
            }
            if (file.createNewFile()) {
                log.debug("File {} was successfully created ", file.getName());
            }
            objectMapper.writeValue(file, issue);
        }, IOException.class));
    }

    /**
     * Saving docx files using web scraping
     */
    public void saveIssueUploads(List<Issue> issues, String loginFormUrl, String actionUrl, String userName, String password, String projectPath, String projectName) throws IOException {
        Map<String, String> cookies;
        if ((cookies = signInToGitLab(loginFormUrl, actionUrl, userName, password)) == null) {
            return;
        }

        issues.stream().parallel().filter(issue -> issue.getDescription() != null && !issue.getDescription().isEmpty()).forEach(Utils.throwingConsumerWrapper(issue ->
                fetchFileByDescription(issue.getDescription(), issue.getIid(), projectPath, cookies, projectName), IOException.class));
    }

    /**
     * Trying to find and save docx file using web scraping
     */
    private void fetchFileByDescription(String description, Long iid, String projectPath, Map<String, String> cookies, String projectName) throws IOException {
        Pattern pattern = Pattern.compile("\\((/uploads/.+?/(.+?))\\)");
        Matcher matcher = pattern.matcher(description);
        String link;
        String fileName;

        while (matcher.find()) {
            link = matcher.group(1);
            fileName = matcher.group(2);
            if (link != null && !link.isEmpty()) {
                log.info("download: {} for issue: {}", fileName, iid);
                byte[] fileData = getUploadFile(cookies, projectPath + link);
                if (fileData != null) {
                    saveFile(fileData, iid, fileName, projectName);
                }
            }
        }
    }

    /**
     * Save docx to file system
     */
    private void saveFile(byte[] fileData, Long iid, String fileName, String projectName) throws IOException {
        File file = new File(MessageFormat.format(Utils.ISSUE_PATH_TEMPLATE, projectName, String.valueOf(iid), fileName));
        if (file.getParentFile().mkdirs()) {
            log.debug("Directory {} was created", file.getPath());
        }
        if (file.createNewFile()) {
            log.debug("File {} was successfully created ", file.getName());
        }

        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(fileData);
        }
    }

    /**
     * Get cookies for authentication
     */
    public Map<String, String> signInToGitLab(String loginFormUrl, String actionUrl, String userName, String password) throws IOException {
        HashMap<String, String> formData = new HashMap<>();
        Connection.Response loginForm = Jsoup.connect(loginFormUrl)
                .method(Connection.Method.GET).userAgent(Utils.USER_AGENT).execute();

        Document loginDoc = loginForm.parse();

        String authToken = Objects.requireNonNull(loginDoc.select("#new_user > input[type=hidden]:nth-child(2)")
                        .first())
                .attr("value");

        formData.put("utf8", "e2 9c 93");
        formData.put("user[login]", userName);
        formData.put("user[password]", password);
        formData.put("authenticity_token", authToken);
        formData.put("user[remember_me]", "1");

        Connection.Response signIn = Jsoup.connect(actionUrl)
                .cookies(loginForm.cookies())
                .data(formData)
                .method(Connection.Method.POST)
                .userAgent(Utils.USER_AGENT)
                .execute();

        if (signIn.statusCode() == HttpURLConnection.HTTP_OK) {
            return signIn.cookies();
        }
        return Collections.emptyMap();
    }

    /**
     * Get docx file from GitLab
     */
    private byte[] getUploadFile(Map<String, String> cookies, String uploadLink) throws IOException {
        byte[] fileData = null;

        Connection.Response testResponse = Jsoup.connect(uploadLink)
                .cookies(cookies)
                .ignoreContentType(true)
                .method(Connection.Method.GET)
                .userAgent(Utils.USER_AGENT).execute();

        if (testResponse.statusCode() == HttpURLConnection.HTTP_OK) {
            BufferedInputStream is = testResponse.bodyStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[4];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            fileData = buffer.toByteArray();
        }
        return fileData;
    }

    /**
     * Load issues from file system with attachments
     */
    public List<Issue> loadIssueDataFromDisk(String projectName) {
        List<Issue> issueList = Collections.synchronizedList(new ArrayList<>());
        File projectRoot = new File(MessageFormat.format(Utils.PROJECT_PATH_TEMPLATE, projectName));
        List<String> issueDirectories = projectRoot.list() == null ? Collections.emptyList() : Arrays.asList(Objects.requireNonNull(projectRoot.list()));


        issueDirectories.stream().parallel().forEach(Utils.throwingConsumerWrapper(path -> {
            File file = new File(MessageFormat.format(Utils.ISSUE_FOLDER_PATH_TEMPLATE, projectName, path));
            List<String> issueFiles = file.list() == null ? Collections.emptyList() : Arrays.asList(Objects.requireNonNull(file.list()));

            Issue issue = new Issue();
            ObjectMapper objectMapper = new ObjectMapper();
            for (String issueFile : issueFiles) {
                String docPath = MessageFormat.format(Utils.ISSUE_PATH_TEMPLATE, projectName, path, issueFile);
                if (issueFile.toLowerCase().contains("json")) {
                    List<String> tempDocsPath = issue.getDocsPath();
                    issue = objectMapper.readValue(new File(docPath), Issue.class);
                    issue.getDocsPath().addAll(tempDocsPath);
                    issueList.add(issue);
                } else {
                    issue.addDocPath(docPath);
                }
            }

        }, IOException.class));

        return issueList;
    }
}
