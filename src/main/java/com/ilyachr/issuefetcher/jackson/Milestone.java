package com.ilyachr.issuefetcher.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Milestone {
    private Date createdAt;
    private String description;
    private Date startDate;
    private Date dueDate;
    private Integer id;
    private Integer iid;
    private Integer projectId;
    private Integer groupId;
    private String state;
    private String title;
    private Date updatedAt;
}
