# P1 — Media & File Storage (Cloudinary) + Catalog Retrofit — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a provider-agnostic `MediaStorageService` with Cloudinary as the sole implementation, a `nad_media_asset` registry + `nad_media_access_log` audit, server-side validated uploads, PUBLIC / PROTECTED / SENSITIVE delivery, and move all catalog imagery (university logo/banner/gallery, scholarship hero/cover, program image) plus the applicant photo onto it.

**Architecture:** New foundational module `nadoumi-modules/nadoumi-media` holds `CloudinaryMediaStorage`, the `nad_media_asset` / `nad_media_access_log` mappers, `MediaValidation`, `MediaCategoryPolicy`, and a thin `MediaService` façade. The SPI (`MediaStorageService` + value types) lives in `nadoumi-common` so domain modules depend only on the abstraction and receive the impl by injection. Domain modules store a `*_media_id` `bigint` FK and never touch `nad_media_asset` directly. Bytes are never in MySQL; there is no local-filesystem implementation.

**Tech Stack:** Java 21 build target, Spring Boot 4.1, MyBatis 3.5, MySQL 8.4, Flyway, `com.cloudinary:cloudinary-http5`, Apache Tika (`tika-core`) for magic-byte detection, JUnit 5 + Testcontainers, Vue 3 / Element Plus (`nadoumi-admin`), Nuxt 3 (`nadoumi-web`).

**Spec:** `docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md` (Part I).

## Global Constraints

- RuoYi-Vue + Spring Boot + MyBatis + MySQL + Maven is the foundation; no core-tech swap. `nadoumi-media` is an approved new module (spec DM8).
- Cross-module calls go through a service interface only — never another module's mapper or entities. Domain modules use `MediaStorageService` / `MediaService`, never `MediaAssetMapper`.
- Separate DTOs per audience; never serialise a persistence entity. Student-facing responses never carry a Cloudinary URL for PROTECTED/SENSITIVE media.
- Migrations are append-only and immutable once merged: `V<n>__nad_<area>.sql`, next unused integer is **V26**. DDL and seed in separate files. Never edit a shipped `V*`.
- MyBatis: `mapUnderscoreToCamelCase` is DISABLED — every mapper needs an explicit `<resultMap>` or `as camelCase` aliases. Nadoumi mappers use fully-qualified `parameterType` / `resultType` (typeAliases only cover `com.ruoyi.**.domain`). `--` is illegal inside `<!-- -->` XML comments.
- Jackson `FAIL_ON_NULL_FOR_PRIMITIVES` is ON — request records use `Boolean` wrappers, not primitive `boolean`.
- `PageResponse<T>` (`com.nadoumi.common.web`) is zero-based: `PageHelper.startPage(page + 1, size)`.
- Exceptions: `NadNotFoundException` (404), `NadBadRequestException` (400), `NadForbiddenException` (403) from `com.nadoumi.identity.exception`.
- Public RuoYi endpoints use `@Anonymous`; staff endpoints `@PreAuthorize("@ss.hasPermi('nad:x:y'))` (exact match); mutations carry `@Log(title, businessType)`.
- English-only comments, identifiers, log messages. Modern clean Java, K&R braces, `final` fields, constructor injection, records for DTOs, no magic constants.
- `CLOUDINARY_URL` is required at startup — the app fails fast if absent/malformed. No `application.yml` default. No secret in logs.
- No test hits Cloudinary or a real filesystem: tests wire `FakeMediaStorage` via `@TestConfiguration`.
- Build Java modules with `JAVA_HOME=$(/usr/libexec/java_home -v 21)`. `nadoumi-common` / `nadoumi-media` must be `mvn install`ed before an offline (`-o`) build of a dependent module. Long `mvn` runs use `run_in_background: true` (macOS has no `timeout`).
- Commit only what the task changed; `git reset -q .claude .gitignore ry.sh` before every commit. Trailers on every commit:
  ```
  Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01ERVUQU6PwTfB1Rwo84ycrW
  ```
- Reviewer decisions taken (spec §6): module name `nadoumi-media`; `/api/media/{id}` indirection ships **and** DTOs also carry the direct `secure_url`; deprecated `*_image_url` columns kept read-only for one release; applicant photo is **PROTECTED** (signed URL, not proxy).

---

## File Structure

**New — `nadoumi-common`** (`com/nadoumi/common/media/`)
- `MediaStorageService.java` — the SPI.
- `MediaUploadCommand.java`, `StoredAsset.java`, `SignedUrl.java`, `ProxyStream.java`, `MediaOwnerRef.java` — value types (records).
- `MediaAccessClass.java`, `MediaOwnerKind.java`, `MediaCategory.java` — enums.
- **Delete** `com/nadoumi/common/storage/DocumentStorage.java` (unused D5 stub; superseded).

**New — `nadoumi-modules/nadoumi-media/`**
- `pom.xml`
- `src/main/java/com/nadoumi/media/package-info.java`
- `config/MediaProperties.java` — `@ConfigurationProperties("nadoumi.media")` (env, ttl, max upload MB).
- `config/CloudinaryConfig.java` — builds the `Cloudinary` bean from `CLOUDINARY_URL`; fails fast.
- `spi/CloudinaryMediaStorage.java` — `implements MediaStorageService`.
- `domain/MediaAsset.java`, `domain/MediaAccessLog.java` — mutable POJOs.
- `mapper/MediaAssetMapper.java`, `mapper/MediaAccessLogMapper.java`
- `src/main/resources/mapper/media/MediaAssetMapper.xml`, `MediaAccessLogMapper.xml`
- `policy/MediaCategoryPolicy.java` — static category → {default access class, allowed MIME set, max bytes, resource_type, folder}.
- `validation/MediaValidation.java` — boundary validator (size, filename, MIME allow-list, magic-byte sniff, denylist, checksum).
- `service/MediaService.java` — façade: `upload(...)`, `issueSignedUrl(...)`, `openProxyStream(...)`, `recordAccess(...)`, `find(...)`, `softDelete(...)`.
- `job/MediaReconciliationJob.java` — Quartz job (destroy DELETED assets past grace, flag orphans).
- `web/MediaController.java` — `GET /api/media/{id}` PUBLIC redirect.
- `src/test/java/com/nadoumi/media/FakeMediaStorage.java` — in-memory test double (also referenced by `ruoyi-admin` tests via test-jar).
- `src/test/java/com/nadoumi/media/MediaValidationTest.java`, `MediaCategoryPolicyTest.java`.

**Modified — build wiring**
- `nadoumi-modules/pom.xml` — add `<module>nadoumi-media</module>`.
- `pom.xml` (root) — `cloudinary.version` + `tika.version` properties; `dependencyManagement` entries for `nadoumi-media`, `cloudinary-http5`, `tika-core`.
- `ruoyi-admin/pom.xml` — `<dependency>` on `nadoumi-media`; test-jar dep on `nadoumi-media` (for `FakeMediaStorage`).

**New — migrations**
- `ruoyi-admin/src/main/resources/db/migration/V26__nad_media.sql`
- `ruoyi-admin/src/main/resources/db/migration/V27__nad_catalog_media_fk.sql`
- `ruoyi-admin/src/main/resources/db/migration/V28__nad_applicant_photo.sql`

**Modified — domain modules (retrofit)**
- `nadoumi-university`: `domain/University.java` (+`logoMediaId`,`bannerMediaId`), `UniversityGalleryImage.java` (+`mediaId`), `mapper/UniversityMapper.xml` + `UniversityMapper.java`, `service/UniversityService.java`, `web/StaffUniversityController.java` (+ upload endpoints), `web/request/UniversityRequest.java`, `web/response/UniversityResponse.java` + `PublicUniversityResponse.java`.
- `nadoumi-scholarship`: analogous (`heroMediaId`, `coverMediaId`), plus the `v_scholarship_student`-backed read path resolves URLs in the service.
- `nadoumi-program`: `imageMediaId` (new field), upload endpoint, request/response.
- `nadoumi-applicant`: `domain/Applicant.java` (+`photoMediaId`), mapper, `service/ApplicantService.java`, `web/StaffApplicantController.java` + a student photo endpoint, request/response.

