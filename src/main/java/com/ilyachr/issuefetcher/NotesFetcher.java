package com.ilyachr.issuefetcher;

import com.ilyachr.issuefetcher.jackson.Issue;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.NotesApi;
import org.gitlab4j.api.models.Note;

import java.util.Collections;
import java.util.List;

@Slf4j
public enum NotesFetcher {
    NOTES_FETCHER;

    public void setIssueNotes(List<Issue> issueList, String projectPath, String projectId, String token) {
        try (GitLabApi gitLabApi = new GitLabApi(projectPath, token)) {
            NotesApi notesApi = gitLabApi.getNotesApi();
            issueList.parallelStream().forEach(Utils.throwingConsumerWrapper(i -> {
                List<Note> noteList = notesApi.getIssueNotes(projectId, i.getIid().intValue());
                Collections.reverse(noteList);
                i.setNoteList(noteList);
                log.info("fetching notes for issue: {}", i.getIid());
            }, GitLabApiException.class));
        }
    }

    public void createIssueNotes(Issue issue, String projectPath, String projectId, String token) {
        try (GitLabApi gitLabApi = new GitLabApi(projectPath, token)) {
            NotesApi notesApi = gitLabApi.getNotesApi();
            issue.getNoteList().forEach(Utils.throwingConsumerWrapper(note -> {
                notesApi.createIssueNote(projectId, issue.getIid().intValue(), note.getBody());
                log.info("notes of issue : {} successfully created", issue.getIid());
            }, GitLabApiException.class));
        }
    }

    //Сначала удаляем старые комментарии потом создаем актуальные
    public void updateIssueNotes(Issue issue, Issue oldIssue, String projectPath, String projectId, String token) {
        try (GitLabApi gitLabApi = new GitLabApi(projectPath, token)) {
            NotesApi notesApi = gitLabApi.getNotesApi();
            oldIssue.getNoteList().forEach(Utils.throwingConsumerWrapper(note ->
                    notesApi.deleteIssueNote(projectId, issue.getIid().intValue(), note.getId()), GitLabApiException.class));
            createIssueNotes(issue, projectPath, projectId, token);
        }
    }
}