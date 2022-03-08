package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class RestApi<T> {

    private Class<T> type;

    public RestApi(Class<T> type) {
        this.type = type;
    }

    @Data
    @Builder
    public static class RestQueryParam {
        String projectPath;
        String projectId;
        String groupId;
        String issueIid;
        String notesIid;
        String epicIid;
        String token;
    }


    private List<URL> getAllPages(RestQueryParam queryParam) throws IOException {

        List<URL> pages = new ArrayList<>();
        URL url = getUrl(queryParam);
        HttpURLConnection connection = getGetQuery(url, queryParam);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            pages.add(url);

            Pattern pattern = Pattern.compile("<(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*))>; rel=\"next\"");
            Matcher matcher;

            while ((matcher = pattern.matcher(getHeaderField(connection))).find()) {
                String page = matcher.group(1);
                log.info("fetching page: {}", page);
                url = new URL(matcher.group(1));
                connection = getGetQuery(url, queryParam);
                pages.add(url);
            }
        }
        return pages;
    }

    public List<T> createGetRequest(RestQueryParam queryParam) throws IOException {
        List<T> list = Collections.synchronizedList(new ArrayList<>());

        getAllPages(queryParam).stream().parallel().forEach(Utils.throwingConsumerWrapper(url -> {
            HttpURLConnection connection = getGetQuery(url, queryParam);
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
                log.error("Failed to create GET request with status {}", connection.getResponseCode());
            }
            connection.disconnect();

        }, IOException.class));

        return list;
    }

    public void createPostRequest(T object, RestQueryParam queryParam, Consumer<T> afterSuccessConsumer) {
        HttpURLConnection connection;
        try {
            connection = getPostQuery(queryParam, object);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                afterSuccessConsumer.accept(object);
            } else {
                log.error("Failed to create POST request with status {}", connection.getResponseCode());
            }
            connection.disconnect();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void createDeleteRequest(RestQueryParam queryParam) {
        HttpURLConnection connection;
        try {
            connection = getDeleteQuery(queryParam);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                log.error("Failed to create DELETE request with status {}", connection.getResponseCode());
            }
            connection.disconnect();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void createPutRequest(T object, RestQueryParam queryParam) {
        HttpURLConnection connection;
        try {
            connection = getPutQuery(queryParam, object);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.error("Failed to create PUT request with status {}", connection.getResponseCode());
            }
            connection.disconnect();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private String getHeaderField(HttpURLConnection connection) throws IOException {
        String header = "";
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            header = connection.getHeaderField("Link");
        }
        connection.disconnect();
        return header;
    }

    public URL getUrl(RestQueryParam queryParam) throws MalformedURLException {
        throw new UnsupportedOperationException("implement \"getUrl\" method");
    }


    private HttpURLConnection getGetQuery(URL url, RestQueryParam queryParam) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("PRIVATE-TOKEN", queryParam.getToken());
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private HttpURLConnection getPostQuery(RestQueryParam queryParam, T object) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getUrl(queryParam).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("PRIVATE-TOKEN", queryParam.getToken());
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        ObjectMapper objectMapper = new ObjectMapper();
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsBytes(object);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public HttpURLConnection getDeleteQuery(RestQueryParam queryParam) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getUrl(queryParam).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("PRIVATE-TOKEN", queryParam.getToken());
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private HttpURLConnection getPutQuery(RestQueryParam queryParam, T object) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getUrl(queryParam).openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("PRIVATE-TOKEN", queryParam.getToken());
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        ObjectMapper objectMapper = new ObjectMapper();
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsBytes(object);
            os.write(input, 0, input.length);
        }
        return connection;
    }

}
