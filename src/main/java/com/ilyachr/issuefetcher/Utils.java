package com.ilyachr.issuefetcher;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.*;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

@Slf4j
public class Utils {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36";

    public static final String PROJECT_PATH_TEMPLATE = "uploads/{0}";

    public static final String ISSUE_FOLDER_PATH_TEMPLATE = "uploads/{0}/{1}";

    public static final String ISSUE_PATH_TEMPLATE = "uploads/{0}/{1}/{2}";

    public static final String EPICS_PATH_TEMPLATE = "uploads/epics/{0}";

    public static final String PROPERTIES_FILE_NAME = "config.properties";

    public static final String EXPORT_MODE = "export";

    public static final String IMPORT_MODE = "import";

    private final Map<GitLabEnum, String> gitLabProperties = new EnumMap<>(GitLabEnum.class);

    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
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
                    log.error(
                            "Exception occured : {} ", exCast.getMessage());
                } catch (ClassCastException ccEx) {
                    log.error("Unexpected runtime exception : {}", ex.getMessage());
                }
            }
        };
    }

    public enum GitLabEnum {
        SSL_DISABLE,
        EPICS_ENABLE,
        ISSUE_ENABLE,
        PARALLEL_MODE,

        GITLAB_FROM_TOKEN,
        GITLAB_FROM_PATH,
        GITLAB_FROM_PROJECT_ID,
        GITLAB_FROM_PROJECT_PATH,
        GITLAB_FROM_GROUP_ID,
        GITLAB_PROJECT_NAME,

        GITLAB_TO_TOKEN,
        GITLAB_TO_PATH,
        GITLAB_TO_PROJECT_ID,
        GITLAB_TO_PROJECT_PATH,
        GITLAB_TO_GROUP_ID,

        GITLAB_FROM_LOGIN_FORM_URL,
        GITLAB_FROM_LOGIN_ACTION_URL,
        GITLAB_FROM_USERNAME,
        GITLAB_FROM_PASSWORD
    }


    public Utils() throws FileNotFoundException, URISyntaxException {
        readProperties();
    }

    private void readProperties() throws FileNotFoundException, URISyntaxException {
        Properties properties = new Properties();

        File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        File file = new File(jarFile.getParent() + "/\\\\" + PROPERTIES_FILE_NAME);

        try (InputStream inputStream = (file.exists()) ? new FileInputStream(file) : getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME)) {

            if (inputStream != null) {
                properties.load(inputStream);

                gitLabProperties.put(GitLabEnum.SSL_DISABLE,  properties.getProperty(GitLabEnum.SSL_DISABLE.name()).trim());
                gitLabProperties.put(GitLabEnum.EPICS_ENABLE, properties.getProperty(GitLabEnum.EPICS_ENABLE.name()).trim());
                gitLabProperties.put(GitLabEnum.ISSUE_ENABLE, properties.getProperty(GitLabEnum.ISSUE_ENABLE.name()).trim());
                gitLabProperties.put(GitLabEnum.PARALLEL_MODE, properties.getProperty(GitLabEnum.PARALLEL_MODE.name()).trim());


                gitLabProperties.put(GitLabEnum.GITLAB_FROM_TOKEN, properties.getProperty(GitLabEnum.GITLAB_FROM_TOKEN.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PATH, properties.getProperty(GitLabEnum.GITLAB_FROM_PATH.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PROJECT_ID, properties.getProperty(GitLabEnum.GITLAB_FROM_PROJECT_ID.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PROJECT_PATH, properties.getProperty(GitLabEnum.GITLAB_FROM_PROJECT_PATH.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_GROUP_ID, properties.getProperty(GitLabEnum.GITLAB_FROM_GROUP_ID.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_PROJECT_NAME, properties.getProperty(GitLabEnum.GITLAB_PROJECT_NAME.name()).trim());


                gitLabProperties.put(GitLabEnum.GITLAB_TO_TOKEN, properties.getProperty(GitLabEnum.GITLAB_TO_TOKEN.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_TO_PATH, properties.getProperty(GitLabEnum.GITLAB_TO_PATH.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_TO_PROJECT_ID, properties.getProperty(GitLabEnum.GITLAB_TO_PROJECT_ID.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_TO_PROJECT_PATH, properties.getProperty(GitLabEnum.GITLAB_TO_PROJECT_PATH.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_TO_GROUP_ID, properties.getProperty(GitLabEnum.GITLAB_TO_GROUP_ID.name()).trim());

                gitLabProperties.put(GitLabEnum.GITLAB_FROM_LOGIN_FORM_URL, properties.getProperty(GitLabEnum.GITLAB_FROM_LOGIN_FORM_URL.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_LOGIN_ACTION_URL, properties.getProperty(GitLabEnum.GITLAB_FROM_LOGIN_ACTION_URL.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_USERNAME, properties.getProperty(GitLabEnum.GITLAB_FROM_USERNAME.name()).trim());
                gitLabProperties.put(GitLabEnum.GITLAB_FROM_PASSWORD, properties.getProperty(GitLabEnum.GITLAB_FROM_PASSWORD.name()).trim());
            }

        } catch (IOException exception) {
            throw new FileNotFoundException("Can't read " + PROPERTIES_FILE_NAME + " file");
        }
    }

    public String getProperty(GitLabEnum gitLabEnum) {
        return gitLabProperties.get(gitLabEnum);
    }

    public void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e.getMessage());
        }
    }

}
