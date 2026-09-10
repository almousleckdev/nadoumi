# Nadoumi — Transactional Email Standardization — Engineering Specification

Status: **DRAFT — for review**. Author: Claude Sonnet 5. Date: 2026-09-10.
Session: https://claude.ai/code/session_01Y7D71KBp6FSgBrHAJP9ZFg

> One consistent, non-boxed, responsive, accessible Nadoumi email design applied
> across every transactional email the platform sends, backed by a single shared
> layout component so no template duplicates HTML/CSS. Adds a post-registration
> **Welcome to Nadoumi** email with dynamically-sourced programme and scholarship
> sections, a **password-changed** security email, and ships the notification
> types + templates for **application-submitted / application-status** emails so
> the Application module (platform Step 6/7) only has to emit an event when it
> lands.

---

## 0. Scope

**In scope**

1. Shared email foundation in `com.nadoumi.identity.service.mail`:
   - `EmailMessage` gains a nullable `htmlBody`; `SmtpMailSender` /
     `LoggingMailSender` learn multipart/alternative.
   - `EmailLayout` component + `EmailContent` model — the single source of the
     Nadoumi email chrome (header wordmark, body, CTA, optional list sections,
     consistent footer with contact block + social icons + logo). Produces HTML
     **and** a parity plain-text alternative from the same model.
   - `BrandProperties` (`@ConfigurationProperties("nadoumi.brand")`) — contact
     details, social URLs, base URL, logo path.
   - Static email assets (`/email/nadoumi-logo.png` + three social icons) served
     anonymously; `SecurityConfig` `permitAll` for `/email/**`.
2. Existing identity emails re-rendered through `EmailLayout`: `otp-register`,
   `otp-password-reset`, `account-exists`. New `password-changed` email +
   after-commit trigger from `StudentAuthService.resetPassword` /
   `changePassword`.
3. Existing notification emails re-rendered through `EmailLayout`:
   `SCHOLARSHIP_PUBLISHED`, `SCHOLARSHIP_DEADLINE_REMINDER`,
   `UNIVERSITY_PUBLISHED`, `PROGRAM_PUBLISHED`, `CONTACT_INQUIRY_RECEIVED`,
   `TASK_PROGRESS`. Chrome + subject-line polish only; template copy rows
   unchanged except optional wording tidy-ups.
4. **Welcome email** — `StudentRegistered` outbox event from
   `StudentAuthService.register()`; `NotificationType.WELCOME`; a
   `WelcomeContentComposer` in `nadoumi-notification` that pulls current
   published programmes and scholarships from the existing catalog services and
   omits any empty section; IN_APP + EMAIL.
5. **Application email readiness** — `OutboxEventTypes` +
   `NotificationType` + `OutboxToNotificationDispatcher` mappings +
   seeded `en` templates for `APPLICATION_SUBMITTED` and
   `APPLICATION_STATUS_CHANGED`. No producer is wired (the Application module
   does not exist yet); nothing sends until it emits the event.
6. Migration `V55__nad_notification_welcome_and_application_templates.sql`.
7. Config: `nadoumi.web.baseUrl`, `nadoumi.brand.*` in `application.yml` with
   env overrides and local-dev defaults.
8. Documentation updates: `COMMUNICATION_AND_NOTIFICATIONS.md`,
   `DOMAIN_EVENTS.md`, `DEVELOPMENT_GUIDELINES.md`,
   `PLATFORM_ARCHITECTURE.md §8`.

**Explicitly out of scope (deferred, tracked)**

- The Application module itself (Step 6/7) and any real producer of
  `ApplicationSubmitted` / `ApplicationStatusChanged`. This spec ships only the
  notification plumbing and templates so that module can call
  `NotificationService.create(...)` with no further email work.
- Per-locale email templates. Everything ships `en`; the `{{var}}` / `${var}`
  renderers and `EmailContent.locale` already leave room for `zh` later.
- SMS / WhatsApp / push channels (schema-only elsewhere; untouched here).
- A shared contact-details API consumed by `nadoumi-web`. Backend
  `nadoumi.brand.contact.*` and `nadoumi-web/app/data/contact.ts` stay as two
  hand-synced sources; the sync obligation is documented.
- Cross-email-client visual QA (Litmus / Email on Acid). Automated tests assert
  structure and accessibility attributes; pixel rendering across clients is a
  manual follow-up.
- Replacing the placeholder logo / social icon PNGs with final brand artwork,
  and setting real social URLs. The mechanism ships; the values are
  environment/asset changes.

---

## 1. Current state (EXISTING)

Two independent email paths, both **plain-text only**, both terminating at the
`MailSender` port (`smtp` adapter → `SimpleMailMessage`; `log` adapter → JSON
outbox file + last-per-recipient cache).

### 1.1 Identity module — `nadoumi-identity`

- `resources/mail/*.txt` templates with `${var}` placeholders, rendered by
  `MailTemplates.render(name, vars)` (unbound placeholder = hard error).
- `OtpService.issue(...)` sends `otp-register` / `otp-password-reset` /
  `account-exists` via `mail.send(new EmailMessage(to, subject, body))`.