**Modified — `ruoyi-admin` config + tests**
- `ruoyi-admin/src/main/resources/application.yml` — `spring.servlet.multipart` caps; `nadoumi.media.*` keys (no `CLOUDINARY_URL` default).
- `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/AbstractNadIntegrationTest.java` — `baseSetup()` clears `nad_media_access_log`, `nad_media_asset`; a `@DynamicPropertySource` `CLOUDINARY_URL` dummy; wire `FakeMediaStorage`.
- `ruoyi-admin/src/test/java/com/ruoyi/db/FlywayMigrationsIT.java` — counts 22→**25** fresh / 21→**24** baselined; assertions for V26/V27/V28.
- New: `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/MediaAccessAuthorizationTest.java`, `MediaAccessLogTest.java`, `CatalogImageRetrofitTest.java`.
- `ruoyi-admin/src/test/resources/application-test.yml` (if present) — `nadoumi.media.env: test`.

**Modified — `nadoumi-admin`**
- `src/components/ui/ImageUpload.vue` — post multipart to the owning module's upload endpoint; emit `{ mediaId, url }`.
- `src/utils/asset.ts` — reduce to absolute-URL passthrough.
- `src/api/{university,scholarship,program,applicant}.ts` — `*MediaId` fields; `*Url` read fields.
- `src/views/universities/UniversityDrawer.vue` + `detail.vue`; `src/views/scholarships/ScholarshipDrawer.vue` + `detail.vue`; `src/views/programs/ProgramDrawer.vue`; `src/views/applicants/*` (photo).
- `src/lang/en.ts` — `imageUpload.*` additions; keep `zh.ts` derived.

**Modified — `nadoumi-web`**
- **Delete** `app/utils/media.ts` and `server/routes/media/[...path].ts`.
- `app/components/marketing/{UniversityCard,ScholarshipCard,ProgramCard}.vue` — consume absolute `url`.
- `app/pages/universities/[slug].vue`, `app/pages/scholarships/[slug].vue`, `app/pages/programs/[slug].vue`, `app/pages/index.vue` — drop `mediaUrl()`.
- `app/types/catalog.ts` — `logoUrl` / `coverUrl` / `heroUrl` / `gallery[].url` are absolute; drop `*ImageUrl` optional fields once unused.
- `nuxt.config.ts` — `@nuxt/image` `cloudinary` provider (or add `res.cloudinary.com` to `domains`).
- `tests/unit/**` fixtures — absolute Cloudinary URLs.

**Modified — docs**
- `docs/ARCHITECTURE.md`, `docs/DEPLOYMENT.md`, `docs/DATABASE_DESIGN.md`, `docs/DOCUMENT_MANAGEMENT.md`, `docs/API_DESIGN.md`, `docs/SECURITY.md`, `docs/FRONTEND_ARCHITECTURE.md`, `docs/ADMIN_ARCHITECTURE.md`, `docs/PLATFORM_ARCHITECTURE.md` (§8 note).

---

## Task 1: `MediaStorageService` SPI in `nadoumi-common`

**Files:**
- Create: `nadoumi-modules/nadoumi-common/src/main/java/com/nadoumi/common/media/MediaStorageService.java`
- Create: `.../media/MediaUploadCommand.java`, `StoredAsset.java`, `SignedUrl.java`, `ProxyStream.java`, `MediaOwnerRef.java`
- Create: `.../media/MediaAccessClass.java`, `MediaOwnerKind.java`, `MediaCategory.java`
- Delete: `nadoumi-modules/nadoumi-common/src/main/java/com/nadoumi/common/storage/DocumentStorage.java`
- Test: `nadoumi-modules/nadoumi-common/src/test/java/com/nadoumi/common/media/MediaCategoryTest.java`

**Interfaces:**
- Produces: the exact SPI in spec §I.2 — `MediaStorageService` with `put`, `replace`, `publicUrl`, `signedUrl`, `openStream`, `find`, `softDelete`; `MediaUploadCommand(InputStream source, String originalFilename, String declaredContentType, long byteSize, MediaCategory category, MediaAccessClass accessClass, MediaOwnerRef owner, long uploadedBy, String checksumSha256)`; `StoredAsset(long id, String provider, MediaAccessClass accessClass, MediaCategory category, String resourceType, String deliveryType, String publicId, String assetId, Long cloudVersion, String secureUrl, String originalFilename, String contentType, long byteSize, Integer width, Integer height, String checksumSha256, long uploadedBy, MediaOwnerRef owner, String status, java.time.Instant createdAt)`; `SignedUrl(String url, java.time.Instant expiresAt)`; `ProxyStream(InputStream body, String contentType, long contentLength, String downloadFilename)`; `MediaOwnerRef(MediaOwnerKind kind, long id)`.
- `MediaAccessClass { PUBLIC, PROTECTED, SENSITIVE }`; `MediaOwnerKind { UNIVERSITY, SCHOLARSHIP, PROGRAM, APPLICANT, APPLICATION, DOCUMENT, USER }`; `MediaCategory { UNIVERSITY_LOGO, UNIVERSITY_BANNER, UNIVERSITY_GALLERY, SCHOLARSHIP_HERO, SCHOLARSHIP_COVER, PROGRAM_IMAGE, APPLICANT_PHOTO, APPLICANT_DOCUMENT, APPLICATION_DOCUMENT, ADMISSION_DOCUMENT, JW202, OTHER_ATTACHMENT }`.

- [ ] **Step 1: Confirm `DocumentStorage` is unused**

Run: `grep -rn "DocumentStorage" --include=*.java .`
Expected: only the file itself (and possibly a mention in `docs/`). If any `.java` imports it, stop and reconcile — otherwise proceed to delete it in Step 3.

- [ ] **Step 2: Write the enum/value types + SPI**

Create the eight `com.nadoumi.common.media` files verbatim from spec §I.2. All records; `MediaStorageService` is an `interface` with a class-level Javadoc pointing at the spec. No Spring, no persistence imports (keeps `nadoumi-common` pure JDK — matches its pom description).

- [ ] **Step 3: Delete the superseded stub**

```bash
git rm nadoumi-modules/nadoumi-common/src/main/java/com/nadoumi/common/storage/DocumentStorage.java
```
If `com/nadoumi/common/storage/` is now empty, remove the directory.

- [ ] **Step 4: Write the failing test**

```java
// MediaCategoryTest.java
package com.nadoumi.common.media;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MediaCategoryTest {

    @Test
    void everyCategoryIsCoveredByTheDocumentedSet() {
        assertThat(Arrays.stream(MediaCategory.values()).map(Enum::name))
                .containsExactlyInAnyOrder(
                        "UNIVERSITY_LOGO", "UNIVERSITY_BANNER", "UNIVERSITY_GALLERY",
                        "SCHOLARSHIP_HERO", "SCHOLARSHIP_COVER", "PROGRAM_IMAGE",
                        "APPLICANT_PHOTO", "APPLICANT_DOCUMENT", "APPLICATION_DOCUMENT",
                        "ADMISSION_DOCUMENT", "JW202", "OTHER_ATTACHMENT");
    }

    @Test
    void accessClassesAreThreeTiered() {
        assertThat(MediaAccessClass.values()).containsExactly(
                MediaAccessClass.PUBLIC, MediaAccessClass.PROTECTED, MediaAccessClass.SENSITIVE);
    }
}
```

- [ ] **Step 5: Run — expect compile + pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-common test`
Expected: BUILD SUCCESS, `MediaCategoryTest` green.

- [ ] **Step 6: Install to local repo**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-common install -DskipTests`
Expected: BUILD SUCCESS (downstream tasks build offline against this).

- [ ] **Step 7: Commit**

```bash
git add nadoumi-modules/nadoumi-common
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): MediaStorageService SPI in nadoumi-common; drop DocumentStorage stub"
```

---

## Task 2: `nadoumi-media` module skeleton + build wiring

**Files:**
- Create: `nadoumi-modules/nadoumi-media/pom.xml`, `.../src/main/java/com/nadoumi/media/package-info.java`
- Modify: `nadoumi-modules/pom.xml` (add module), `pom.xml` (root — properties + dependencyManagement), `ruoyi-admin/pom.xml` (add dependency + test-jar dependency)

**Interfaces:**
- Consumes: `nadoumi-common` (`MediaStorageService`).
- Produces: an installable `com.nadoumi:nadoumi-media` jar on the `ruoyi-admin` classpath; a `nadoumi-media` **test-jar** carrying `FakeMediaStorage`.

- [ ] **Step 1: Add version properties to the root `pom.xml`**

In `<properties>` add:
```xml
<cloudinary.version>2.3.0</cloudinary.version>
<tika.version>2.9.2</tika.version>
```
(Confirm the latest stable `com.cloudinary:cloudinary-http5` on Maven Central before pinning; use that exact version.)

