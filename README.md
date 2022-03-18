Перенос gitlab issues из разных проектов с их сериализацией и сохранением вложенных документов

For start:
- mvn clean compile assembly:single resources:resources
- Choose config and fill GITLAB_FROM_USERNAME, GITLAB_FROM_PASSWORD
- Rename config file to config.properties
- Put config.properties along with GitLabIssueMigrator.jar (config.properties in the GitLabIssueMigrator.jar directory)
- java -jar GitLabIssueMigrator.jar