- `contact-inquiry.txt` still on disk but appears **dead** — the comms doc
  records that `ContactService`'s direct support-inbox mail was removed and
  replaced by the `CONTACT_INQUIRY_RECEIVED` notification. To be confirmed and
  deleted (§4.2).
- `MailDiagnostics` sends an ad-hoc "Nadoumi mail test" — left as plain text.

### 1.2 Notification module — `nadoumi-notification`

- DB templates `nad_notification_template (type, channel, locale, subject_tpl,
  body_tpl)` with `{{var}}` placeholders, rendered by `NotificationRenderer`
  (`locale='en'` fallback; unbound placeholder / missing template = hard error).
- `NotificationDeliveryDispatcher` drains `PENDING` `nad_notification_delivery`
  rows, renders `(type, channel='EMAIL', 'en', context)`, calls
  `EmailNotificationChannel.send(recipient, subject, body)` →
  `mail.send(new EmailMessage(recipient, subject, body))`.
- Producers: `ContactService` → `ContactInquiryReceived`;
  `ScholarshipAdminService` → `ScholarshipPublished`; `UniversityService` /
  `ProgramService` → `UniversityPublished` / `ProgramPublished`;
  `ScholarshipDeadlineReminderJob` → `ScholarshipDeadlineReminder`; task events
  → `TaskProgressChanged`. All flow through the transactional outbox
  (`nad_outbox_event`, `OutboxWriter` SPI in `nadoumi-common`) →
  `OutboxPollerJob` → `OutboxToNotificationDispatcher`.

### 1.3 Module dependency direction (a hard constraint)

```
nadoumi-common   ← nadoumi-identity   ← nadoumi-university ← nadoumi-scholarship
                                      ← nadoumi-applicant                ↑
                                      ← nadoumi-program  ────────────────┘
nadoumi-identity ← nadoumi-notification   (build order: notification is last)
```

- `nadoumi-notification` already depends on `nadoumi-identity`, so the shared
  email shell lives in `com.nadoumi.identity.service.mail` and both modules use
  it with no new edge and no cycle.
- `nadoumi-identity` **must not** depend on `nadoumi-scholarship` /
  `nadoumi-program`, so the Welcome email's catalog sections cannot be composed
  in identity. They are composed in `nadoumi-notification`, which may add
  read-only dependencies on those two modules (both precede it in build order).

### 1.4 Config / assets today

- `application.yml`: `spring.mail.*`, `nadoumi.mail.{transport,from,logFile,
  supportInbox}`, `nadoumi.mail.dev-inbox.enabled`, `nadoumi.web.loginUrl`. No
  base URL for building links, no brand block.
- No Thymeleaf or any HTML-templating dependency on the classpath.
- Only brand asset in the repo is `nadoumi-web/app/assets/images/logo.jpg` (a
  photographic JPG, not an email logo). Contact details live only in
  `nadoumi-web/app/data/contact.ts`. No social media URLs anywhere.

---

## 2. Design — shared email foundation (PROPOSED)

All new types in `com.nadoumi.identity.service.mail` unless noted.

### 2.1 `EmailMessage`

```java
/** A rendered, ready-to-send email. htmlBody is null for text-only messages. */
public record EmailMessage(String to, String subject, String body, String htmlBody) {

    /** Text-only message (diagnostics, back-compat call sites). */
    public static EmailMessage text(String to, String subject, String body) {
        return new EmailMessage(to, subject, body, null);
    }
}
```

`body` is always the plain-text alternative and is never null. Every existing
`new EmailMessage(to, subject, body)` call site becomes `EmailMessage.text(...)`
or is replaced by an `EmailLayout` render (§3, §4).

### 2.2 `SmtpMailSender`

```java
@Override
public void send(EmailMessage message) {
    if (message.htmlBody() == null) {
        sendPlainText(message);          // existing SimpleMailMessage path, unchanged
        return;
    }
    MimeMessage mime = mail.createMimeMessage();
    try {
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(message.to());
        helper.setSubject(message.subject());
        helper.setText(message.body(), message.htmlBody());   // plain + HTML → multipart/alternative
        mail.send(mime);
    }
    catch (MessagingException | MailException e) {
        log.error("SMTP send failed to={} subject=\"{}\"", message.to(), message.subject(), e);
        throw e instanceof MailException me ? me : new MailSendException("MIME assembly failed", e);
    }
}
```

- Body is still omitted from the log line (it can carry an OTP).
- `NadApiExceptionHandler`'s existing `MailException → 502` mapping is
  preserved; `MessagingException` is wrapped in `MailSendException` so the
  mapping still applies.

### 2.3 `LoggingMailSender`

- `toJsonLine` gains an `"html"` field (`null` when absent), same escaping.
- `lastByRecipient` keeps the whole `EmailMessage`. `DevMailController`
  (`GET /api/dev/mail/latest`) keeps returning the **plain-text** `body` — the
  Playwright `readOtp` helper is unaffected.

### 2.4 `BrandProperties`