- [ ] **Step 2: Add managed dependencies to the root `pom.xml` `<dependencyManagement>`**

```xml
<dependency>
  <groupId>com.nadoumi</groupId>
  <artifactId>nadoumi-media</artifactId>
  <version>${ruoyi.version}</version>
</dependency>
<dependency>
  <groupId>com.cloudinary</groupId>
  <artifactId>cloudinary-http5</artifactId>
  <version>${cloudinary.version}</version>
</dependency>
<dependency>
  <groupId>org.apache.tika</groupId>
  <artifactId>tika-core</artifactId>
  <version>${tika.version}</version>
</dependency>
```

- [ ] **Step 3: Create `nadoumi-modules/nadoumi-media/pom.xml`**

Parent `com.nadoumi:nadoumi-modules:3.9.2`. `packaging` jar. Dependencies: `nadoumi-common`; `spring-boot-starter-web`; `spring-boot-starter-validation`; `mybatis-spring-boot-starter`; `cloudinary-http5`; `tika-core`; `com.ruoyi:ruoyi-common` (for `@Anonymous`, `SecurityUtils`, `@Log`) — check how `nadoumi-identity` pulls RuoYi types and mirror it; `spring-boot-starter-quartz` **only if** `MediaReconciliationJob` needs it directly (it can instead be a `@Component` invoked by a `sys_job` row — decide in Task 12); `spring-boot-starter-test` (test scope). Add the `maven-jar-plugin` `test-jar` goal so `FakeMediaStorage` is publishable:
```xml
<build><plugins>
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <executions><execution><goals><goal>test-jar</goal></goals></execution></executions>
  </plugin>
</plugins></build>
```

- [ ] **Step 4: Add the module to `nadoumi-modules/pom.xml`**

Insert `<module>nadoumi-media</module>` after `<module>nadoumi-common</module>` (build order: media depends only on common).

- [ ] **Step 5: Wire into `ruoyi-admin/pom.xml`**

Add after the `nadoumi-program` dependency:
```xml
<dependency>
  <groupId>com.nadoumi</groupId>
  <artifactId>nadoumi-media</artifactId>
</dependency>
<dependency>
  <groupId>com.nadoumi</groupId>
  <artifactId>nadoumi-media</artifactId>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 6: Add `package-info.java`**

```java
/**
 * Media &amp; file storage. Owns {@code nad_media_asset} / {@code nad_media_access_log};
 * {@code CloudinaryMediaStorage} is the sole production impl of
 * {@link com.nadoumi.common.media.MediaStorageService}. Domain modules depend on the
 * SPI, never on this package's mappers. See
 * {@code docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md}.
 */
package com.nadoumi.media;
```

- [ ] **Step 7: Verify it builds and is on the app classpath**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media -am install -DskipTests
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl ruoyi-admin -am -DskipTests compile
```
Expected: both BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-modules/nadoumi-media/pom.xml nadoumi-modules/nadoumi-media/src/main/java pom.xml nadoumi-modules/pom.xml ruoyi-admin/pom.xml
git reset -q .claude .gitignore ry.sh
git commit -m "build(media): scaffold nadoumi-media module + Cloudinary/Tika dependency management"
```

---

## Task 3: V26 migration — `nad_media_asset` + `nad_media_access_log`

**Files:**
- Create: `ruoyi-admin/src/main/resources/db/migration/V26__nad_media.sql`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/db/FlywayMigrationsIT.java`

**Interfaces:**
- Produces: the two tables per spec §I.3 / §I.4. Running total after this task: **23 fresh / 22 baselined** migrations.

- [ ] **Step 1: Write `V26__nad_media.sql`**

`nad_media_asset` exactly per spec §I.3 (all columns, `uk_media_provider_public_id`, `idx_media_owner`, `idx_media_category`, `idx_media_status`, self-FK `superseded_by`). `nad_media_access_log` exactly per spec §I.4 (append-only, `idx_mal_asset`, `idx_mal_app`, `idx_mal_actor`, FK `media_asset_id → nad_media_asset(id) ON DELETE RESTRICT`). Header comment: purpose + "manual rollback: DROP TABLE nad_media_access_log, nad_media_asset;". `engine=innodb default charset=utf8mb4`. Audit columns follow the RuoYi `BaseEntity` convention (`create_by varchar(64)`, `create_time datetime`, `update_by`, `update_time`).

- [ ] **Step 2: Write the failing IT assertions**

In `FlywayMigrationsIT`, bump `freshDatabase_appliesTheWholeChain` expected count `22 → 23` and `existingRuoYiDatabase...` `21 → 22`. Add:
```java
assertThat(tableExists("nad_media_asset")).isTrue();
assertThat(tableExists("nad_media_access_log")).isTrue();
assertThat(columnExists("nad_media_asset", "access_class")).isTrue();
assertThat(columnExists("nad_media_asset", "public_id")).isTrue();
assertThat(indexExists("nad_media_asset", "uk_media_provider_public_id")).isTrue();
```
(Use the same helper style already in the file.)

- [ ] **Step 3: Run the IT**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -pl ruoyi-admin -Dit.test=FlywayMigrationsIT -DfailIfNoTests=false verify` (background; needs Docker).
Expected: `FlywayMigrationsIT` green, migration count 23 / 22.

- [ ] **Step 4: Commit**

```bash
git add ruoyi-admin/src/main/resources/db/migration/V26__nad_media.sql ruoyi-admin/src/test/java/com/ruoyi/db/FlywayMigrationsIT.java
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): V26 nad_media_asset + nad_media_access_log"
```

---

## Task 4: `MediaAsset` / `MediaAccessLog` domain + mappers

**Files:**
- Create: `nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/domain/MediaAsset.java`, `domain/MediaAccessLog.java`
- Create: `.../mapper/MediaAssetMapper.java`, `.../mapper/MediaAccessLogMapper.java`
- Create: `nadoumi-modules/nadoumi-media/src/main/resources/mapper/media/MediaAssetMapper.xml`, `MediaAccessLogMapper.xml`
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/mapper/MediaMapperSmokeTest.java` *(optional — the real coverage is the `ruoyi-admin` ITs; keep this a compile/loads check or skip if it needs a DB)*

**Interfaces:**
- Produces: `MediaAssetMapper` — `insert(MediaAsset)`, `findById(Long)`, `updateStatus(@Param("id") long, @Param("status") String, @Param("supersededBy") Long, @Param("by") String)`, `findStaleDeleted(@Param("before") LocalDateTime)`, `findByOwner(@Param("kind") String, @Param("id") long)`. `MediaAccessLogMapper` — `insert(MediaAccessLog)` only (append-only).
- Consumes: nothing outside the module.

- [ ] **Step 1: Write `MediaAsset` / `MediaAccessLog` POJOs**

Mutable POJOs mirroring the V26 columns (Java field names camelCase). `MediaAsset` also carries convenience getters `MediaAccessClass getAccessClassEnum()` etc. if useful, but keep the stored values as `String` to match the mapper.

- [ ] **Step 2: Write the mapper interfaces**

`@Mapper`-less (project uses `@MapperScan("com.nadoumi.**.mapper")` — confirm the scan covers `com.nadoumi.media.mapper`; the pattern `com.nadoumi.**.mapper` does). Fully-qualified `parameterType` / `resultType` in XML.

- [ ] **Step 3: Write the XML with an explicit `<resultMap>`**

`mapUnderscoreToCamelCase` is off — map every column (`access_class` → `accessClass`, `public_id` → `publicId`, `cloud_version` → `cloudVersion`, `byte_size` → `byteSize`, `owner_kind` → `ownerKind`, …). `mapper` XML lives under `src/main/resources/mapper/media/` (matches `classpath*:mapper/**/*Mapper.xml`).

- [ ] **Step 4: Build**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media -am install -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/domain nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/mapper nadoumi-modules/nadoumi-media/src/main/resources
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): nad_media_asset / nad_media_access_log domain + MyBatis mappers"
```

---

## Task 5: `MediaCategoryPolicy`

**Files:**
- Create: `nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/policy/MediaCategoryPolicy.java`
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/MediaCategoryPolicyTest.java`

**Interfaces:**
- Produces: `MediaCategoryPolicy.of(MediaCategory) -> CategoryRule` where `CategoryRule(MediaAccessClass defaultAccessClass, Set<String> allowedMime, long maxBytes, String resourceType, String folderSuffix)`. Static, no Spring. `folder(MediaCategory, String env) -> "nadoumi/<env>/<suffix>"`.

