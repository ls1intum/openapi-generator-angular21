# Releasing

This generator is published to **Maven Central** (the Sonatype Central Portal). Consumers — Artemis and
any other project — then resolve it from a plain `mavenCentral()` repository, with **no authentication**:

```kotlin
repositories { mavenCentral() }
dependencies { /* openapiGenerator | classpath */ "de.tum.cit.aet:openapi-generator-angular21:1.1.0" }
```

Publishing is automated by [`.github/workflows/build.yml`](.github/workflows/build.yml): pushing a tag
that starts with `v` (e.g. `v1.1.0`) runs the `publish` job, which uploads the GPG-signed artifacts to
the Central Portal via the [`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/)
plugin and (because `automaticRelease = true`) releases the deployment once it validates.

## One-time setup

These steps are done **once** by a maintainer/org admin; afterwards every release is just a tag push.

### 1. Claim the `de.tum.cit.aet` namespace on the Central Portal

1. Sign in at <https://central.sonatype.com> (GitHub or email).
2. Add the namespace `de.tum.cit.aet` and verify ownership of the `tum.de` domain by publishing the
   **DNS `TXT` record** the portal shows you (a one-off verification token).
   - This requires access to the `tum.de` DNS zone. If that is not available, the simplest alternative
     is to switch the `group` to `io.github.ls1intum` (GitHub-verified, no DNS needed) — but that
     changes the artifact coordinates everywhere, so the `tum.de` namespace is preferred.

### 2. Generate a Central Portal user token

In the Central Portal → *Account* → *Generate User Token*. This yields a username/password pair used
for the upload (it is **not** your login password).

### 3. Create a GPG signing key

```bash
gpg --gen-key                                   # create a key for the maintainer/CI identity
gpg --list-secret-keys --keyid-format=long      # note the key id
# Publish the public key so Central can verify signatures:
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
# Export the private key in the ASCII-armored form the plugin expects:
gpg --armor --export-secret-keys <KEY_ID>       # the whole block, including the BEGIN/END lines
```

### 4. Add four GitHub Actions repository secrets

`Settings → Secrets and variables → Actions → New repository secret`:

| Secret                   | Value                                                              |
|--------------------------|-------------------------------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user-token **username**                            |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user-token **password**                            |
| `SIGNING_KEY`            | the full ASCII-armored GPG **private** key from step 3            |
| `SIGNING_PASSWORD`       | the passphrase for that key                                       |

The workflow maps these onto the Gradle properties the plugin reads
(`ORG_GRADLE_PROJECT_mavenCentralUsername`, `…Password`, `…signingInMemoryKey`, `…signingInMemoryKeyPassword`).

## Cutting a release

1. Bump `version` in [`build.gradle.kts`](build.gradle.kts) (and the version references in
   [`README.md`](README.md)) to the new release version, e.g. `1.2.0`. Use a non-`SNAPSHOT` version.
2. Merge to `main`.
3. Tag and push:
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```
4. The `publish` job signs and uploads to the Central Portal and creates a GitHub Release. The artifact
   appears on Maven Central within ~15–30 minutes (search index can lag a few hours).

## Releasing locally (fallback)

If you ever need to publish without CI, provide the same four values as Gradle properties (e.g. in
`~/.gradle/gradle.properties`: `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`,
`signingInMemoryKeyPassword`) and run:

```bash
./gradlew publishToMavenCentral --no-configuration-cache
```

## Building without releasing

`./gradlew publishToMavenLocal` installs the artifacts into your local `~/.m2` repository and **requires
no signing key** (signing is gated on the key being present). This is the path downstream projects use to
build the generator from source when they want to regenerate their client before a version is on Central.
