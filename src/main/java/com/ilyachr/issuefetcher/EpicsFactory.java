package com.ilyachr.issuefetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilyachr.issuefetcher.jackson.Epic;
import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class EpicsFactory extends RestApi<Epic> {

    @Getter
    private static final EpicsFactory instance = new EpicsFactory();

    private EpicsFactory() {
        super(Epic.class);
    }

    @Override
    public URL getUrl(RestQueryParam queryParam) throws MalformedURLException {
        return queryParam.getEpicIid() == null ?
                new URL(queryParam.getProjectPath() + "/api/v4/groups/" + queryParam.getGroupId() + "/epics") :
                new URL(queryParam.getProjectPath() + "/api/v4/groups/" + queryParam.getGroupId() + "/epics/" + queryParam.getEpicIid());
    }

    public List<Epic> fetchEpics(String projectPath, String groupId, String token) throws IOException {
        return createGetRequest(RestQueryParam.builder().
                projectPath(projectPath).
                groupId(groupId).
                token(token)
                .build());
    }

    public void createEpics(List<Epic> fromEpicList, List<Epic> toEpicList, String projectPath, String groupId, String token) {
        Map<Integer, Epic> toEpicMap = toEpicList.stream().collect(Collectors.toMap(Epic::getIid, epic -> epic));

        fromEpicList.forEach(Utils.throwingConsumerWrapper(epic -> {
            //создаем epic если такого epic не было
            if (!toEpicMap.containsKey(epic.getIid())) {
                createNewEpic(epic, projectPath, groupId, token);

                RestQueryParam queryParam = RestQueryParam.builder().
                        projectPath(projectPath).
                        groupId(groupId).
                        epicIid(epic.getIid().toString()).
                        token(token)
                        .build();

                FieldUpdater.FIELD_UPDATER.update(getUrl(queryParam), queryParam, FieldUpdater.ParamForUpdate.builder().
                        epicIId(epic.getIid().toString()).
                        updatedAt(epic.getUpdatedAt()).
                        build());
            } else {
                // иначе обновляем существующую
                updateEpic(epic, toEpicMap.get(epic.getIid()), projectPath, groupId, token);
            }

        }, IOException.class));
    }

    private void createNewEpic(Epic epic, String projectPath, String groupId, String token) {
        createPostRequest(epic, RestQueryParam.builder().
                projectPath(projectPath).
                groupId(groupId).
                token(token)
                .build(), e -> {
        });
    }

    private void updateEpic(Epic epic, Epic oldEpic, String projectPath, String groupId, String token) {
        if (LocalDateTime.parse(epic.getUpdatedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .isAfter(LocalDateTime.parse(oldEpic.getUpdatedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))) {
            createPutRequest(epic, RestQueryParam.builder().
                    projectPath(projectPath).
                    groupId(groupId).
                    epicIid(epic.getIid().toString()).
                    token(token)
                    .build());
        }
    }


    public void deleteEpics(List<Epic> epicList, String projectPath, String groupId, String token, boolean isParallel) {
        Stream<Epic> epicListStream = isParallel ? epicList.parallelStream() : epicList.stream();
        epicListStream.forEach(epic -> createDeleteRequest(
                RestQueryParam.builder().
                        projectPath(projectPath).
                        groupId(groupId).
                        epicIid(epic.getIid().toString()).
                        token(token)
                        .build()));
    }

    /**
     * Load epics from file system
     */
    public List<Epic> loadEpicsDataFromDisk() {
        List<Epic> epicList = Collections.synchronizedList(new ArrayList<>());
        File groupRoot = new File(MessageFormat.format(Utils.PROJECT_PATH_TEMPLATE, "epics"));
        List<String> epicDirectories = groupRoot.list() == null ? Collections.emptyList() : Arrays.asList(Objects.requireNonNull(groupRoot.list()));

        epicDirectories.stream().parallel().forEach(Utils.throwingConsumerWrapper(path -> {
            File file = new File(MessageFormat.format(Utils.EPICS_PATH_TEMPLATE, path));
            ObjectMapper objectMapper = new ObjectMapper();
            if (file.exists() && file.getName().toLowerCase().endsWith("epic")) {
                Epic epic = objectMapper.readValue(file, Epic.class);
                epicList.add(epic);
            }
        }, IOException.class));

        return epicList;
    }


    /**
     * Saving epics to file
     */
    public void saveEpicToFile(List<Epic> epicList) {
        epicList.stream().parallel().forEach(Utils.throwingConsumerWrapper(epic -> {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(MessageFormat.format(Utils.EPICS_PATH_TEMPLATE, String.valueOf(epic.getIid())) + ".epic");
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
