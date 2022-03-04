package com.ilyachr.issuefetcher.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gitlab4j.api.models.Note;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Issue {

    private Integer count;
    private Integer completedCount;
    private Long id;
    private Long iid;
    private Long project_id;
    private String title;
    private String description;
    private String state;
    private String created_at;
    private String updated_at;
    private String closed_at;
    private User closed_by;
    private String[] labels;
    private Milestone milestone;
    private Assignee[] assignees;
    private Author author;
    private Assignee assignee;
    private Boolean confidential;
    private String dueDate;
    private Boolean subscribed;
    private Integer userNotesCount;
    private String webUrl;
    private Integer weight;
    private Boolean discussionLocked;
    private Integer upvotes;
    private Integer downvotes;
    private Integer mergeRequestsCount;
    private Boolean hasTasks;
    private String taskStatus;
    private TaskCompletionStatus taskCompletionStatus;
    private Integer epic_iid;

    private List<String> docsPath;

    private List<Note> noteList;

    public Issue() {
        docsPath = new ArrayList<>();
        noteList = new ArrayList<>();
    }

    @JsonIgnore
    public List<String> getDocsPath() {
        return docsPath;
    }

    @JsonIgnore
    public void addDocPath(String docPath) {
        docsPath.add(docPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Issue issue = (Issue) o;
        return Objects.equals(iid, issue.iid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iid);
    }
}
