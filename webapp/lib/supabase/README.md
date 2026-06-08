# Supabase Client Configurations (`webapp/lib/supabase/`)

This directory contains utility functions to instantiate Supabase client instances for both client-side and server-side contexts.

## 🎯 Major Function & Purpose

These configuration files bridge the Next.js framework with Supabase backend services. They handle user authentication, database queries, and remote edge function calls. Due to Next.js App Router architecture, separate client instances are instantiated depending on whether code runs in the browser, a Server Component, a Server Action, or an API Route Handler.

---

## 📂 Files

* **[`client.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/supabase/client.ts)**: Configures and exports a Supabase client for Client Components executing in the browser.
* **[`server.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/supabase/server.ts)**: Configures and exports a Supabase client for server-side environments (Server Components, Server Actions, Route Handlers). Automatically parses and coordinates authentication cookies via Next.js.

---

## 🔌 External Dependencies

* **`@supabase/supabase-js`**: Core Supabase client library.
* **`@supabase/ssr`**: Server-side rendering helper library for managing cookie-based user sessions.
* **`next/headers`**: Native Next.js headers/cookies server utilities.