- [ ] **Step 1: Write the failing test**

```java
@Test void logoIsPublicImage4mb() {
    var r = MediaCategoryPolicy.of(MediaCategory.UNIVERSITY_LOGO);
    assertThat(r.defaultAccessClass()).isEqualTo(MediaAccessClass.PUBLIC);
    assertThat(r.allowedMime()).containsExactlyInAnyOrder("image/jpeg","image/png","image/webp");
    assertThat(r.maxBytes()).isEqualTo(4L * 1024 * 1024);
    assertThat(r.resourceType()).isEqualTo("image");
}
@Test void jw202IsSensitivePdf20mb() {
    var r = MediaCategoryPolicy.of(MediaCategory.JW202);
    assertThat(r.defaultAccessClass()).isEqualTo(MediaAccessClass.SENSITIVE);
    assertThat(r.allowedMime()).contains("application/pdf");
    assertThat(r.maxBytes()).isEqualTo(20L * 1024 * 1024);
}
@Test void folderIsEnvNamespaced() {
    assertThat(MediaCategoryPolicy.folder(MediaCategory.SCHOLARSHIP_HERO, "prod"))
        .isEqualTo("nadoumi/prod/scholarship/hero");
}
@Test void everyCategoryHasARule() {
    for (var c : MediaCategory.values()) assertThat(MediaCategoryPolicy.of(c)).isNotNull();
}
```

- [ ] **Step 2: Implement from the spec §I.5 table**

Named constants for the size caps (`MB = 1024*1024`); no magic numbers. An `EnumMap<MediaCategory, CategoryRule>` built once in a static initializer.

- [ ] **Step 3: Run — expect pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media test`

- [ ] **Step 4: Commit**

```bash
git add nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/policy nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/MediaCategoryPolicyTest.java
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): MediaCategoryPolicy — per-category access class, MIME, size, folder"
```

---

## Task 6: `MediaValidation`

**Files:**
- Create: `nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/validation/MediaValidation.java`
- Create: `.../validation/MediaValidationException.java` (extends `NadBadRequestException`? — no; it needs distinct HTTP codes. Make it carry an enum `Reason { TOO_LARGE(413), DISALLOWED_TYPE(415), TYPE_MISMATCH(422), BAD_NAME(422) }` and let the module's `@RestControllerAdvice` — or the existing Nadoumi problem+json handler — map it. Check how `nadoumi-identity` exceptions are mapped and follow that.)
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/validation/MediaValidationTest.java`

**Interfaces:**
- Produces: `MediaValidation.check(InputStream head, String declaredContentType, String originalFilename, long byteSize, MediaCategory category) -> ValidatedUpload(String sanitizedFilename, String resolvedContentType)`. Throws `MediaValidationException(Reason)`.
- Consumes: `MediaCategoryPolicy`, Tika `org.apache.tika.detect.DefaultDetector` / `TikaConfig`.

- [ ] **Step 1: Write the failing tests**

- `rejectsOversize` — `byteSize` one over `policy.maxBytes` → `TOO_LARGE`.
- `rejectsDisallowedDeclaredType` — `text/html` for `UNIVERSITY_LOGO` → `DISALLOWED_TYPE`.
- `rejectsSvg` and `rejectsHtml` and `rejectsExecutable` regardless of category → `DISALLOWED_TYPE`.
- `rejectsDeclaredImageButSniffedPdf` — declared `image/png`, bytes are `%PDF` → `TYPE_MISMATCH`.
- `acceptsRealPng` — 1×1 PNG bytes, declared `image/png`, category `UNIVERSITY_LOGO` → returns `resolvedContentType = "image/png"`.
- `acceptsRealPdfForDocument` — `%PDF-1.4` bytes, declared `application/pdf`, category `APPLICATION_DOCUMENT` → ok.
- `sanitizesFilename` — `"../../etc/passwd .png"` → `"etc_passwd.png"` (no separators, no control chars, ≤ 100).
- `blankFilenameGetsGenerated` — `""` → matches `^upload-[0-9a-f-]{36}\.png$` (extension from resolved type).

Use small literal byte arrays for PNG/PDF/GIF signatures.

- [ ] **Step 2: Implement**

Order: size → filename sanitize → declared-type allow-list → Tika sniff on first 8 KiB → sniffed in allow-list → declared/sniffed family match → hard denylist (`text/html`, `image/svg+xml`, `application/xhtml+xml`, `application/x-msdownload`, `application/x-sh`, `application/zip`, `application/java-archive`). Compute nothing that needs the full stream here (checksum is done in `MediaService` while streaming to Cloudinary).

- [ ] **Step 3: Run — expect pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media test`

- [ ] **Step 4: Commit**

```bash
git add nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/validation nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/validation
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): MediaValidation — size, filename, MIME allow-list + magic-byte sniff"
```

---

## Task 7: `MediaProperties` + `CloudinaryConfig` (fail-fast)

**Files:**
- Create: `.../config/MediaProperties.java`, `.../config/CloudinaryConfig.java`
- Modify: `ruoyi-admin/src/main/resources/application.yml`
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/config/CloudinaryConfigTest.java`

**Interfaces:**
- Produces: `MediaProperties` — `String env` (default `dev`), `int signedUrlTtlSeconds` (default 180, clamp 60–600 in a `@PostConstruct` / accessor), `int maxUploadMb` (default 20). Bound from `nadoumi.media.*`.
- Produces: a `Cloudinary` `@Bean` built from `CLOUDINARY_URL`; throws `IllegalStateException("CLOUDINARY_URL is not configured")` when the env var is missing/blank/malformed, at context init.

- [ ] **Step 1: Write the failing test**

`CloudinaryConfigTest` — an `ApplicationContextRunner` with `CloudinaryConfig`:
- no `CLOUDINARY_URL` → context fails, message contains `CLOUDINARY_URL`.
- `CLOUDINARY_URL=cloudinary://k:s@demo` → `Cloudinary` bean present, `cloudinary.config.cloudName == "demo"`.

- [ ] **Step 2: Implement**

`@Configuration @EnableConfigurationProperties(MediaProperties.class)`. Read `System.getenv("CLOUDINARY_URL")` (Cloudinary's SDK also reads it, but read explicitly to fail fast with a clear message). Clamp TTL with named constants `MIN_TTL = 60`, `MAX_TTL = 600`.

- [ ] **Step 3: Add config keys to `application.yml`**

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 22MB
nadoumi:
  media:
    env: ${NADOUMI_MEDIA_ENV:dev}
    signed-url-ttl-seconds: ${NADOUMI_MEDIA_SIGNED_URL_TTL_SECONDS:180}
    max-upload-mb: ${NADOUMI_MEDIA_MAX_UPLOAD_MB:20}
```
**No `CLOUDINARY_URL` key** — it stays a pure environment variable, undefined here so the app fails fast if the deployer forgets it.

- [ ] **Step 4: Run — expect pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media test`

- [ ] **Step 5: Commit**

```bash
git add nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/config nadoumi-modules/nadoumi-media/src/test ruoyi-admin/src/main/resources/application.yml
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): MediaProperties + fail-fast CloudinaryConfig; multipart caps"
```

---

## Task 8: `FakeMediaStorage` (test double) + `CloudinaryMediaStorage`

**Files:**
- Create: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/FakeMediaStorage.java`
- Create: `nadoumi-modules/nadoumi-media/src/main/java/com/nadoumi/media/spi/CloudinaryMediaStorage.java`
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/spi/FakeMediaStorageTest.java`

**Interfaces:**
- Consumes: `MediaStorageService`, `MediaAssetMapper`, `MediaCategoryPolicy`, `MediaProperties`, `Cloudinary`.
- Produces: `FakeMediaStorage implements MediaStorageService` — deterministic: `put` persists a `nad_media_asset` row via the real `MediaAssetMapper` (so ITs see rows) but stores bytes in an in-memory `Map<Long,byte[]>`; `signedUrl` returns `https://fake.local/<publicId>?exp=<epoch>`; `publicUrl` returns `https://fake.local/public/<publicId>`; `openStream` returns the stored bytes. `CloudinaryMediaStorage` — the real impl; `put` uploads via `cloudinary.uploader().upload(bytes, options)` with `type` = `upload` (PUBLIC) / `authenticated` (PROTECTED/SENSITIVE), `resource_type` from the policy, `folder` from `MediaCategoryPolicy.folder(cat, props.env())`, `use_filename=false`, `unique_filename=true`; persists a row; `signedUrl` = `cloudinary.url().signed(true).resourceType(...).type("authenticated").expiresAt(...).generate(publicId)`; `openStream` opens an internal signed URL server-side via an HTTP client and returns the stream; `softDelete` updates the row status only (destroy is the reconciliation job).

- [ ] **Step 1: Write `FakeMediaStorage`**

Constructor takes `MediaAssetMapper`. Thread-safe `ConcurrentHashMap`. Honour `accessClass`: `secureUrl` non-null only for PUBLIC. Compute SHA-256 of the bytes for the row's `checksum_sha256`.

- [ ] **Step 2: Write `FakeMediaStorageTest`** *(pure unit — mock `MediaAssetMapper`)*

- `putPersistsRowAndReturnsStoredAsset` — verify `mapper.insert(...)` called; returned `StoredAsset.secureUrl()` set for PUBLIC, null for PROTECTED.
- `signedUrlForProtectedHasExpiry` — `expiresAt` ≈ now + ttl.
- `publicUrlThrowsForProtected`, `signedUrlThrowsForPublic` — guard-rail parity with the real impl.
- `openStreamReturnsStoredBytes`.

- [ ] **Step 3: Implement `CloudinaryMediaStorage`**

Match the guard-rails asserted above (throw `IllegalArgumentException` for wrong access class per method). Log `public_id` + bytes + owner, never the URL for PROTECTED/SENSITIVE, never the secret.

- [ ] **Step 4: Run — expect pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media test`

- [ ] **Step 5: Install (with test-jar)**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media install -DskipTests`
Expected: both `nadoumi-media-3.9.2.jar` and `nadoumi-media-3.9.2-tests.jar` in `~/.m2`.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-modules/nadoumi-media/src
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): CloudinaryMediaStorage impl + FakeMediaStorage test double"
```

---

## Task 9: `MediaService` façade + `MediaController`

**Files:**
- Create: `.../service/MediaService.java`, `.../web/MediaController.java`
- Create: `.../web/dto/MediaUploadResult.java` (`record MediaUploadResult(long mediaId, String url)` — `url` null for PROTECTED/SENSITIVE)
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/service/MediaServiceTest.java`

