package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.User;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class UsersFetcher extends Fetching<User> {

    public UsersFetcher() {
        super(User.class);
    }

    public Map<String, Integer> getAllUsers(String projectPath, String projectId, String token) throws IOException {

        Map<String, Integer> usersIds;
        usersIds = fetchAll(projectPath, projectId, token)
                .stream().collect(Collectors.toMap(User::getName, User::getId));

        return usersIds;
    }


    @Override
    public URL getMainUrl(String projectPath, String projectId) throws MalformedURLException {
        return new URL(projectPath + "/api/v4/projects/" + projectId + "/users");
    }

}
