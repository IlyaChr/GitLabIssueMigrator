package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.User;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class UsersFetcher extends RestApi<User> {

    @Getter
    private static final UsersFetcher instance = new UsersFetcher();

    private UsersFetcher() {
        super(User.class);
    }

    public Map<String, Integer> getAllUsers(String projectPath, String projectId, String token) throws IOException {

        Map<String, Integer> usersIds;
        usersIds = createGetRequest(RestQueryParam.builder().
                projectPath(projectPath).
                projectId(projectId).
                token(token)
                .build())
                .stream().collect(Collectors.toMap(User::getName, User::getId));

        return usersIds;
    }


    @Override
    public URL getUrl(RestQueryParam queryParam) throws MalformedURLException {
        return new URL(queryParam.getProjectPath() + "/api/v4/projects/" + queryParam.getProjectId() + "/users");
    }

}