**Interfaces:**
- Produces:
  - `MediaService.upload(MultipartFile file, MediaCategory category, MediaAccessClass accessClassOrNull, MediaOwnerRef owner) -> StoredAsset` — runs `MediaValidation.check`, resolves access class (arg else `policy.defaultAccessClass`), streams to `MediaStorageService.put` while computing SHA-256.
  - `MediaService.issueSignedUrl(long assetId, MediaAccessLogContext ctx) -> SignedUrl` — asserts the asset is PROTECTED, writes a `nad_media_access_log` `GRANTED` row, returns `mediaStorage.signedUrl(assetId, ttl)`.
  - `MediaService.openProxyStream(long assetId, MediaAccessLogContext ctx) -> ProxyStream` — asserts PROTECTED or SENSITIVE, logs `STREAM_PROXY GRANTED`, returns `mediaStorage.openStream(assetId)`.
  - `MediaService.denyAndLog(long assetId, MediaAccessLogContext ctx, String reason)` — writes a `DENIED` row; caller then throws `NadForbiddenException`.
  - `MediaService.find(long assetId) -> Optional<StoredAsset>`; `MediaService.softDelete(long assetId, long actorUserId)`.
  - `MediaAccessLogContext(long actorUserId, Long actorApplicantId, Long applicationId, Long documentId, String ip, String userAgent)`.
- Consumes: `MediaStorageService`, `MediaValidation`, `MediaCategoryPolicy`, `MediaAssetMapper`, `MediaAccessLogMapper`, `MediaProperties`.

- [ ] **Step 1: Write the failing `MediaServiceTest`** (unit, mocked collaborators)

- `uploadValidatesThenPuts` — bad MIME → `MediaValidationException`, `mediaStorage.put` never called.
- `uploadResolvesDefaultAccessClass` — null arg + `UNIVERSITY_LOGO` → PUBLIC passed to `put`.
- `issueSignedUrlWritesGrantedLog` — verify `accessLogMapper.insert` with `result=GRANTED`, `access_kind=SIGNED_URL_ISSUED`, `ttl_seconds` = props.
- `issueSignedUrlRejectsPublicAsset` — PUBLIC asset → `IllegalArgumentException` (callers use the public URL).
- `openProxyStreamAllowsSensitive` — SENSITIVE asset → returns stream, logs `STREAM_PROXY`.
- `denyAndLogWritesDeniedRow`.

- [ ] **Step 2: Implement `MediaService`**

`@Service`, constructor injection, `@Transactional` on `upload` and `softDelete`. Access-log writes are their own short transaction (`REQUIRES_NEW`) so a later `NadForbiddenException` does not roll back the audit row.

- [ ] **Step 3: Implement `MediaController`**

```java
@RestController
@RequestMapping("/api/media")
class MediaController {
    @GetMapping("/{id}")
    @Anonymous
    ResponseEntity<Void> get(@PathVariable long id) {
        StoredAsset a = service.find(id).orElseThrow(() -> new NadNotFoundException("media not found"));
        if (a.accessClass() != MediaAccessClass.PUBLIC) throw new NadNotFoundException("media not found"); // never reveal protected via this route
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(a.secureUrl())).build();
    }
}
```

- [ ] **Step 4: Run — expect pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media test`

- [ ] **Step 5: Install + commit**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-media install -DskipTests
git add nadoumi-modules/nadoumi-media/src
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): MediaService facade (upload/sign/proxy/audit) + GET /api/media/{id}"
```

---

## Task 10: V27 migration — catalog media FKs + recreate `v_scholarship_student`

**Files:**
- Create: `ruoyi-admin/src/main/resources/db/migration/V27__nad_catalog_media_fk.sql`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/db/FlywayMigrationsIT.java`

**Interfaces:**
- Produces: `nad_university.logo_media_id` / `banner_media_id`; `nad_university_gallery.media_id`; `nad_scholarship.hero_media_id` / `cover_media_id`; `nad_program.image_media_id` (all `bigint null`, FK → `nad_media_asset(id) ON DELETE SET NULL`). `v_scholarship_student` recreated to also select `hero_media_id`, `cover_media_id` (keeps every existing column; still no university/partnership/commission column). Running total: **24 fresh / 23 baselined**.

- [ ] **Step 1: Write `V27__nad_catalog_media_fk.sql`**

`alter table … add column … after …`; `add constraint fk_…_media foreign key (…) references nad_media_asset(id) on delete set null`. Then `drop view if exists v_scholarship_student; create view v_scholarship_student as select … , s.hero_media_id, s.cover_media_id from nad_scholarship s where s.status='ACTIVE' and s.publish_status='PUBLISHED';` — copy the current column list from `V25__nad_scholarship_reference.sql` and append the two. Header comment lists the manual rollback (drop columns + FKs, recreate the V25 view).

- [ ] **Step 2: Update `FlywayMigrationsIT`**

Counts `23 → 24` / `22 → 23`. Assert:
```java
assertThat(columnExists("nad_university", "logo_media_id")).isTrue();
assertThat(columnExists("nad_university_gallery", "media_id")).isTrue();
assertThat(columnExists("nad_scholarship", "hero_media_id")).isTrue();
assertThat(columnExists("nad_program", "image_media_id")).isTrue();
assertThat(viewColumnExists("v_scholarship_student", "hero_media_id")).isTrue();
// still no leak:
assertThat(viewColumnExists("v_scholarship_student", "university_id")).isFalse();
```

- [ ] **Step 3: Run the IT** (background; Docker)

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -pl ruoyi-admin -Dit.test=FlywayMigrationsIT -DfailIfNoTests=false verify`

- [ ] **Step 4: Commit**

```bash
git add ruoyi-admin/src/main/resources/db/migration/V27__nad_catalog_media_fk.sql ruoyi-admin/src/test/java/com/ruoyi/db/FlywayMigrationsIT.java
git reset -q .claude .gitignore ry.sh
git commit -m "feat(media): V27 catalog *_media_id FKs + recreate v_scholarship_student"
```

---

## Task 11: V28 migration — `nad_applicant.photo_media_id`

