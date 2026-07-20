# Releasing Retold

Retold releases are published from one GitHub Actions workflow to GitHub Releases, Modrinth, and CurseForge.

The workflow validates the version and changelog, builds the release JAR, runs unit tests and NeoForge GameTests, creates a SHA-256 checksum, and publishes the same version metadata and release notes to all three platforms.

## One-Time GitHub Setup

Open **Settings → Secrets and variables → Actions** in the GitHub repository.

Add these repository **variables**:

| Name | Value |
| --- | --- |
| `MODRINTH_PROJECT_ID` | The stable Modrinth project ID for Retold |
| `CURSEFORGE_PROJECT_ID` | The numeric CurseForge project ID for Retold |

Add these repository **secrets**:

| Name | Value |
| --- | --- |
| `MODRINTH_TOKEN` | A Modrinth personal access token permitted to create versions |
| `CURSEFORGE_TOKEN` | A CurseForge Authors API token permitted to upload files |

Never commit either token to the repository, paste it into an issue, or include it in workflow logs.

Modrinth recommends storing the stable project ID rather than the editable slug. The ID can be read from the project settings or from the public project API. The CurseForge project ID is shown in the project's author/dashboard information.

## Backfill An Existing GitHub Release

Use the separate **Backfill Existing Release** workflow only when a version already exists on GitHub Releases but is missing from Modrinth, CurseForge, or both. Do not use the normal **Release** workflow for this situation because it intentionally refuses to recreate an existing GitHub release.

1. Confirm the existing GitHub release has a `retold-<version>.jar` asset.
2. Open **Actions → Backfill Existing Release → Run workflow**.
3. Leave the branch set to `master`.
4. Enter the version without the `v` prefix, for example `0.2.1`.
5. Enable only the destinations that do not already contain that version.
6. Click **Run workflow** and open the resulting run to follow each step.

The backfill workflow:

- requires the matching `v<version>` tag and GitHub release
- downloads the exact existing GitHub release JAR instead of rebuilding current source
- verifies the GitHub checksum when a checksum asset exists
- reads release notes from the tagged version of `CHANGELOG.md`
- publishes independently to Modrinth and CurseForge
- renders the CurseForge changelog as HTML after upload so headings, lists, and inline code remain intact
- never creates or modifies a GitHub release

If one destination succeeds and the other fails, rerun the workflow with only the failed destination enabled. This avoids attempting to create a duplicate version on the successful platform.

This process is for historical or missed uploads. New releases should always use the tagged automatic process below.

## Prepare A Release

1. Update `mod_version` in `gradle.properties`.
2. Replace the matching content under `## Next - Unreleased` in `CHANGELOG.md` with a dated heading in this exact form:

   ```markdown
   ## 0.3.0 - 2026-08-01
   ```

3. Restore the empty `Next - Unreleased` section for future work.
4. Merge the release preparation changes into `master`.
5. Confirm the normal CI workflow is green.
6. Complete the required manual in-game, multiplayer, and existing-world checks described in `AGENTS.md`.

## Publish Automatically

Create and push an annotated tag that exactly matches `mod_version`:

```bash
git switch master
git pull --ff-only
git tag -a v0.3.0 -m "Retold 0.3.0"
git push origin v0.3.0
```

Pushing the tag starts `.github/workflows/release.yml`. The workflow refuses to publish when:

- the tag and `mod_version` disagree
- the matching changelog section is missing or empty
- a GitHub release for the version already exists
- required project IDs or API tokens are missing
- the build, unit tests, PMD, or GameTests fail
- the expected release JAR is missing

When all checks pass, the workflow publishes:

- `retold-<version>.jar` to Modrinth and CurseForge
- the same JAR plus its `.sha256` checksum to GitHub Releases
- the matching `CHANGELOG.md` section as the changelog on every platform
- an HTML-rendered copy of those notes on CurseForge to preserve headings, lists, and inline code
- NeoForge, Minecraft 26.2, Java 25, and client-and-server metadata

Versions containing `-alpha` or `-snapshot` publish as alpha. Versions containing `-beta` or `-rc` publish as beta. Other versions publish as releases.

## Manual Recovery

The workflow can also be started from **Actions → Release → Run workflow**. Enter the version without the `v` prefix. The entered version must still match `mod_version`, and every normal validation and publishing step still runs.

Use manual publishing only when the release tag workflow did not start. If a run partially publishes, first inspect GitHub, Modrinth, and CurseForge. Either upload the missing platform manually, or delete every version and GitHub release created by the failed run before retrying. The existing Git tag can remain in place.

## Repair A CurseForge Changelog

Use **Actions → Repair CurseForge Changelog → Run workflow** when a CurseForge file already exists but its changelog was flattened or otherwise formatted incorrectly.

1. Open the affected file in the CurseForge author dashboard.
2. Copy the numeric file ID from the file page URL.
3. Open **Actions → Repair CurseForge Changelog → Run workflow** in GitHub.
4. Enter the version without the `v` prefix and the numeric CurseForge file ID.
5. Run the workflow.

The repair workflow reads the matching release section from the tagged `CHANGELOG.md`, renders it to HTML with GitHub's Markdown renderer, and updates only that existing CurseForge file. It does not upload another JAR or modify GitHub or Modrinth.

New normal and backfilled releases perform this HTML update automatically after the CurseForge upload.