```java
@ConfigurationProperties("nadoumi.brand")
public record BrandProperties(
        String baseUrl,               // e.g. https://nadoumi.com  (no trailing slash)
        String logoPath,              // default /email/nadoumi-logo.png
        String wordmark,              // "Nadoumi"
        Contact contact,
        Social social,
        String preferencesPath) {     // default /account/notifications

    public record Contact(List<String> emails, List<String> phones,
                          String officeEn, String hours) {}

    /** Any field null/blank → that icon is omitted from the footer. */
    public record Social(String facebook, String instagram, String tiktok) {}

    public String logoUrl()        { return baseUrl + logoPath; }
    public String preferencesUrl() { return baseUrl + preferencesPath; }
    public String assetUrl(String p) { return baseUrl + p; }
}
```

Registered via `@EnableConfigurationProperties` in the identity module's
autoconfiguration (or `NotificationAutoConfiguration` if that is where mail beans
are already wired — to be confirmed during implementation).

### 2.5 `EmailContent` — the model every email is expressed in

```java
public record EmailContent(
        String preheader,             // hidden inbox-preview line
        String heading,               // the single <h1>
        List<Block> blocks,
        Cta cta,                      // nullable
        List<ListItem> items,         // nullable/empty — the Welcome sections
        String itemsHeading,          // heading shown above items when present
        Locale locale,                // defaults to en
        boolean showPreferencesLink   // true only for non-transactional types
) {
    public sealed interface Block {}
    public record Paragraph(String text) implements Block {}
    public record CodeBlock(String value) implements Block {}      // the OTP, large + spaced
    public record KeyValues(List<Entry> entries) implements Block {}
    public record Entry(String label, String value) {}
    public record Divider() implements Block {}

    public record Cta(String label, String url) {}
    public record ListItem(String title, String meta, String url) {}

    public static Builder builder(String heading) { ... }
}
```

A fluent `Builder` keeps call sites readable. `blocks` is required and non-empty;
everything else is optional.

### 2.6 `EmailLayout` — the only place email chrome exists

```java
@Component
public class EmailLayout {
    public EmailLayout(BrandProperties brand) { ... }

    public EmailRender render(EmailContent content) { ... }   // { String html; String text; }
}
```

**Visual rules (satisfy "clean, elegant, not boxed/card-based, responsive").**

- Full-bleed background `#f6f7f8`; one centred content table, `max-width: 600px`,
  `width: 100%`; horizontal padding `24px`, vertical rhythm from margins only.
- **No card**: no border, no border-radius, no box-shadow, no contrasting panel
  around the content. The content sits directly on the page background with the
  inner table transparent / same background. Structure is conveyed by type
  scale and a single `1px #e3e5e8` hairline under the wordmark and above the
  footer.
- Header: `brand.wordmark` as text, `18px/700`, letter-spacing `0.02em`,
  top-left, `28px` top padding. No logo image in the header.
- Body: `<h1>` `22px/600 #16181a`; paragraphs `15px/1.6 #3b3f45`; links
  `#1a56db` underlined.
  - `CodeBlock`: `28px/700`, letter-spacing `0.25em`, `#16181a`, on its own
    line with `18px` vertical margin. Not in a box.
  - `KeyValues`: a borderless two-column table, label `#6b7078` right-aligned in
    a narrow first column, value `#16181a`.
  - `Divider`: the same `1px #e3e5e8` hairline, `24px` margins.
- CTA: one `<a>` styled as a solid button — `background:#16181a; color:#fff;
  padding:12px 22px; border-radius:6px; font-weight:600; display:inline-block`.
  Minimum tap target ≥ 44px tall. A `border-radius` on a *button* is allowed;
  the rule is "no card around the content". The URL is always also emitted in
  the plain-text part.
- `items` section (Welcome only): `itemsHeading` as an `<h2>` `16px/600`, then
  one row per `ListItem` — `title` `15px/600`, `meta` `13px #6b7078` on the next
  line, `url` as a "View" link — rows separated by the `1px` hairline. **Plain
  rows, never bordered cards.** The whole section is skipped when `items` is
  empty.
- **Footer, byte-identical across every email:**
  1. hairline;
  2. contact block — `officeEn`, `hours`, each `brand.contact.emails` as a
     `mailto:` link, each `brand.contact.phones` as a `tel:` link; `13px/1.6
     #6b7078`;
  3. social row — for each of Facebook / Instagram / TikTok whose URL is
     configured, a `24×24` `<a><img></a>` with descriptive `alt` (e.g.
     `"Nadoumi on Instagram"`); centred; omitted entirely if none configured;
  4. the Nadoumi logo — `<img src="{brand.logoUrl()}" alt="Nadoumi" width="120">`
     centred, `24px` top margin;
  5. one legal line — `"© {year} Nadoumi. International education platform."`
     `12px #9aa0a6`;
  6. only when `content.showPreferencesLink()` — `"Manage your email
     preferences"` linking `brand.preferencesUrl()`.
