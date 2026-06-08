---
name: update-logs-readme
description: Guides the agent on how to systematically maintain, format, and keep development logs (APP_DEVLOG.md, WEBAPP_DEVLOG.md) and folder READMEs updated to ensure documentation parity and integrity.
license: Apache-2.0
compatibility: Requires file writing and editing capability
metadata:
  author: NutriAI-Dev
  version: "1.0"
---

# Documentation & Logs Maintenance Playbook

This skill outlines a standardized, procedural methodology for maintaining, formatting, and updating development logs (`APP_DEVLOG.md` and `WEBAPP_DEVLOG.md`) and folder-level `README.md` files throughout the NutriAI codebase.

---

## 📝 1. Development Logs Maintenance

Development logs are the source of truth for the chronological history of changes, phase completions, and architecture decisions.

### 📅 When to Update
Every time you create, modify, or delete files, or complete a specific task or phase:
1. **Identify the target log:**
   * Android client / shared backend database changes ➔ [`APP_DEVLOG.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/APP_DEVLOG.md)
   * Web companion client changes ➔ [`WEBAPP_DEVLOG.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/WEBAPP_DEVLOG.md)
2. **Batch changes:** Group related changes together under a single logical Phase or Sub-phase entry (e.g., `Phase W20: Environment Configuration Setup Template`).

### 📐 Entry Structure & Format
Ensure every new log entry adheres to the following markdown template at the bottom of the log file:

```markdown
---

## Phase <ID>: <Title or Feature Name>

**Status:** <✅ Completed / ⚙️ In Progress>
**Date:** <Current Date, e.g., June 8, 2026>

### Summary
<2-3 sentences summarizing the purpose, motivation, and outcome of the phase.>

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `relative/path/to/file` | <Created/Updated/Deleted> | <Detailed, developer-facing explanation of the edits made to this file.> |

### Key Implementation Details
<Optional: Include any critical code snippets, configurations, or logic flow descriptions here.>

### Architecture Decisions Added
<Optional: Include any key architectural changes or design decisions made during this phase.>

| # | Decision | Rationale |
|---|----------|-----------|
| <ID> | <Decision Name/Description> | <Why this implementation choice was made.> |
```

---

## 📁 2. Folder-level READMEs Maintenance

To keep the codebase easy to navigate and self-documenting, each subdirectory or package directory must have a descriptive `README.md` file.

### 📅 When to Update or Create
* **Create a README:** Whenever a new directory/folder is introduced to hold source code, assets, or configs.
* **Update a README:** Whenever files are added, renamed, or deleted within an existing directory, or when a folder's external dependencies change.

### 📐 Required README Structure
Every folder-level `README.md` must contain the following sections:

1. **Folder Overview:** A high-level description of what the folder represents.
2. **File & Subdirectory Manifest:** A table or list describing each file or child folder inside it.
3. **Major Function / Purpose:** The primary role of the directory within the larger architecture.
4. **External Dependencies:** A list of third-party libraries, SDKs, or services this folder relies on (or "None" if none).

#### Template for a Folder README:
```markdown
# Directory Name (`path/to/directory`)

## 🎯 Overview
<High-level summary of the folder's role.>

## 📂 File & Directory Manifest
| File / Folder | Purpose |
|---------------|---------|
| `subfolder/`  | <Purpose of subfolder> |
| `file.ts`     | <Purpose of file.ts> |

## ⚙️ Major Function & Purpose
<Detailed description of the responsibilities and architecture role of this directory.>

## 🔌 External Dependencies
* <Dependency Name> (e.g., `@supabase/supabase-js`)
* None (if no external dependencies)
```

---

## 📋 3. Final Verification Checklist
Before completing your current turn or declaring a task finished, run through this verification checklist:
- [ ] Are all added or modified files documented in the appropriate dev log (`APP_DEVLOG.md` or `WEBAPP_DEVLOG.md`)?
- [ ] If any new directories were created, do they contain a compliant `README.md`?
- [ ] If any existing files were moved or added to a directory, has that directory's `README.md` been updated to list them?
- [ ] Do all file references in your log entries and READMEs use absolute/relative markdown links?
