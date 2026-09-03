package com.nadoumi.identity.contact;

import java.time.LocalDateTime;

/**
 * Row of {@code nad_contact_inquiry} — a message left on the public website's
 * Contact page. Lives in {@code nadoumi-identity} for now because that module
 * already owns the anonymous {@code /api/public} + mail infrastructure; it moves
 * to a {@code nadoumi-content} module when that context is built.
 */
public class ContactInquiry {

    private Long id;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private String phone;
    private String category;
    private String subject;
    private String message;
    private ContactInquiryStatus status = ContactInquiryStatus.NEW;
    private String source = "WEBSITE";
    private String locale;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private Long handledBy;
    private LocalDateTime handledAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ContactInquiryStatus getStatus() {
        return status;
    }

    public void setStatus(ContactInquiryStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }
}
