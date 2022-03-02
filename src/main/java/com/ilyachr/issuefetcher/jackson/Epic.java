package com.ilyachr.issuefetcher.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Epic {
    private Integer id;
    private Integer iid;
    private Integer groupId;
    private String title;
    private String description;
    private org.gitlab4j.api.models.Author author;
    private List<String> labels;
    private Date startDate;
    private Date endDate;
    private Date createdAt;
    private Date updatedAt;
}