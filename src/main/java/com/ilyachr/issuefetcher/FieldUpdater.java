package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
public enum FieldUpdater {
    FIELD_UPDATER;

    @Data
    @Builder
    public static class ParamForUpdate {
        private IssueState state;
        private Long issueIid;
        private String description;
        private List<Integer> assigneeIds;
        private List<String> labels;
        private String title;
        private String updatedAt;
        private String epicIId;
    }

    public enum IssueState {
        CLOSE("closed", "close"),
        OPEN("opened", "reopen");

        private String state;
        private String action;

        IssueState(String state, String action) {
            this.state = state;
            this.action = action;
        }

        public String getState() {
            return state;
        }

        public String getAction() {
            return action;
        }

        public static IssueState getIssueState(String state) {
            return Arrays.stream(IssueState.values()).filter(s -> s.state.equals(state)).findFirst().orElse(null);
        }

    }

    private enum Attribute {
        STATE("state_event"),
        ASSIGNEE("assignee_ids"),
        DESCRIPTION("description"),
        LABELS("labels"),
        TITLE("title"),
        UPDATED_DATE("updated_at"),
        EPIC_IID("epic_iid");

        private String name;

        Attribute(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public void update(URL url, RestApi.RestQueryParam queryParam, ParamForUpdate param) throws IOException {
        HttpURLConnection connection = getConnectionForUrl(url, queryParam.getToken(), param);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            log.info("successfully updated: with param {}", queryParam);
        } else {
            log.error("not updated - responseCode: {} and param {}", connection.getResponseCode() , queryParam);
        }
        connection.disconnect();
    }

    public HttpURLConnection getConnectionForUrl(URL url, String token, ParamForUpdate param) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("PRIVATE-TOKEN", token);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            StringBuilder updateRequest = new StringBuilder();

            ObjectMapper objectMapper = new ObjectMapper();


            if (param.getState()!= null && !param.getState().getState().isEmpty()) {
                addComma(updateRequest).append(getAttributeQuery(Attribute.STATE.getName(), objectMapper.writeValueAsString(param.getState().getAction())));
            }

            if (!isEmpty(param.getAssigneeIds())) {
                addComma(updateRequest).append(getIntegerArrayAttributeQuery(Attribute.ASSIGNEE.getName(), param.getAssigneeIds()));
            }

            if (!StringUtils.isEmpty(param.getDescription())) {
                addComma(updateRequest).append(getAttributeQuery(Attribute.DESCRIPTION.getName(), objectMapper.writeValueAsString(param.getDescription())));
            }

            if (!isEmpty(param.getLabels())) {
                addComma(updateRequest).append(getStringArrayAttributeQuery(Attribute.LABELS.getName(), param.getLabels()));
            }

            if (!StringUtils.isEmpty(param.getTitle())) {
                addComma(updateRequest).append(getAttributeQuery(Attribute.TITLE.getName(), objectMapper.writeValueAsString(param.getTitle())));
            }

            if (!StringUtils.isEmpty(param.getUpdatedAt())) {
                addComma(updateRequest).append(getAttributeQuery(Attribute.UPDATED_DATE.getName(), objectMapper.writeValueAsString(param.getUpdatedAt())));
            }

            if (!StringUtils.isEmpty(param.getEpicIId())) {
                addComma(updateRequest).append(getAttributeQuery(Attribute.EPIC_IID.getName(), objectMapper.writeValueAsString(param.getEpicIId())));
            }

            updateRequest.insert(0, "{");
            updateRequest.append("}");
            byte[] input = updateRequest.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return connection;
    }

    private String getAttributeQuery(String key, String value) {
        return ("\"" + key + "\" : " + value);
    }


    private String getIntegerArrayAttributeQuery(String key, List<Integer> values) {
        StringBuilder query = new StringBuilder("\"" + key + "\" : [");
        for (int i = 0; i < values.size(); i++) {
            query.append(values.get(i));
            if (i != values.size() - 1) {
                query.append(",");
            }
        }
        query.append("]");
        return query.toString();
    }

    private String getStringArrayAttributeQuery(String key, List<String> values) {
        StringBuilder query = new StringBuilder("\"" + key + "\" : [");
        for (int i = 0; i < values.size(); i++) {
            query.append("\"");
            query.append(values.get(i));
            query.append("\"");
            if (i != values.size() - 1) {
                query.append(",");
            }
        }
        query.append("]");
        return query.toString();
    }

    public static boolean isEmpty(Collection coll) {
        return coll == null || coll.isEmpty();
    }

    public StringBuilder addComma(StringBuilder updateRequest) {
        if (updateRequest.length() != 0) {
            updateRequest.append(",\n");
        }
        return updateRequest;
    }

}
