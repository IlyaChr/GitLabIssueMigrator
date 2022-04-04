package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;
import com.ilyachr.issuefetcher.jackson.Note;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class NotesFactory extends RestApi<Note> {

    @Getter
    private static final NotesFactory instance = new NotesFactory();

    private NotesFactory() {
        super(Note.class);
    }

    @Override
    public URL getUrl(RestQueryParam queryParam) throws MalformedURLException {
        return queryParam.getNotesIid() == null ?
                new URL(queryParam.getProjectPath() + "/api/v4/projects/" + queryParam.getProjectId() + "/issues/" + queryParam.getIssueIid() + "/notes") :
                new URL(queryParam.getProjectPath() + "/api/v4/projects/" + queryParam.getProjectId() + "/issues/" + queryParam.getIssueIid() + "/notes/" + queryParam.getNotesIid());
    }


    public void setIssueNotes(List<Issue> issueList, String projectPath, String projectId, String token, boolean isParallel) {
        Stream<Issue> issueListStream = isParallel ? issueList.parallelStream() : issueList.stream();
        issueListStream.forEach(Utils.throwingConsumerWrapper(i -> {
            List<Note> noteList = createGetRequest(RestQueryParam.builder().
                    projectPath(projectPath).
                    projectId(projectId).
                    issueIid(i.getIid().toString()).
                    token(token).
                    build());
            Collections.reverse(noteList);
            i.setNoteList(noteList);
        }, IOException.class));
    }

    public void createIssueNotes(Issue issue, String projectPath, String projectId, String token) {
        issue.getNoteList().forEach(note -> createPostRequest(note, RestQueryParam.builder().
                projectPath(projectPath).
                projectId(projectId).
                issueIid(issue.getIid().toString()).
                token(token).
                build(), n -> {
        }));
    }

    //Сначала удаляем старые комментарии потом создаем актуальные
    public void updateIssueNotes(Issue issue, Issue oldIssue, String projectPath, String projectId, String token) {
        oldIssue.getNoteList().forEach(note ->
                createDeleteRequest(RestQueryParam.builder().
                        projectPath(projectPath).
                        projectId(projectId).
                        issueIid(issue.getIid().toString()).
                        notesIid(note.getId().toString()).
                        token(token).
                        build()));
        createIssueNotes(issue, projectPath, projectId, token);
    }
}