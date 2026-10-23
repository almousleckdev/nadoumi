package com.nadoumi.university.domain.enums;

/** {@code nad_university_link.kind}. WECHAT carries an account id; every other kind a web URL. */
public enum LinkKind {
    ADMISSIONS, VIRTUAL_TOUR, WIKIPEDIA, FACEBOOK, X, INSTAGRAM, YOUTUBE, LINKEDIN, TIKTOK, WECHAT, WEIBO;

    public boolean isWebLink() {
        return this != WECHAT;
    }
}