- **Responsive / client:** table layout with `role="presentation"`,
  `cellpadding=0 cellspacing=0 border=0`; all decorative CSS inline; a single
  `<style>` in `<head>` for `@media (max-width:620px)` (drop side padding to
  `16px`, scale `<h1>` to `20px`) and `@media (prefers-color-scheme: dark)`
  (background `#16181a`, text `#e8eaed`, hairline `#2a2d31`, button inverts to
  `#f6f7f8`/`#16181a`, logo swap not attempted — the mono logo reads on both).
  MSO conditional comment wrapping the button for Outlook.
- **Accessibility:** `<!DOCTYPE html>`, `<html lang="{locale.language}">`,
  `<meta charset>`, `<title>` = heading, hidden preheader span, semantic
  `<h1>`/`<h2>`, every `<img>` has real `alt` (`alt=""` only for purely
  decorative), colour pairs meet WCAG AA (checked in tests via a small contrast
  helper), no information conveyed by colour alone, link text is meaningful
  ("View programme", not "click here").

**Plain-text generation (same method, same model).** `heading` upper-cased with
an underline of `=`; `Paragraph` as-is wrapped at ~72 cols; `CodeBlock` →
`CODE: 123456`; `KeyValues` → `label: value` lines; `Divider` → a `---` line;
`Cta` → `{label}: {url}`; `items` → `itemsHeading` then `- {title} — {meta} —
{url}` per row; footer → the contact lines, the configured social URLs as
`Facebook: {url}` lines, the legal line, and the preferences URL when
applicable. A test asserts every URL and the OTP present in the HTML is also
present in the text.

### 2.7 Static assets

`ruoyi-admin/src/main/resources/static/email/`:

| File | Notes |
|---|---|
| `nadoumi-logo.png` | 240×(auto) @2x, transparent, mono/dark ink. **Placeholder**, REPLACE-ME. |
| `facebook.png` `instagram.png` `tiktok.png` | 48×48 @2x, `#6b7078` glyph, transparent. **Placeholders**, REPLACE-ME. |

- Referenced by absolute URL (`brand.baseUrl + path`) — email clients need
  absolute `src`.
- `SecurityConfig`: add `/email/**` to the anonymous / `permitAll` matcher
  list (alongside the existing static-resource allowances). A test asserts an
  unauthenticated `GET /email/nadoumi-logo.png` returns `200`.
- Docs note: to change the logo or icons, replace the file and (if the CDN
  fronts `static/`) bust cache; no code change.

---

## 3. Design — identity emails on the shell (PROPOSED)

### 3.1 OTP + account-exists

`OtpService` keeps using `MailTemplates.render("otp-register" | "otp-password-reset"
| "account-exists", vars)` for the **copy**, then wraps:

```java
String copy = templates.render("otp-register", Map.of("ttlMinutes", ttl));
EmailContent content = EmailContent.builder("Verify your email address")
        .preheader("Your Nadoumi verification code")
        .paragraph(copy)                       // template text, minus the code line
        .code(code)                            // CodeBlock
        .paragraph("This code expires in " + ttl + " minutes and can be used once.")
        .paragraph("If you didn't start creating a Nadoumi account, you can ignore this email.")
        .build();
EmailRender r = emailLayout.render(content);
mail.send(new EmailMessage(email, "Verify your email — Nadoumi", r.text(), r.html()));
```

The `.txt` files are trimmed to just the prose paragraphs (the code and expiry
lines move into `EmailContent` so the code can be a `CodeBlock`). `MailTemplates`
and `MailTemplatesTest` are otherwise unchanged.

Standardised subjects and headings:

| Template | Subject | `<h1>` | CTA |
|---|---|---|---|
| `otp-register` | `Verify your email — Nadoumi` | Verify your email address | — |
| `otp-password-reset` | `Reset your Nadoumi password` | Reset your password | — |
| `account-exists` | `Your Nadoumi account` | You already have a Nadoumi account | `Sign in` → `loginUrl` |

### 3.2 `password-changed` (new)

- New template `resources/mail/password-changed.txt` (prose only).
- `EmailContent`: `<h1>` "Your password was changed", a paragraph noting the
  change and (if available) an approximate time, a paragraph "If this wasn't
  you, contact us immediately", CTA "Contact support" →
  `mailto:{first brand.contact.emails}`. Transactional → `showPreferencesLink =
  false`.
- Trigger: a small `PasswordChangedEvent(userId, email)` published by
  `StudentAuthService` after `resetPassword` and `changePassword`, consumed by a
  `@TransactionalEventListener(phase = AFTER_COMMIT)` listener in the identity
  module that resolves the address and calls `EmailLayout` + `MailSender`. Both
  methods already revoke sessions, so a failed email must not roll the password
  change back — hence AFTER_COMMIT, and the listener swallows/logs send failure
  (it is a courtesy notice, not a gate). Optionally `@Async` via the existing
  `ThreadPoolConfig` executor; acceptable to run inline in v1.

### 3.3 `MailDiagnostics`

Left sending plain text (`EmailMessage.text(...)`). Out of scope to prettify an
ops-only probe; a one-line comment records the choice.

