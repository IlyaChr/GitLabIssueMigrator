package com.ilyachr.issuefetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;


public class Utils {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36";
    public static final String PROPERTIES_FILE_NAME = "config.properties";

    private final Map<GitLabEnum, String> gitLabProperties = new HashMap<>();

    @FunctionalInterface
    public static interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    public static <T, E extends Exception> Consumer<T> throwingConsumerWrapper(
            ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionClass) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                try {
                    E exCast = exceptionClass.cast(ex);
                    System.err.println(
                            "Exception occured : " + exCast.getMessage());
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public enum GitLabEnum {
        GITLAB_FROM_TOKEN,
        GITLAB_FROM_PATH,
        GITLAB_FROM_PROJECT_ID,
        GITLAB_FROM_PROJECT_PATH,

        GITLAB_TO_TOKEN,
        GITLAB_TO_PATH,
        GITLAB_TO_PROJECT_ID,
        GITLAB_TO_PROJECT_PATH,

        GITLAB_FROM_LOGIN_FORM_URL,
        GITLAB_FROM_LOGIN_ACTION_URL,
        GITLAB_FROM_USERNAME,
        GITLAB_FROM_PASSWORD
    }


    public Utils() throws FileNotFoundException {
        readProperties();
    }

    private void readProperties() throws FileNotFoundException {

        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME)) {

            if (inputStream != null) {
                properties.load(inputStream);

                gitLabProperties.put(GitLabEnum.GITLAB_FROM_TOKEN, properties.getProperty(GitLabEnum.GITLAB_FROM_TOKEN.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PATH, properties.getProperty(GitLabEnum.GITLAB_FROM_PATH.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PROJECT_ID, properties.getProperty(GitLabEnum.GITLAB_FROM_PROJECT_ID.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PROJECT_PATH, properties.getProperty(GitLabEnum.GITLAB_FROM_PROJECT_PATH.name()));

                gitLabProperties.put(GitLabEnum.GITLAB_TO_TOKEN, properties.getProperty(GitLabEnum.GITLAB_TO_TOKEN.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_TO_PATH, properties.getProperty(GitLabEnum.GITLAB_TO_PATH.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_TO_PROJECT_ID, properties.getProperty(GitLabEnum.GITLAB_TO_PROJECT_ID.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_TO_PROJECT_PATH, properties.getProperty(GitLabEnum.GITLAB_TO_PROJECT_PATH.name()));

                gitLabProperties.put(GitLabEnum.GITLAB_FROM_LOGIN_FORM_URL, properties.getProperty(GitLabEnum.GITLAB_FROM_LOGIN_FORM_URL.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_LOGIN_ACTION_URL, properties.getProperty(GitLabEnum.GITLAB_FROM_LOGIN_ACTION_URL.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_USERNAME, properties.getProperty(GitLabEnum.GITLAB_FROM_USERNAME.name()));
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PASSWORD, properties.getProperty(GitLabEnum.GITLAB_FROM_PASSWORD.name()));
            }

        } catch (IOException exception) {
            throw new FileNotFoundException("Can't read " + PROPERTIES_FILE_NAME + " file");
        }
    }

    public String getProperty(GitLabEnum gitLabEnum) {
        return gitLabProperties.get(gitLabEnum);
    }
}
