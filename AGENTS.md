# NutriAI — Developer Agent Guidelines

Welcome, Agent! This repository contains both the Android and Web client applications for **NutriAI**, a comprehensive nutrition tracking and planning platform powered by AI.

This file serves as a persistent context guide to help you navigate, understand, and write code for this project without violating its architectural principles.

---

## 📂 Project Structure

```
NutriAI/
├── app/                     # Android Application (Kotlin, Jetpack Compose)
│   └── src/main/java/...   # Clean Architecture structure (di, domain, data, presentation)
├── webapp/                  # Web Companion App (Next.js 14 App Router, TypeScript)
│   ├── app/                 # Next.js App Router views & layouts
│   ├── components/          # Reusable React components
│   └── lib/                 # Web-specific utilities and Supabase clients
├── supabase/                # Backend Configuration
│   ├── functions/           # Centralized Edge Functions (Deno / TypeScript)
│   └── migrations/          # PostgreSQL schema migrations and RLS policies
├── docs/                    # Technical documentation
│   └── WEB_ARCHITECTURE_PLAN.md
├── APP_DEVLOG.md            # Detailed history & tracker for the Android application
└── WEBAPP_DEVLOG.md         # Detailed history & tracker for the Web companion
```

---

## 🛠️ Technology Stacks & Conventions

### 1. Android Application (`/app`)
* **Framework:** Jetpack Compose (Material3 theming)
* **Architecture:** Clean Architecture (`Domain` ➔ `Data` ➔ `Presentation`)
  * **Domain:** Pure Kotlin. Contains models, repository interfaces, and use cases.
  * **Data:** Room Database, Retrofit API services, repository implementations.
  * **Presentation:** Compose screens, ViewModels (StateFlows for UI state).
* **Dependency Injection:** Hilt (`@HiltAndroidApp`, `@Inject`, `@Module`)
* **Local Storage:** Room Database (cached locally, offline-first design)
* **API Integration:** Direct interaction with Supabase and OpenFoodFacts/USDA/IFCT APIs (gradually migrating to Supabase Edge Functions in newer phases).

### 2. Web Companion (`/webapp`)
* **Framework:** Next.js 14 (App Router)
* **Styling:** Tailwind CSS v4 (using CSS variables for dark mode support)
* **State Management:** Zustand (for client-side UI states) and TanStack Query v5 (for cache management and data fetching)
* **Execution Model:** Online-first (no local DB/IndexedDB caching; directly queries Supabase with `TanStack Query`)
* **Special Webpack Configuration:** `undici` and `https-proxy-agent` use native node imports that Webpack cannot bundle. They must remain listed in `serverExternalPackages` inside `next.config.ts`.

### 3. Backend & Database (`/supabase`)
* **Database:** PostgreSQL on Supabase (using RLS policies for access control)
* **Business Logic:** Centralized in **Supabase Edge Functions** to prevent code/logic drift between Android and Web clients.
* **Database Recalculations:** Automatic triggers handle daily log adjustments when underlying food macro definitions are updated.

---

## 🧑‍💻 Commands & Developer Operations

### Android Development
* Build debug application: `./gradlew assembleDebug`
* Build debug application (Windows, using user-space JDK 17): `cmd /c "(set JAVA_HOME=C:\Users\<Username>\AppData\Local\Java\jdk17) && gradlew.bat assembleDebug"`
* Run tests: `./gradlew test`

### Web Development
* Install dependencies: `npm install` (or `bun install`)
* Start dev server: `npm run dev` (runs on `http://localhost:3000`)
* Build production bundle: `npm run build`

### Supabase Development
* Start local Supabase container: `supabase start`
* Deploy an edge function: `supabase functions deploy <function-name>`
* Create database migration: `supabase db diff -f <migration-name>`

---

## ⚠️ Key Guidelines for AI Agents

1. **Centralize Business Logic:** When introducing new algorithms, validation rules, or scaling logic, place them in a **Supabase Edge Function** (`/supabase/functions/`) rather than writing separate implementations in Kotlin and TypeScript.
2. **Type Safety:** Maintain strict TypeScript typing in `/webapp` and Deno functions. Avoid using `any`.
3. **Keep Dev Logs Updated (Conditional):** Document changes you make to the development logs **ONLY** when the `update-logs-readme` skill is explicitly called/requested. Otherwise, do not modify these files.
   * Android client changes go into [APP_DEVLOG.md](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/APP_DEVLOG.md).
   * Web client changes go into [WEBAPP_DEVLOG.md](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/WEBAPP_DEVLOG.md).
4. **Row-Level Security (RLS):** Ensure any schema migrations targeting user data have corresponding RLS policies restricting operations to the authenticated user owning the record (`auth.uid()`).
5. **Brand-Aware Lookup and Scaling:** Be extremely careful with macro scaling:
   * Maintain unit scaling (e.g., standardizing conversions between gram and non-gram boundaries).
   * Brand lookup should resolve to specific products if brand names are available.
6. **Folder Documentation (READMEs):** Each subdirectory/folder will have a `README.md` file that the agent should read before exploring the folder. The `README.md` must contain:
   * An overview of the folder.
   * A description of the files in the folder.
   * The major function/purpose of the folder.
   * A list of external dependencies (if any).

