package com.ilyachr.issuefetcher.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Note {
    private Integer id;
    private String body;
    private String attachment;
    private Author author;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("noteable_id")
    private Integer noteableId;

    @JsonProperty("noteable_iid")
    private Integer noteableIid;

    private Boolean resolvable;

    private Boolean downvote;

    @JsonProperty("expires_at")
    private String expiresAt;

    private String fileName;
    private String noteableType;
    private Boolean system;
    private String title;
    private Boolean upvote;
    private Boolean resolved;
    private Participant resolvedBy;

    @JsonProperty("resolved_at")
    private String resolvedAt;
}