**Files:**
- Create: `ruoyi-admin/src/main/resources/db/migration/V28__nad_applicant_photo.sql`
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/db/FlywayMigrationsIT.java`

**Interfaces:**
- Produces: `nad_applicant.photo_media_id bigint null` + FK → `nad_media_asset(id) ON DELETE SET NULL`. Running total: **25 fresh / 24 baselined** — the final P1 count.

- [ ] **Step 1: Write `V28__nad_applicant_photo.sql`** (single `alter table` + FK; header rollback comment).
- [ ] **Step 2: `FlywayMigrationsIT`** counts `24 → 25` / `23 → 24`; assert `columnExists("nad_applicant", "photo_media_id")`.
- [ ] **Step 3: Run the IT** (background; Docker).
- [ ] **Step 4: Commit** — `feat(media): V28 nad_applicant.photo_media_id`.

---

## Task 12: `MediaReconciliationJob` + `sys_job` seed

**Files:**
- Create: `.../job/MediaReconciliationJob.java`
- Decide: add a `sys_job` row via a small seed inside **V26** *(if V26 not yet merged, fold it in)* or a new `V29__nad_media_job_seed.sql`. Since V26 will already be committed by now, use **V29** (DDL/seed separation is fine — this is seed).
- Modify: `FlywayMigrationsIT` (counts `25 → 26` / `24 → 25`) — **note: this makes the P1 final count 26 / 25**; update the spec's §4 note when doing docs (Task 20).
- Test: `nadoumi-modules/nadoumi-media/src/test/java/com/nadoumi/media/job/MediaReconciliationJobTest.java`

**Interfaces:**
- Produces: `MediaReconciliationJob.run()` — for each `nad_media_asset` with `status='DELETED'` and `deleted_at < now - grace`: if no `nad_media_access_log` / (later) `nad_document_version` references it → `mediaStorage` provider destroy + hard-delete row; else set `status='PURGED'` + provider destroy only. `grace` = `nadoumi.media.reconcile-grace-days` (default 30).

- [ ] **Step 1: Write the failing test** — mocked mappers + `MediaStorageService`; assert destroy called for a stale DELETED row, not for a fresh one; row hard-deleted when unreferenced, `PURGED` when a log row references it.
- [ ] **Step 2: Implement** as a plain `@Component` with a `run()` method (RuoYi Quartz invokes beans by name — mirror an existing `ryTask`-style job; check `ruoyi-quartz` samples).
- [ ] **Step 3: Write `V29__nad_media_job_seed.sql`** — insert a `sys_job` (`job_name='Media reconciliation'`, `invoke_target='mediaReconciliationJob.run()'`, `cron_expression='0 30 3 * * ?'`, `status='0'`), idempotent (`on duplicate key`/guarded).
- [ ] **Step 4: `FlywayMigrationsIT`** counts `25 → 26` / `24 → 25`; assert the `sys_job` row exists.
- [ ] **Step 5: Run** module test + the IT (background) — commit `feat(media): asset reconciliation job + V29 sys_job seed`.

---

## Task 13: `AbstractNadIntegrationTest` — wire `FakeMediaStorage` + clear media tables

**Files:**
- Modify: `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/AbstractNadIntegrationTest.java`
- Create: `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/support/MediaTestConfig.java`

**Interfaces:**
- Consumes: `FakeMediaStorage` (from the `nadoumi-media` test-jar), `MediaAssetMapper`.
- Produces: every existing IT continues to pass with a `@Primary FakeMediaStorage` bean replacing `CloudinaryMediaStorage`; `baseSetup()` truncates the media tables.

- [ ] **Step 1: `MediaTestConfig`**

```java
@TestConfiguration
class MediaTestConfig {
    @Bean @Primary
    MediaStorageService fakeMediaStorage(MediaAssetMapper mapper) { return new FakeMediaStorage(mapper); }
}
```
Import it from `AbstractNadIntegrationTest` via `@Import(MediaTestConfig.class)`.

- [ ] **Step 2: `@DynamicPropertySource`** — add `r.add("CLOUDINARY_URL" ...)`? Env vars can't be set this way. Instead: guard `CloudinaryConfig`'s `Cloudinary` bean with `@ConditionalOnProperty` / `@ConditionalOnMissingBean` so `MediaTestConfig`'s `@Primary` fake wins and the real `Cloudinary` bean is only built when `CLOUDINARY_URL` is present. Simplest: make `CloudinaryConfig` `@ConditionalOnProperty(name = "nadoumi.media.provider", havingValue = "cloudinary", matchIfMissing = true)` **and** tolerate a missing bean in tests by having `MediaTestConfig` also stub a no-op `Cloudinary`. Pick one approach in Task 7 and reference it here. **Chosen:** `CloudinaryMediaStorage` and the `Cloudinary` bean are `@ConditionalOnMissingBean(MediaStorageService.class)`; the fail-fast check moves into `CloudinaryMediaStorage`'s constructor, which never runs in tests because the fake is `@Primary` + registered first via `@Import`. Update Task 7/8 accordingly.

- [ ] **Step 3: `baseSetup()`** — add, before the `nad_university` delete:
```java
jdbc.update("delete from nad_media_access_log");
jdbc.update("update nad_university set logo_media_id=null, banner_media_id=null");
jdbc.update("update nad_university_gallery set media_id=null");
jdbc.update("update nad_scholarship set hero_media_id=null, cover_media_id=null");
jdbc.update("update nad_program set image_media_id=null");
jdbc.update("update nad_applicant set photo_media_id=null");
jdbc.update("delete from nad_media_asset");
```
(Null the FKs first so the `nad_media_asset` delete isn't blocked by `ON DELETE SET NULL` — actually `SET NULL` allows the delete, but doing it explicitly keeps row state clean between tests.)

- [ ] **Step 4: Run the existing nadoumi ITs** (background):
`JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -pl ruoyi-admin verify`
Expected: all currently-green suites still green (StaffUniversityTest, StaffScholarshipTest, StaffProgramTest, StaffApplicantSubResourceTest, FlywayMigrationsIT, …).

- [ ] **Step 5: Commit** — `test(media): wire FakeMediaStorage into the IT base + clear media tables`.

---

## Task 14: University retrofit — upload endpoints + `*MediaId` + resolved URLs

**Files:**
- Modify: `nadoumi-modules/nadoumi-university/pom.xml` (add `nadoumi-media`? — no; add `nadoumi-common` already there; the controller needs `MediaService` which is in `nadoumi-media`. Add a `nadoumi-media` dependency, OR move `MediaService` behaviour needed by controllers into a `nadoumi-common` interface `MediaGateway` implemented by `nadoumi-media`. **Chosen:** add a `MediaGateway` interface to `nadoumi-common` — `upload(...)`, `find(...)`, `publicUrl(...)`, `issueSignedUrl(...)`, `openProxyStream(...)` — implemented by `MediaService` in `nadoumi-media`. Domain modules depend on `nadoumi-common` only. Update Tasks 1 & 9: `MediaGateway` in common, `MediaService implements MediaGateway`.)
- Modify: `domain/University.java`, `domain/UniversityGalleryImage.java`, `mapper/UniversityMapper.java` + `.xml`, `service/UniversityService.java`, `web/StaffUniversityController.java`, `web/request/UniversityRequest.java`, `web/response/UniversityResponse.java`, `web/response/PublicUniversityResponse.java`
- Test: `nadoumi-modules/nadoumi-university/src/test/java/com/nadoumi/university/service/UniversityServiceTest.java` (extend), and a slice for the URL resolution.

**Interfaces:**
- Consumes: `MediaGateway` (`nadoumi-common`).
- Produces:
  - `POST /api/staff/universities/{id}/logo` / `/banner` — `multipart file` → `MediaUploadResult{mediaId, url}` (`nad:university:edit`, `@Log`). Sets `logo_media_id` / `banner_media_id` on the row.
  - `POST /api/staff/universities/{id}/gallery` — `multipart file` → append a gallery row with `media_id` (respects the ≤ 6 cap).
  - `UniversityRequest` gains `Long logoMediaId`, `Long bannerMediaId`; `GalleryInput` gains `Long mediaId` (and keeps `imageUrl` optional for the deprecation window). `logoImageUrl` / `coverImageUrl` request fields **removed**.
  - `UniversityResponse` / `PublicUniversityResponse` gain `String logoUrl`, `String bannerUrl`, and `gallery[].url` — resolved: `mediaId != null ? mediaGateway.publicUrl(mediaId) : legacyImageUrl`.

- [ ] **Step 1: Write/extend the failing service test**

- `createResolvesLogoUrlFromMediaId` — given a `MediaGateway` stub returning `https://res.cloudinary.com/x/logo.png` for id 7, `create(req with logoMediaId=7)` → `get(id).logoUrl()` equals that URL.
- `legacyImageUrlStillRenders` — a row with `logo_media_id=null` but `logo_image_url='/profile/…'` → `logoUrl()` returns the legacy value (fallback path).
- `galleryCapStillSix` — 7 gallery inputs → `NadBadRequestException` (unchanged behaviour).

