# NutriAI — AI-Powered Cross-Platform Nutrition Tracking & Planning

NutriAI is a comprehensive nutrition tracking, recipe building, and meal planning platform. It consists of a local-first **Android Application**, an online-first **Next.js Web Companion**, and a shared serverless **Supabase Backend** hosting database schema migrations and AI business logic.

---

## 🎯 Repository Overview & Architecture

NutriAI centralizes its core calculations and AI operations in serverless edge functions to prevent behavioral discrepancies (logic drift) between client platforms. The repository is structured as follows:

```
NutriAI/
├── app/                     # Android Application (Kotlin, Jetpack Compose, Room, Hilt)
├── webapp/                  # Web Companion App (Next.js 14, TypeScript, Tailwind CSS, Zustand, TanStack Query)
├── supabase/                # Shared Backend (PostgreSQL, migrations, RLS policies, Deno Edge Functions)
├── docs/                    # Technical architecture plans and specifications
└── gradle/                  # Android Gradle build wrapper version catalogs
```

---

## 📂 Directories & Key Files

### Project Modules
* **[`app/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app)**: Android native codebase structured with Clean Architecture (`domain`, `data`, and `presentation` layers).
* **[`webapp/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp)**: Next.js frontend companion providing logging, charts, settings, and scanning dashboards in the browser.
* **[`supabase/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase)**: Houses local configuration, database SQL schemas, and serverless edge functions.

### Reference & Log Files
* **[`APP_DEVLOG.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/APP_DEVLOG.md)**: Detailed phase-by-phase development history, decisions log, and bug tracker for the Android app.
* **[`WEBAPP_DEVLOG.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/WEBAPP_DEVLOG.md)**: Detailed phase-by-phase development history, decisions log, and bug tracker for the Next.js web app.
* **[`AGENTS.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/AGENTS.md)**: Guidelines and operational instructions for AI developer agents exploring or editing this codebase.
* **[`docs/WEB_ARCHITECTURE_PLAN.md`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/docs/WEB_ARCHITECTURE_PLAN.md)**: Design document reviewing integration structures and database sync schemas.

---

## 🔌 System Requirements & Dependencies

To compile and run the components of the ecosystem locally:

### 1. Android Development
* **Java Development Kit (JDK 17)**
* **Android Studio** (or Gradle build commands)

### 2. Web Companion Development
* **Node.js 20+** (or Bun runtime engine)

### 3. Backend Database & Services
* **Supabase CLI** (for managing local database containers and deployments)
* **Docker Desktop** (required locally by the Supabase CLI to host local database services, Kong gateways, and Deno edge wrappers)
