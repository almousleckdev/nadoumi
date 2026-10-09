package com.nadoumi.identity.profile;

/** The raw columns {@link PublicProfileRules} needs, as read from {@code sys_user} and the owner applicant. */
public class PublicProfileRow {
    private Long userId;
    private String userType;
    private String nickName;
    private String userName;
    private String avatar;
    private String ownerGivenName;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getOwnerGivenName() { return ownerGivenName; }
    public void setOwnerGivenName(String ownerGivenName) { this.ownerGivenName = ownerGivenName; }
}
