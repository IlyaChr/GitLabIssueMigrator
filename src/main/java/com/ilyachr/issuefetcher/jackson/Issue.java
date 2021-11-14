package com.ilyachr.issuefetcher.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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

    private List<String> docsPath;

    @JsonIgnore
    public List<String> getDocsPath() {
        if (docsPath == null) {
            docsPath = new ArrayList<>();
        }
        return docsPath;
    }

    @JsonIgnore
    public void addDocPath(String docPath) {
        if (docsPath == null) {
            docsPath = new ArrayList<>();
        }
        docsPath.add(docPath);
    }

}
