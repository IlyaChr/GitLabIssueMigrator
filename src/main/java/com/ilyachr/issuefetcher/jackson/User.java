package com.ilyachr.issuefetcher.jackson;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends AbstractUser<User> {
    private String bio;
    private Boolean bot;
    private Boolean canCreateGroup;
    private Boolean canCreateProject;
    private Integer colorSchemeId;
    private Date confirmedAt;
    private Date currentSignInAt;
    private List<CustomAttribute> customAttributes;
    private Boolean external;
    private String externUid;
    private Integer extraSharedRunnersMinutesLimit;
    private List<Identity> identities;
    private Boolean isAdmin;
    private Date lastActivityOn;
    private Date lastSignInAt;
    private String linkedin;
    private String location;
    private String organization;
    private Boolean privateProfile;
    private Integer projectsLimit;
    private String provider;
    private String publicEmail;
    private Integer sharedRunnersMinutesLimit;
    private String skype;
    private String state;
    private Integer themeId;
    private String twitter;
    private Boolean twoFactorEnabled;
    private String websiteUrl;
    private Boolean skipConfirmation;
}
