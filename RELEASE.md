# Release Flow

## Prerequisites

- All features are merged into `develop` and tested
- `mvnd` (Maven Daemon) is installed

## Steps

### 1. Switch to `main` & merge `develop`

```bash
git checkout main
git merge develop
```

### 2. Set release version (no SNAPSHOT)

```bash
mvnd versions:set -DnewVersion=1.2.3 -DgenerateBackupPoms=false
```

### 3. Commit, tag & push

```bash
git add pom.xml
git commit -m "chore: release version 1.2.3"
git tag v1.2.3
git push origin main
git push origin v1.2.3
```

> **Note:** The tag push triggers the GitHub Actions release pipeline, which builds and pushes the Docker image to GHCR.

### 4. Switch back to `develop`

```bash
git checkout develop
```

### 5. Merge `main` back

```bash
git merge main
```

### 6. Set next SNAPSHOT version

```bash
mvnd versions:set -DnewVersion=1.2.4-SNAPSHOT -DgenerateBackupPoms=false
```

### 7. Commit & push

```bash
git add pom.xml
git commit -m "chore: bump version to 1.2.4-SNAPSHOT"
git push origin develop
```
