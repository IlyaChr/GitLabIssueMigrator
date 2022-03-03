package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Epic;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class EpicsFetcher extends Fetching<Epic> {

    public EpicsFetcher() {
        super(Epic.class);
    }

    public EpicsFetcher(Class<Epic> type) {
        super(type);
    }

    public List<Epic> fetchAllEpics(String projectPath, String projectId, String token) throws IOException {
        return fetchAll(projectPath, projectId, token);
    }

    @Override
    public URL getMainUrl(String projectPath, String groupsId) throws MalformedURLException {
        return new URL(projectPath + "/api/v4/groups/" + groupsId + "/epics?state=all");
    }
}
