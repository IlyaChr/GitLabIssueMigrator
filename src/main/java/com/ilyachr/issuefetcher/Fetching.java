package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class Fetching<T> {

    private Class<T> type;

    public Fetching(Class<T> type) {
        this.type = type;
    }

    private List<URL> getAllPages(String projectPath, String projectId, String token) throws IOException {

        List<URL> pages = new ArrayList<>();
        URL url = getMainUrl(projectPath, projectId);
        HttpURLConnection connection = getConnectionForUrl(url, token);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            pages.add(url);

            Pattern pattern = Pattern.compile("<(https://.{50,150}?)>; rel=\"next\"");
            Matcher matcher;

            while ((matcher = pattern.matcher(getHeaderField(connection))).find()) {
                String page = matcher.group(1);
                log.debug("fetching page: {}", page);
                url = new URL(matcher.group(1));
                connection = getConnectionForUrl(url, token);
                pages.add(url);
            }
        }


        return pages;
    }

    public List<T> fetchAll(String projectPath, String projectId, String token) throws IOException {
        List<T> list = Collections.synchronizedList(new ArrayList<>());

        getAllPages(projectPath, projectId, token).stream().parallel().forEach(Utils.throwingConsumerWrapper(url -> {
            HttpURLConnection connection = getConnectionForUrl(url, token);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                ObjectMapper objectMapper = new ObjectMapper();

                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, type);
                list.addAll(objectMapper.readValue(response.toString(), listType));

            } else {
                log.error("Failed to fetched with status {}", connection.getResponseCode());
            }
            connection.disconnect();

        }, IOException.class));

        return list;
    }


    private HttpURLConnection getConnectionForUrl(URL url, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private String getHeaderField(HttpURLConnection connection) throws IOException {
        String header = "";
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            header = connection.getHeaderField("Link");
        }
        connection.disconnect();
        return header;
    }

    public abstract URL getMainUrl(String projectPath, String projectId) throws MalformedURLException;

}
