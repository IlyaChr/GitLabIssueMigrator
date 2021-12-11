package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class IssuesFetcher {

    public List<Issue> fetchAllIssues(String projectPath, String projectId, String token, String projectName) throws IOException {

        URL url = new URL(projectPath + "/api/v4/projects/" + projectId + "/issues?state=all");
        HttpURLConnection connection = getConnectionForUrl(url, token);
        List<Issue> issues = getIssuesForConnection(connection, projectName);

        Pattern pattern = Pattern.compile("<(https://.{50,200}?)>; rel=\"next\"");
        Matcher matcher;

        while ((matcher = pattern.matcher(connection.getHeaderField("Link"))).find()) {
            String page = matcher.group(1);
            log.info("fetching on page: {}", page);
            url = new URL(matcher.group(1));
            connection = getConnectionForUrl(url, token);
            issues.addAll(getIssuesForConnection(connection, projectName));
        }

        return issues;
    }


    public void fetchAllUploadsIssues(List<Issue> issues, String loginFormUrl, String actionUrl, String userName, String password, String projectPath, String projectName) throws IOException {
        Map<String, String> cookies;
        if ((cookies = signInToGitLab(loginFormUrl, actionUrl, userName, password)) == null) {
            return;
        }

        issues.stream().parallel().filter(issue -> issue.getDescription() != null && !issue.getDescription().isEmpty()).forEach(Utils.throwingConsumerWrapper(issue ->
                fetchFileByDescription(issue.getDescription(), issue.getIid(), projectPath, cookies, projectName), IOException.class));

    }

    public HttpURLConnection getConnectionForUrl(URL url, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private List<Issue> getIssuesForConnection(HttpURLConnection connection, String projectName) throws IOException {
        List<Issue> issues = new ArrayList<>();
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            ObjectMapper objectMapper = new ObjectMapper();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            issues = objectMapper.readValue(response.toString(), new TypeReference<List<Issue>>() {
            });
            connection.disconnect();

            issues.stream().parallel().forEach(Utils.throwingConsumerWrapper(issue -> {
                File file = new File(projectName + "\\" + issue.getIid() + "\\" + issue.getIid() + ".json");
                file.getParentFile().mkdirs();
                if (!file.exists()) {
                    file.createNewFile();
                }
                objectMapper.writeValue(file, issue);
            }, IOException.class));
        }
        return issues;
    }

    private void fetchFileByDescription(String description, Long iid, String projectPath, Map<String, String> cookies, String projectName) throws IOException {
        Pattern pattern = Pattern.compile("(/uploads/.+?/(.+?).docx)");
        Matcher matcher = pattern.matcher(description);
        String link, fileName;

        while (matcher.find()) {
            link = matcher.group(1);
            fileName = matcher.group(2);
            if (link != null && !link.isEmpty()) {
                log.info("download: " + fileName + " for issue: " + iid.toString());
                byte[] fileData = getUploadFile(cookies, projectPath + link);
                if (fileData != null) {
                    saveFile(fileData, iid, fileName, projectName);
                }
            }
        }
    }


    private void saveFile(byte[] fileData, Long iid, String fileName, String projectName) throws IOException {
        File file = new File(projectName + "\\" + iid.toString() + "\\" + fileName + ".docx");
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(file, false);
        outputStream.write(fileData);
        outputStream.close();
    }

    public Map<String, String> signInToGitLab(String loginFormUrl, String actionUrl, String userName, String password) throws IOException {
        HashMap<String, String> formData = new HashMap<>();
        Connection.Response loginForm = Jsoup.connect(loginFormUrl)
                .method(Connection.Method.GET).userAgent(Utils.USER_AGENT).execute();

        Document loginDoc = loginForm.parse();

        String authToken = loginDoc.select("#new_user > input[type=hidden]:nth-child(2)")
                .first()
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
        return null;
    }

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

    public List<Issue> loadIssueDataFromDisk(String projectName) throws IOException {
        List<Issue> issueList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(projectName);
        List<String> issueDirectories = file.list() == null ? Collections.emptyList() : Arrays.asList(Objects.requireNonNull(file.list()));

        for (String path : issueDirectories) {
            file = new File(projectName + "\\" + path);
            List<String> issueFiles = file.list() == null ? Collections.emptyList() : Arrays.asList(Objects.requireNonNull(file.list()));

            Issue issue = null;

            for (String issueFile : issueFiles) {
                if (issueFile.contains("json")) {
                    List<String> docsPath = new ArrayList<>();
                    if (issue != null) {
                        docsPath = issue.getDocsPath();
                    }
                    issue = objectMapper.readValue(new File(projectName + "\\" + path + "\\" + issueFile), Issue.class);
                    if (docsPath != null && !docsPath.isEmpty()) {
                        issue.getDocsPath().addAll(docsPath);
                    }
                    issueList.add(issue);

                } else if (issueFile.contains("docx")) {
                    String docPath = projectName + "\\" + path + "\\" + issueFile;
                    if (issue == null) {
                        issue = new Issue();
                    }
                    issue.addDocPath(docPath);
                }
            }

        }
        return issueList;
    }
}