---

## 4. Design — notification emails on the shell (PROPOSED)

### 4.1 `EmailNotificationChannel`

Gains `EmailLayout` + `BrandProperties`. The `NotificationChannel.send(recipient,
subject, body)` SPI signature is **unchanged**; the channel wraps the already
rendered `body` string into a standard `EmailContent`:

```java
EmailContent content = EmailContent.builder(subject)          // subject doubles as <h1> source
        .preheader(firstSentenceOf(body))
        .paragraphsFrom(body)                                  // split on blank lines
        .cta(ctaFor(type))                                     // type → destination
        .locale(Locale.ENGLISH)
        .showPreferencesLink(!type.isTransactional())
        .build();
```

`ctaFor(NotificationType)`:

| Type | CTA label | URL |
|---|---|---|
| `SCHOLARSHIP_PUBLISHED`, `SCHOLARSHIP_DEADLINE_REMINDER` | View scholarships | `{baseUrl}/scholarships` |
| `PROGRAM_PUBLISHED` | View programmes | `{baseUrl}/programs` |
| `UNIVERSITY_PUBLISHED` | View universities | `{baseUrl}/universities` |
| `CONTACT_INQUIRY_RECEIVED`, `TASK_PROGRESS` | Open the admin console | `{brand admin URL}` |
| `APPLICATION_SUBMITTED`, `APPLICATION_STATUS_CHANGED` | View your application | `{baseUrl}/account/applications` |

The channel needs `type` — `NotificationDeliveryDispatcher.attempt(...)` already
resolves `NotificationType type`; it passes it to the channel. Options:
(a) widen the SPI to `send(recipient, subject, body, NotificationType type)`;
(b) add an overload used only by `EmailNotificationChannel`. **Decision: (a)** —
one narrow SPI change, all three current channels (`EMAIL`, plus the `IN_APP`
no-op and any future SMS) get the type, which is generally useful. `IN_APP`
never reaches the dispatcher so only `EmailNotificationChannel` reads it today.

`nad_notification_template` EMAIL bodies are unchanged except: subjects
standardised to end `" — Nadoumi"` where they do not already
(`V55` `UPDATE`s the existing rows; see §6).

### 4.2 `contact-inquiry.txt`

Confirm during implementation that no code path renders it (`grep`), then delete
the file. If something still uses it, fold that call site onto `EmailLayout`
instead and note it in the PR.

---

## 5. Design — Welcome email (PROPOSED)

### 5.1 Trigger

`StudentAuthService.register(...)`, inside its existing
`@Transactional(rollbackFor = Exception.class)`, after
`identityMapper.markEmailVerified(...)`:

```java
outbox.append(OutboxEventTypes.STUDENT_REGISTERED, "user", user.getUserId(),
        Map.of("userId", user.getUserId(),
               "email", email,
               "firstName", req.firstName().trim(),
               "displayName", user.getNickName(),
               "locale", "en"));
```

- `OutboxWriter` is the `nadoumi-common` SPI (`Propagation.MANDATORY`) already
  used by the catalog producers; identity already depends on `nadoumi-common`.
- New constant `OutboxEventTypes.STUDENT_REGISTERED = "StudentRegistered"`.
- Emitting inside the tx means a rolled-back registration emits no event; the
  poller delivers after commit. Registration never fails because of email.

### 5.2 New notification type

```java
WELCOME(true, "Welcome to Nadoumi", Set.of())   // no secondary channel — see below
```

Transactional so a preference cannot suppress it. IN_APP is always produced (a
"Welcome" entry in the bell). The type declares **no** secondary channel on
purpose: the EMAIL for this type is composed and sent by
`WelcomeContentComposer` (§5.3), not by the generic
`NotificationDeliveryDispatcher` flat-string path — because the Welcome email
carries structured list sections the generic channel does not model. The
composer still writes an auditable EMAIL `nad_notification_delivery` row.

### 5.3 Composition — `WelcomeContentComposer` (in `nadoumi-notification`)

`nadoumi-notification`'s POM adds `nadoumi-scholarship` and `nadoumi-program`
(both build before it; verified no cycle — neither depends on notification).

`OutboxToNotificationDispatcher.dispatch(...)` gains a `StudentRegistered` branch
that delegates to `WelcomeContentComposer`:

1. Parse `userId`, `firstName`, `locale` from the payload.
2. `recipients = [userId]` (the payload-recipient path — **not** the
   `nad:notification:list` fan-out).
3. Pull dynamic sections from existing services, read-only:
   - **Featured programmes** — `ProgramService.list(new ProgramSearch(...),
     page=1, size=3)` ordered by most-recently-published (use whatever ordering
     the public list already exposes; no new query if avoidable). Map each to
     `ListItem(title = "{programName} — {universityName}", meta =
     "{degreeLevel} · {country}", url = "{baseUrl}/programs/{slug}")`.
   - **Hot scholarships** — `ScholarshipService.list(new ScholarshipSearch(...),
     page=1, size=3)` ordered by soonest upcoming `deadline` (reuse the public
     ordering; if the public list cannot order by deadline, add a narrow
     read-only mapper method `findUpcoming(limit)` in the scholarship module and
     expose it on `ScholarshipService` — a service-interface call, never the
     mapper directly). Map to `ListItem(title, meta = "Deadline {deadline} ·
     {country}", url = "{baseUrl}/scholarships/{slug}")`.
   - Student-facing fields only (`PublicScholarshipResponse` /
     `PublicProgramResponse`) — no university/partnership/internal data
     (`security.md` scholarship confidentiality; the public DTOs already
     enforce this).
