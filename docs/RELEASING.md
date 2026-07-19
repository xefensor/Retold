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
- NeoForge, Minecraft 26.2, Java 25, and client-and-server metadata

Versions containing `-alpha` or `-snapshot` publish as alpha. Versions containing `-beta` or `-rc` publish as beta. Other versions publish as releases.

## Manual Recovery

The workflow can also be started from **Actions → Release → Run workflow**. Enter the version without the `v` prefix. The entered version must still match `mod_version`, and every normal validation and publishing step still runs.

Use manual publishing only when the release tag workflow did not start. If a run partially uploads to an external platform, inspect all three platforms before retrying so the retry does not create duplicate versions.
