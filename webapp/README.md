# NutriAI Web Companion Application (`webapp/`)

This directory houses the frontend web application of the NutriAI platform, built as a companion to the Android client.

## 🎯 Major Function & Purpose

The `webapp` is a Next.js 14 web application designed as an online-first companion to the core Android application. It allows users to log and parse food items via natural language processing, scan food labels using vision models, track calorie and macro target goals in a daily diary, and view interactive trend reports. It communicates directly with the shared PostgreSQL backend and invokes centralized Supabase Edge Functions for business logic validation.

---

## 📂 Project Structure

### Subdirectories
* **[`app/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app)**: Next.js App Router routing segments, layouts, and page views.
* **[`components/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components)**: Reusable React visual elements, layout wrappers, charts, and dashboard views.
* **[`lib/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib)**: State stores, React Hooks, Supabase setups, TypeScript types, and general utility functions.
* **[`public/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/public)**: Statically served vectors and logo graphics.

### Configuration & Tooling Files
* **[`next.config.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/next.config.ts)**: Configures Next.js parameters, bundler externals, and server settings.
* **[`instrumentation.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/instrumentation.ts)**: Configures proxy connections on start-up.
* **[`proxy.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/proxy.ts)**: Local developer utility supporting proxy requests.
* **[`package.json`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/package.json)**: Declares application metadata, package dependency definitions, and terminal run scripts.
* **[`tsconfig.json`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/tsconfig.json)**: Custom compiler configurations for TypeScript.
* **[`postcss.config.mjs`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/postcss.config.mjs)**: Layout styling parameters for PostCSS.
* **[`AGENTS.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/AGENTS.md)**: Workspace instruction overrides for developer agents working on web layouts.
* **[`CLAUDE.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/CLAUDE.md)**: System load directive referencing agent configurations.

---

## 🧑‍💻 Setup & Dev Commands

### 1. Install Dependencies
```bash
npm install
# or
bun install
```

### 2. Start Local Development Server
```bash
npm run dev
# runs on http://localhost:3000
```

### 3. Build Production Bundle
```bash
npm run build
```

---

## 🔌 Core Tech Stack & Dependencies

* **Framework**: Next.js 14 (App Router)
* **Styling**: Tailwind CSS v4
* **Query & Cache Engine**: TanStack Query v5
* **Client State Management**: Zustand
* **Interactive Charts**: Recharts
* **Backend Database Client**: Supabase Client SDK (`@supabase/supabase-js`, `@supabase/ssr`)