4. Build `EmailContent`:
   - `<h1>` "Welcome to Nadoumi, {firstName}" (fallback "Welcome to Nadoumi"
     when `firstName` blank);
   - a short intro paragraph (from the `WELCOME` EMAIL template body, rendered
     by `NotificationRenderer` with `{{firstName}}`), then a "here's how to get
     started" paragraph;
   - `itemsHeading` "Programmes to explore" + programme `items` — **section
     omitted entirely if the list is empty**;
   - a second render pass appends `itemsHeading` "Scholarships closing soon" +
     scholarship `items` — same omit-if-empty rule. (`EmailContent` supports a
     single `items` group; the composer concatenates the two groups into one
     list with a `Divider`-led sub-heading, or `EmailContent` is extended to
     `List<ItemGroup>` — decide in implementation, favour the smaller change.)
   - CTA "Explore Nadoumi" → `{baseUrl}`;
   - `showPreferencesLink = false` (transactional).
5. Send:
   - `NotificationService.create(NotificationRequest.fromEvent(userId, WELCOME,
     "Welcome to Nadoumi", <in-app body>, "outbox:{eventId}:{userId}",
     payloadJson))` — because `WELCOME` declares no secondary channel this
     writes **only** the IN_APP row (recorded `SENT`), idempotent on
     `(userId, source_ref)` exactly like every other type. If the row already
     exists (`create` returns `0`), the composer stops here — the EMAIL was
     already sent on the first delivery.
   - The composer then renders the structured `EmailContent` via `EmailLayout`
     and sends it **directly through `MailSender`**, and inserts a
     `nad_notification_delivery` row for `(notificationId, channel=EMAIL)` set
     `SENT` (or `FAILED` + `next_attempt` on a thrown send, reusing the
     dispatcher's backoff constants) so the send is auditable in the staff
     notification console.
   - Idempotency for the direct send rides on the IN_APP `create` result above
     (the `(userId, source_ref)` unique key); the poller's at-least-once
     redelivery is a no-op.

> This is the one deliberate asymmetry in the design: the Welcome email is rich
> enough to justify bypassing the flat-string channel, and isolating it keeps
> the generic pipeline unchanged. Reviewed and accepted in the design
> conversation.

### 5.4 `WELCOME` template rows

`V55` seeds `WELCOME` / `IN_APP` (`body_tpl` = `"Welcome to Nadoumi — explore
programmes and scholarships from your dashboard."`) and `WELCOME` / `EMAIL`
(`subject_tpl` = `"Welcome to Nadoumi"`, `body_tpl` = the intro prose with
`{{firstName}}`). The composer uses the EMAIL `body_tpl` as the intro paragraph
so ops can edit the copy without a redeploy.

---

## 6. Design — Application email readiness (PROPOSED, no producer)

### 6.1 Constants + types

```java
// OutboxEventTypes
APPLICATION_SUBMITTED       = "ApplicationSubmitted";
APPLICATION_STATUS_CHANGED  = "ApplicationStatusChanged";

// NotificationType
APPLICATION_SUBMITTED(true,      "Application submitted",      Set.of(EMAIL)),
APPLICATION_STATUS_CHANGED(true, "Application status updated", Set.of(EMAIL)),
```

### 6.2 Dispatcher mapping

`OutboxToNotificationDispatcher.mapType` gets the two `case` labels. Recipients
come from the payload `recipientUserIds` array (the applicant's linked users —
the Application module supplies them). No fan-out, no permission audience.
Context vars the future producer must supply: `applicationRef`,
`opportunityTitle`, `status` (for status-changed), `firstName`.

### 6.3 Templates (`V55`)

`en` IN_APP + EMAIL rows for both types, `{{var}}` placeholders only, no data
that a producer will not supply:

- `APPLICATION_SUBMITTED` / EMAIL — subject `"We received your application —
  Nadoumi"`, body confirming `{{applicationRef}}` for `{{opportunityTitle}}` and
  what happens next.
- `APPLICATION_STATUS_CHANGED` / EMAIL — subject `"Your application status:
  {{status}} — Nadoumi"`, body naming `{{applicationRef}}`,
  `{{opportunityTitle}}`, `{{status}}`.

### 6.4 Documentation

`DOMAIN_EVENTS.md` lists `StudentRegistered` (producer wired now),
`ApplicationSubmitted` / `ApplicationStatusChanged` (producer = Application
module, **PLANNED**), with emit conditions and the context contract, so Step 6/7
implements against a fixed shape.

---

## 7. Migration — `V55__nad_notification_welcome_and_application_templates.sql`

- Next unused integer after `V54`. Append-only, immutable once merged.
- DML only (no DDL). Header comment with manual rollback:
  `delete from nad_notification_template where type in ('WELCOME',
  'APPLICATION_SUBMITTED','APPLICATION_STATUS_CHANGED');` plus the `UPDATE`
  reversions.
- `INSERT` the six new template rows (3 types × {IN_APP, EMAIL}, `locale='en'`,
  `create_by='system'`).
- `UPDATE` existing EMAIL `subject_tpl`s that don't end in `" — Nadoumi"` to add
  it (`SCHOLARSHIP_PUBLISHED`, `UNIVERSITY_PUBLISHED`, `PROGRAM_PUBLISHED`,
  `SCHOLARSHIP_DEADLINE_REMINDER`, `CONTACT_INQUIRY_RECEIVED`) — scoped to
  `channel='EMAIL' AND locale='en'`.
- Double-brace `{{var}}` only (Flyway placeholder replacement is on).
- `FlywayMigrationsIT` covers it automatically.

---

## 8. Configuration

`application.yml` additions (all env-overridable, local-dev defaults shown):

```yaml
nadoumi:
  web:
    loginUrl: ${NADOUMI_WEB_LOGIN_URL:http://localhost:3000/login}
    baseUrl:  ${NADOUMI_WEB_BASE_URL:http://localhost:3000}     # NEW, no trailing slash
  brand:
    wordmark: Nadoumi
    logoPath: /email/nadoumi-logo.png
    preferencesPath: /account/notifications
    contact:
      emails:  ${NADOUMI_BRAND_EMAILS:support@nadoumi.com}
      phones:  ${NADOUMI_BRAND_PHONES:+86 159 0823 7607}
      officeEn: ${NADOUMI_BRAND_OFFICE:Room 02, Floor 8, Unit 1, Building 6, Jiuzhou Beijun, No. 2 Fuxi Road, Fucheng District, Mianyang, Sichuan, China}
      hours:   ${NADOUMI_BRAND_HOURS:Monday to Friday, 09:00–18:00 (China Standard Time, UTC+8)}
    social:
      facebook:  ${NADOUMI_SOCIAL_FACEBOOK:}      # blank → icon omitted
      instagram: ${NADOUMI_SOCIAL_INSTAGRAM:}
      tiktok:    ${NADOUMI_SOCIAL_TIKTOK:}
```

Defaults for `contact.*` mirror `nadoumi-web/app/data/contact.ts`.
`DEVELOPMENT_GUIDELINES.md` records that the two must be kept in sync until a
shared API exists. `list`-shaped env vars are comma-split by Spring
(`emails`, `phones`).

---

## 9. Testing

### 9.1 New unit tests

- **`EmailLayoutTest`** (identity):
  - golden HTML + golden plain-text for a representative `EmailContent` (heading,
    two paragraphs, a `CodeBlock`, a CTA, two `items`, `showPreferencesLink =
    true`);
  - HTML/text **parity**: every URL and the code present in HTML is present in
    text;
  - footer present in every render; `officeEn`, `hours`, each email/phone
    rendered;
  - social icon `<a>` appears only for configured URLs (parameterised: none,
    one, all three);
  - `<html lang="en">`, `<title>`, hidden preheader, single `<h1>`, every `<img>`
    has non-empty `alt` (logo, social);
  - `role="presentation"` on layout tables;
  - a small contrast helper asserts each foreground/background pair ≥ 4.5:1
    (body text) / ≥ 3:1 (large text, button);
  - `items` section absent when `items` empty;
  - preferences link absent when `showPreferencesLink = false`.
- **`SmtpMailSenderTest`** (identity): with a mocked `JavaMailSender` capturing
  the `MimeMessage` — `htmlBody != null` → `multipart/alternative` with both
  parts; `htmlBody == null` → the existing `SimpleMailMessage` path; a
  `MessagingException` surfaces as a `MailException` (→ 502 mapping intact).
- **`BrandPropertiesTest`**: binding, comma-split lists, `logoUrl()` /
  `preferencesUrl()` composition, no trailing-slash doubling.
- **`WelcomeContentComposerTest`** (notification): mocked `ScholarshipService` /
  `ProgramService` —
  - both sections populated when services return items;
  - programme section omitted when `ProgramService` returns empty; scholarship
    section omitted when empty; both omitted → email still valid (intro + CTA);
  - only the registering `userId` is targeted (no fan-out);
  - idempotent on repeated dispatch of the same `(eventId, userId)`;
  - the composed EMAIL uses only public DTO fields (no `university` /
    `partnership` leakage) — an explicit assertion, satisfying the
    `security.md` test requirement for this new student-facing email path.

### 9.2 Updated tests

- **`OtpServiceTest`**: OTP still in the plain-text `body`; `htmlBody` non-null
  and contains the shell (`wordmark`, footer); subject strings updated.
- **`MailTemplatesTest`**: trimmed `.txt` copy contract (paragraphs only).
- **`LoggingMailSenderTest`**: outbox JSON line has an `"html"` field; `last(...)`
  still returns plain `body`.
- **`EmailNotificationChannel` test** (add if absent): body wrapped in the
  shell, plain-text alternative present, CTA URL matches the type, transactional
  types render no preferences link.
- **`NotificationRendererTest`**: unaffected (still renders the flat body);
  confirm no regression.

### 9.3 Integration

- **`FlywayMigrationsIT`**: `V55` applies; `NotificationRenderer.render(t, ch,
  "en", ctx)` resolves for `WELCOME`, `APPLICATION_SUBMITTED`,
  `APPLICATION_STATUS_CHANGED` on both channels with a representative context.
- **Welcome end-to-end** (`AbstractNadIntegrationTest` subclass, `log`
  transport): register a student via the OTP flow → assert one
  `nad_notification` `WELCOME` row for the new user, one EMAIL
  `nad_notification_delivery` row reaching `SENT`, and the `log` outbox's last
  message to that address has a non-null `html` containing the wordmark + footer
  + "Explore Nadoumi".
- **`SecurityConfig`**: unauthenticated `GET /email/nadoumi-logo.png` → `200`;
  a protected endpoint still → `401`.
- **No regression**: `NotificationPipelineTest`, `OtpEndToEndTest`,
  `DevMailEndpointDisabledTest`, `MediaAccessAuthorizationTest` stay green.

### 9.4 Manual follow-up (tracked, not blocking)

Render each email in Gmail (web + iOS/Android), Outlook (Windows + web), Apple
Mail, and one dark-mode client; confirm the non-boxed layout, footer, and logo.

---

## 10. Incremental delivery

Each phase: `mvn -q -pl <modules> -am test` green (plus the touched integration
tests) before the next. Docs touched in the phase that changes the behaviour
they describe.

1. **Foundation** — `EmailMessage.htmlBody`; `SmtpMailSender` /
   `LoggingMailSender` multipart; `BrandProperties` + config; `EmailContent` +
   `EmailLayout`; static assets + `SecurityConfig`; `EmailLayoutTest`,
   `SmtpMailSenderTest`, `BrandPropertiesTest`, `LoggingMailSenderTest`. No call
   site uses `EmailLayout` yet — zero behaviour change.
2. **Identity emails** — `OtpService` onto the shell; trim `.txt`s; new
   `password-changed` template + `PasswordChangedEvent` +
   `@TransactionalEventListener`; update `OtpServiceTest`, `MailTemplatesTest`;
   delete dead `contact-inquiry.txt` (if confirmed). Update
   `COMMUNICATION_AND_NOTIFICATIONS.md`.
3. **Notification announcements** — widen `NotificationChannel.send` with
   `NotificationType`; `EmailNotificationChannel` onto the shell + `ctaFor`;
   channel test. (Subject `" — Nadoumi"` polish lands with `V55` in phase 4/5.)
4. **Welcome email** — `OutboxEventTypes.STUDENT_REGISTERED`;
   `StudentAuthService.register` emits it; `NotificationType.WELCOME`;
   `nadoumi-notification` POM += scholarship + program; `WelcomeContentComposer`
   + dispatcher branch; `WelcomeContentComposerTest` + the end-to-end IT.
5. **Application readiness + `V55`** — the two `OutboxEventTypes`, the two
   `NotificationType`s, dispatcher `mapType` cases,
   `V55__nad_notification_welcome_and_application_templates.sql` (WELCOME +
   APPLICATION_* inserts, subject `UPDATE`s); `FlywayMigrationsIT` coverage;
   `DOMAIN_EVENTS.md`.
6. **Docs sweep** — `DEVELOPMENT_GUIDELINES.md` ("adding a transactional
   email"), `PLATFORM_ARCHITECTURE.md §8` phase tracker, final
   `COMMUNICATION_AND_NOTIFICATIONS.md` reconciliation.

---

## 11. Risks / open points

- **`ScholarshipService` / `ProgramService` public list ordering** — if neither
  can order by "soonest deadline" / "most recently published" without a new
  query, a narrow read-only mapper method + service accessor is added in the
  owning module (§5.3). Confirmed acceptable; keep it minimal.
- **`EmailContent` one group vs `List<ItemGroup>`** — Welcome needs two labelled
  list sections. Prefer extending `EmailContent` to hold a small
  `List<ItemGroup>` over hacking two renders together; decide when writing
  phase 4.
- **`@EnableConfigurationProperties` home** — `BrandProperties` must be
  registered where the mail beans already are. Identify the exact
  autoconfiguration in phase 1.
- **Welcome direct-send auditing** — the composer writes its own EMAIL
  `nad_notification_delivery` row; ensure the staff notification console query
  does not assume the dispatcher is the only writer.
- **Placeholder assets** — the logo and social PNGs ship as obvious
  placeholders; the PR description and `DEPLOYMENT.md` note they must be
  replaced and real social URLs set before production.
- **Dark-mode logo** — a single mono logo is used for both schemes rather than
  swapping assets; acceptable for v1.
```
