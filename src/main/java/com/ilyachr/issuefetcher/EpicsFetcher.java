package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.EpicsApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Epic;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

@Slf4j
public enum EpicsFetcher {
    EPICS_FETCHER;

    public List<Epic> fetchEpics(String projectPath, String groupId, String token) throws GitLabApiException {
        List<Epic> epicList;
        try (GitLabApi gitLabApi = new GitLabApi(projectPath, token)) {
            EpicsApi epicsApi = gitLabApi.getEpicsApi();
            epicList = epicsApi.getEpics(groupId);
        }
        return epicList;
    }

    public void createEpics(List<Epic> epicList, String projectPath, String groupId, String token) {
        try (GitLabApi gitLabApi = new GitLabApi(projectPath, token)) {
            EpicsApi epicsApi = gitLabApi.getEpicsApi();
            epicList.parallelStream().forEach(Utils.throwingConsumerWrapper(epic -> {
                epicsApi.createEpic(groupId, epic);
                log.info("epic : {} successfully created", epic.getIid());
            }, GitLabApiException.class));
        }
    }


    /**
     * Load epics from file system
     */
    public List<Epic> loadEpicsDataFromDisk(String groupId) {
        List<Epic> epicList = Collections.synchronizedList(new ArrayList<>());
        File groupRoot = new File(MessageFormat.format(Utils.PROJECT_PATH_TEMPLATE, groupId));
        List<String> epicDirectories = groupRoot.list() == null ? Collections.emptyList() : Arrays.asList(Objects.requireNonNull(groupRoot.list()));

        epicDirectories.stream().parallel().forEach(Utils.throwingConsumerWrapper(path -> {
            File file = new File(MessageFormat.format(Utils.EPICS_PATH_TEMPLATE, groupId, path));
            ObjectMapper objectMapper = new ObjectMapper();
            if (file.exists() && file.getName().toLowerCase().contains("epic")){
                Epic epic = objectMapper.readValue(file, Epic.class);
                epicList.add(epic);
            }
        }, IOException.class));

        return epicList;
    }


    /**
     * Saving epics to file
     */
    public void saveEpicToFile(List<Epic> epicList, String groupId) {
        epicList.stream().parallel().forEach(Utils.throwingConsumerWrapper(epic -> {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(MessageFormat.format(Utils.EPICS_PATH_TEMPLATE, groupId, String.valueOf(epic.getIid())) + ".epic");
            if (file.getParentFile().mkdirs()) {
                log.debug("Directory {} was created", file.getPath());
            }
            if (file.createNewFile()) {
                log.debug("File {} was successfully created ", file.getName());
            }
            objectMapper.writeValue(file, epic);
        }, IOException.class));
    }

}
