---
name: parity-check
description: Audits the codebase to verify logic, database calculation, and user interface parity between the Android application and the Web companion. Use when checking for cross-platform behavior drift.
license: Apache-2.0
compatibility: Requires grep-search capability and codebase access
metadata:
  author: NutriAI-Dev
  version: "2.0"
---

# Parity Check Playbook

This skill outlines a generalized, procedural methodology for auditing and reconciling business logic, database operations, and user interface flows between the Android application (`/app`) and the Next.js Web companion (`/webapp`).

Use this playbook to trace, compare, and verify parity for **any feature or logic block**, ensuring alignment and preventing behavior drift across clients.

---

## 🎯 Procedural Audit Steps

When this skill is invoked to audit a specific feature or module, execute the following dynamic tracing and comparison procedures:

### Step 1: Locate Target Source Files
Search the codebase on both platforms to isolate all source files, schemas, and assets implementing the feature:
1. **Identify the Android Code Path:**
   * Locate the Domain components ([`domain/model/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model), [`domain/repository/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository), and [`domain/usecase/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase)).
   * Locate the Data implementations ([`data/local/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data/local) for Room caching, [`data/remote/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data/remote) for networking, and repository implementations).
   * Locate UI screens and ViewModels in [`presentation/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation).
2. **Identify the Web Code Path:**
   * Locate matching type schemas in [`lib/types/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types).
   * Locate state stores in [`lib/stores/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/stores) and fetching/mutation hooks in [`lib/hooks/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks).
   * Locate UI components in [`components/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components) and route layouts in [`app/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app).
3. **Identify Backend Dependencies:** Check if the feature invokes database tables, triggers, or centralized Deno Edge Functions in [`supabase/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase).

### Step 2: Compare Data Schemas & Contracts
* Compare Android Kotlin domain data models against Web TypeScript definitions.
* Verify property names, data types, nullability boundaries, collection formats, and default value initializations.
* Trace translation helpers (e.g., database row-to-domain parsers) to ensure data is normalized consistently on both clients.

### Step 3: Trace and Reconcile Business Logic
* **Centralized Logic:** Check if the core rules, filters, or algorithms are delegated to a shared Supabase Edge Function. If so, verify that both platforms invoke the edge function identically and handle its requests and payloads similarly.
* **Client-Side Logic:** For features, algorithms, or logic executing locally on the clients:
  * Trace the step-by-step algorithms, conditional branches, default fallbacks, and validation limits on both platforms.
  * Reconcile all calculations, data transformations, filtering rules, and business validations to ensure identical outcomes.


### Step 4: Audit UI Presentation and Event Flows
* Compare visual structures, dashboards indicators, input panels, and dialog flows.
* Compare color styles and typography parameters (mapping Android Compose M3 colors/fonts to Tailwind CSS v4 design variables).
* Reconcile user flows (e.g., form input states, error notifications, validation barriers, confirmation triggers) to ensure consistent user feedback.

### Step 5: Verify Storage Cache & Sync Protocols
* Note that the Web application operates online-first (caching via TanStack Query), while the Android app is offline-first (caching in Room and queuing updates via WorkManager sync routines).
* Ensure web actions write values matching the synchronizer constraints, indexes, timestamps, and row-level security (RLS) scopes expected by the Android database synchronization engine.

---

## 📝 Expected Output Report

Generate a Markdown Parity Report detailing:
1. **Audited Scope/Feature:** Name and brief summary of the audited logic or design module.
2. **File Mapping:** List of compared source files on both platforms.
3. **Parity Assessment Summary:** A clear statement on whether the app and webapp are already at parity for the audited scope.
4. **Identified Discrepancies:** Detailed list of any differences in styling, schema parameters, business logic algorithms, or user flows.
5. **Reconciliation Necessity & Action Plan:** 
   * A determination of whether each identified discrepancy must be brought to parity, along with the reasoning/justification (e.g., framework-specific behaviors vs. actual logic drift).
   * Concrete action steps or edits required to resolve the necessary gaps.
   * Assessment of whether client-side logic should be migrated into a centralized Supabase Edge Function to prevent future drift.