- [ ] **Step 2: Migration-aligned mapper + domain changes**

Add `logoMediaId` / `bannerMediaId` to `University`, `mediaId` to `UniversityGalleryImage`; update the `<resultMap>` and `cols` and `insert`/`update` in `UniversityMapper.xml`; add `updateLogoMediaId` / `updateBannerMediaId` / `insertGalleryImageWithMedia` statements (or extend the existing ones).

- [ ] **Step 3: Service + controller**

`UniversityService.setLogo(long id, long mediaId)` etc.; URL resolution in `UniversityResponse.of(...)` needs the gateway — either pass resolved strings into `of(...)` from the service, or give the service a `toResponse(University)` that injects URLs. Prefer the latter (keeps `of` pure). Add the three upload endpoints to `StaffUniversityController`, each: `service.uploadLogo(id, file)` → `mediaGateway.upload(file, UNIVERSITY_LOGO, null, new MediaOwnerRef(UNIVERSITY, id))` → `service.setLogo(id, asset.id())` → return `{asset.id(), asset.secureUrl()}`.

- [ ] **Step 4: Build + test**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-common,nadoumi-modules/nadoumi-media install -DskipTests
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -q -pl nadoumi-modules/nadoumi-university test
```

- [ ] **Step 5: Install + commit**

`feat(university): logo/banner/gallery uploads via MediaGateway; *_media_id + resolved URLs`.

---

## Task 15: Scholarship retrofit

**Files:**
- Modify: `nadoumi-scholarship` — `domain/Scholarship.java` (+`heroMediaId`,`coverMediaId`), mapper + XML, `service/ScholarshipAdminService.java` + `ScholarshipService.java` (public read resolves URLs), `web/StaffScholarshipController.java` (+ `/hero` `/cover` uploads), `web/request/ScholarshipRequest.java` (`heroMediaId`,`coverMediaId`; drop `heroImageUrl`/`coverImageUrl` request fields), `web/response/{ScholarshipView,PublicScholarshipResponse}.java`.
- Test: `nadoumi-modules/nadoumi-scholarship/src/test/java/.../ScholarshipServiceTest.java` (extend).

**Interfaces:**
- Produces: `POST /api/staff/scholarships/{id}/hero` / `/cover` (`nad:scholarship:edit`); `heroUrl` / `coverUrl` on both the staff view and `PublicScholarshipResponse`, resolved from `*_media_id` via `MediaGateway`, legacy `*_image_url` fallback. The `v_scholarship_student` read path (public) resolves `hero_media_id` / `cover_media_id` — **still no confidential column touched**.

- [ ] **Step 1: Failing test** — public detail of a PUBLISHED+ACTIVE scholarship with `hero_media_id=9` → `heroUrl` is the gateway's URL; a leak assertion (no `universityId` / `partnership` / `commission` field) still holds.
- [ ] **Step 2–3: Implement** mirroring Task 14.
- [ ] **Step 4: Build + test** (`install` common+media first).
- [ ] **Step 5: Commit** — `feat(scholarship): hero/cover uploads via MediaGateway; media_id + resolved URLs`.

---

## Task 16: Program image + Applicant photo (PROTECTED)

**Files:**
- Modify: `nadoumi-program` — `domain/Program.java` (+`imageMediaId`), mapper + XML, service, `web/StaffProgramController.java` (+ `/image` upload), request/response (`imageMediaId`, `imageUrl`).
- Modify: `nadoumi-applicant` — `domain/Applicant.java` (+`photoMediaId`), mapper + XML, `service/ApplicantService.java`, `web/StaffApplicantController.java` (+ `POST /{id}/photo` upload, `GET /{id}/photo` → 302 signed URL), request/response.
- Test: `ProgramServiceTest`, `ApplicantServiceTest` (extend).

**Interfaces:**
- Produces:
  - Program: `POST /api/staff/programs/{id}/image` (`nad:program:edit`); `imageUrl` on `ProgramResponse` / `PublicProgramResponse`.
  - Applicant photo: `POST /api/staff/applicants/{id}/photo` (`nad:applicant:edit` — and a student route `POST /api/student/applicants/{id}/photo` gated by `EDIT_PROFILE` capability); category `APPLICANT_PHOTO`, access class **PROTECTED**. `GET .../{id}/photo` — checks `VIEW_PROFILE` for the applicant → `mediaGateway.issueSignedUrl(photoMediaId, ctx)` → `302` (or `{url,expiresAt}` with `?json=1`). No photo URL in any list/detail JSON body.

- [ ] **Step 1: Failing tests**
  - `programImageResolvesUrl` (mirror Task 14).
  - `applicantPhotoUploadStoresProtectedAsset` — after upload, `nad_media_asset` row `access_class='PROTECTED'`, `nad_applicant.photo_media_id` set, upload response has **no** `url`.
  - `applicantPhotoDownloadRequiresViewProfile` — owner/grant-holder → `302` + a `nad_media_access_log` `GRANTED` row; unrelated user → `403` + `DENIED` row.
- [ ] **Step 2–3: Implement.**
- [ ] **Step 4: Build + module tests.**
- [ ] **Step 5: Commit** — `feat(catalog): program image + applicant photo (PROTECTED) via MediaGateway`.

---

## Task 17: `ruoyi-admin` ITs — media authorization, audit, retrofit

**Files:**
- Create: `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/MediaAccessAuthorizationTest.java`, `MediaAccessLogTest.java`, `CatalogImageRetrofitTest.java`
- Modify: `StaffUniversityTest.java`, `StaffScholarshipTest.java`, `StaffProgramTest.java` — swap URL-field assertions for `*Url` (resolved) + assert an upload round-trips.

**Interfaces:**
- Consumes: `FakeMediaStorage` (deterministic URLs), `mvc`, `jdbc`, `createStaff` / `createStudent` / tokens.

- [ ] **Step 1: `CatalogImageRetrofitTest`**
  - `universityLogoUpload` — `multipart POST /api/staff/universities/{id}/logo` (a 1×1 PNG part) → `201`/`200` `{mediaId, url}`; `GET /{id}` → `logoUrl` == the returned url; a `nad_media_asset` row `category='UNIVERSITY_LOGO'`, `access_class='PUBLIC'`, `owner_kind='UNIVERSITY'`.
  - `scholarshipHeroUpload` + `publicDetailShowsHeroUrl` + `studentViewStillNoConfidentialField`.
  - `oversizeLogoRejected` — a part claiming 5 MB for a 4 MB-capped category → `413`.
  - `htmlDisguisedAsPngRejected` — `%3Chtml%3E` bytes declared `image/png` → `422`.
- [ ] **Step 2: `MediaAccessAuthorizationTest`**
  - `publicMediaServedToAnonymous` — `GET /api/media/{id}` for a PUBLIC asset → `302` to the fake public URL.
  - `protectedMediaNeverViaPublicRoute` — `GET /api/media/{id}` for a PROTECTED applicant photo → `404`.
  - `applicantPhotoOwnerGetsSignedUrl` / `otherApplicantForbidden` (`403` + `DENIED` log).
  - `revokedGrantLosesAccessImmediately` — grant `VIEW_PROFILE`, download OK; revoke; next download `403`.
- [ ] **Step 3: `MediaAccessLogTest`**
  - every `GET .../photo` (grant + deny) writes exactly one `nad_media_access_log` row with the right `result` / `access_kind` / `ttl_seconds`.
- [ ] **Step 4: Run** `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -pl ruoyi-admin verify` (background) — all green.
- [ ] **Step 5: Commit** — `test(media): IT coverage for upload validation, delivery authz, access audit, retrofit`.

---

## Task 18: `nadoumi-admin` — `ImageUpload` → module endpoints; drawers/detail

**Files:**
- Modify: `src/components/ui/ImageUpload.vue`, `src/utils/asset.ts`, `src/api/{university,scholarship,program,applicant}.ts`, `src/views/universities/{UniversityDrawer,detail}.vue`, `src/views/scholarships/{ScholarshipDrawer,detail}.vue`, `src/views/programs/ProgramDrawer.vue`, `src/views/applicants/*` (photo), `src/lang/en.ts`
- Test: `nadoumi-admin/tests/unit/pages/UniversityDrawer.test.ts`, `Scholarships.test.ts`, `Programs.test.ts` (fixtures + upload flow), a new `ImageUpload.test.ts`.

**Interfaces:**
- `ImageUpload.vue` props gain `action: string` (the module upload URL, e.g. `/staff/universities/12/logo`) and `field: 'mediaId'`. `beforeUpload` keeps the client-side type/size pre-check (defence in depth) but the server is authoritative. `onSuccess(res)` reads `{ code, data: { mediaId, url } }` (RuoYi `AjaxResult` envelope — confirm the module controllers return raw records vs the `AjaxResult` wrapper; Nadoumi controllers return raw, so `res` is `{ mediaId, url }`). Emits `update:modelValue` with `mediaId` (number) and exposes the preview `url`.
- `assetUrl(ref)` — now: absolute `https?:` / `data:` → return as-is; anything else → return as-is too (no more `/dev-api` prefixing). Keep the function as a one-liner shim; delete in a later cleanup.

- [ ] **Step 1: Rewrite `ImageUpload.vue`** — `:action` from the parent, `:headers` unchanged, `name="file"`, emit `mediaId`; preview from the returned `url` held in local state.
- [ ] **Step 2: API types** — `UniversityInput.logoMediaId?: number` etc.; `University.logoUrl: string` etc.; drop `logoImageUrl`.
- [ ] **Step 3: Drawers/detail** — pass `:action="`/staff/universities/${id}/logo`"`; bind `v-model` to `form.logoMediaId`; render previews/detail from `x.logoUrl`. For **create** (no id yet): disable image upload until the entity is saved once, with a hint (`t('imageUpload.saveFirst')`) — matches the fact that uploads are `POST /{id}/…`.
- [ ] **Step 4: i18n** — `imageUpload.saveFirst`, adjust `imageUpload.hint`. `zh.ts` auto-covers (derived).
- [ ] **Step 5: Lint + typecheck + unit**

```bash
cd nadoumi-admin && pnpm exec eslint --fix "src/**/*.{vue,ts}" >/dev/null 2>&1; pnpm run lint && pnpm run build && pnpm run test
```
- [ ] **Step 6: Commit** — `feat(admin): image uploads post to module media endpoints; render resolved URLs`.

---

## Task 19: `nadoumi-web` — drop the interim `/media` proxy; consume Cloudinary URLs

**Files:**
- Delete: `app/utils/media.ts`, `server/routes/media/[...path].ts`
- Modify: `app/types/catalog.ts`, `app/components/marketing/{UniversityCard,ScholarshipCard,ProgramCard}.vue`, `app/pages/{index,universities/[slug],scholarships/[slug],programs/[slug]}.vue`, `nuxt.config.ts`
- Test: `tests/unit/**` fixtures + `tests/unit/pages/{universities-list,university-detail,scholarship-detail,program-detail,index}.test.ts`

**Interfaces:**
- `UniversitySummary.logoUrl` / `coverUrl` (absolute), `gallery[].url`; `ScholarshipCard.heroUrl` / `coverUrl`; `ProgramCard.imageUrl` — all plain absolute strings now. Drop `mediaUrl()` calls; use the field directly. `<NuxtImg>` `src` is the absolute Cloudinary URL; `@nuxt/image` `provider: 'cloudinary'` with `cloudinary.baseURL` set, **or** add `res.cloudinary.com` to `image.domains` and keep `provider: 'none'` for these.

- [ ] **Step 1: Delete the two files**; `grep -rn "mediaUrl\|/media/" app server` → no hits.
- [ ] **Step 2: Types + components + pages** — swap `mediaUrl(x.logoImageUrl)` → `x.logoUrl`.
- [ ] **Step 3: `nuxt.config.ts`** — `image.domains: ['res.cloudinary.com', ...existing]` (unsplash stays for placeholder art).
- [ ] **Step 4: Fixtures** — replace `/profile/...` / `/media/...` test strings with `https://res.cloudinary.com/demo/image/upload/v1/x.jpg`.
- [ ] **Step 5: Typecheck + test**

```bash
cd nadoumi-web && pnpm run typecheck && pnpm run test
```
- [ ] **Step 6: Commit** — `refactor(web): consume absolute Cloudinary URLs; remove interim /media proxy`.

---

## Task 20: Documentation

**Files:**
- Modify: `docs/ARCHITECTURE.md`, `docs/DEPLOYMENT.md`, `docs/DATABASE_DESIGN.md`, `docs/DOCUMENT_MANAGEMENT.md`, `docs/API_DESIGN.md`, `docs/SECURITY.md`, `docs/FRONTEND_ARCHITECTURE.md`, `docs/ADMIN_ARCHITECTURE.md`, `docs/PLATFORM_ARCHITECTURE.md`

**Interfaces:** — the exact content is enumerated in spec §I.11 plus:
- `PLATFORM_ARCHITECTURE.md §8` — add a row noting the media layer (Cloudinary) landed ahead of Step 7; Step 7 now "consumes `nadoumi-media`".
- `FRONTEND_ARCHITECTURE.md` / `ADMIN_ARCHITECTURE.md` — the `ImageUpload` change, removal of the `/media` proxy, `MediaUpload`/`assetUrl` shim status.
- Migration ledger in `DATABASE_DESIGN.md` — V26–V29 (note the final P1 count 26 / 25 after Task 12's `sys_job` seed).

- [ ] **Step 1:** apply each doc's changes; `EXISTING` / `PROPOSED` / `DECISION` labels where the doc uses them; D5 row → "superseded by DM1".
- [ ] **Step 2:** re-read each edited section for accuracy against what P1 actually built.
- [ ] **Step 3: Commit** — `docs: Cloudinary media layer — storage, security, DB, API, deployment`.

---

## Task 21: Full-suite gate + spec sync

- [ ] **Step 1:** `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -o -pl ruoyi-admin -am verify` (background) — everything green, final `FlywayMigrationsIT` 26 / 25.
- [ ] **Step 2:** `cd nadoumi-admin && pnpm run lint && pnpm run build && pnpm run test`; `cd ../nadoumi-web && pnpm run typecheck && pnpm run test`.
- [ ] **Step 3:** update `docs/superpowers/specs/2026-09-03-…-design.md` §4 migration numbers if Task 12's seed shifted them (V26–V29 for P1; P2 starts at V30).
- [ ] **Step 4:** update the `phase-status.md` memory: P1 done, Cloudinary media layer, `nadoumi-media` module, V26–V29.
- [ ] **Step 5: Commit** — `chore(media): P1 gate green — full verify + frontend suites`.

---

## Self-review (done at authoring time)

- **Spec coverage:** SPI (T1), module (T2), `nad_media_asset` + `nad_media_access_log` (T3), mappers (T4), policy/limits (T5), validation + magic bytes (T6), fail-fast config (T7), Cloudinary impl + fake (T8), façade + audit + `/api/media/{id}` (T9), catalog FKs + view recreate (T10), applicant photo column (T11), reconciliation (T12), IT base (T13), university/scholarship/program/applicant retrofit (T14–T16), authz + audit + retrofit ITs (T17), admin UI (T18), web (T19), docs ×8 (T20), gate (T21). Every spec §I item maps to a task.
- **Placeholder scan:** none — each step has concrete SQL/Java/commands. Two deliberate *decisions embedded in steps* (T6 exception mapping, T13 conditional-bean approach, T14 `MediaGateway`-in-common) are resolved inline with a "Chosen:" note and feed back into T1/T7/T9.
- **Type consistency:** `MediaGateway` (common) vs `MediaService` (media, `implements MediaGateway`) — T1/T9/T14 aligned. `MediaUploadResult{mediaId,url}` used by T9/T14–T16/T18. `nad_media_access_log` column names (`access_kind`, `result`, `deny_reason`, `ttl_seconds`) consistent T3/T9/T17. Migration running totals: V26→23/22, V27→24/23, V28→25/24, V29(seed)→26/25 — consistent across T3/T10/T11/T12/T21 and flagged for the spec in T21.

---

## Execution handoff

Plan saved to `docs/superpowers/plans/2026-09-03-p1-media-storage-cloudinary.md`. Two execution options:

1. **Subagent-Driven (recommended)** — a fresh subagent per task, review between tasks, fast iteration. REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`.
2. **Inline Execution** — tasks in this session via `superpowers:executing-plans`, batched with checkpoints.

P2 (application engine backend), P3 (public Apply Now + student portal) and P4 (admin workbench) plans are written next as separate plan docs.
