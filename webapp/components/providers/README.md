# React Context Providers (`webapp/components/providers/`)

This directory contains React Context Providers that inject global runtime clients and state engines into the client component tree.

## 🎯 Major Function & Purpose

The `providers` directory coordinates the initializers for the application's core data connections. By wrapping the root layout in these provider containers, descendants in the component tree can instantly consume Supabase API clients and TanStack Query state managers via React Context.

---

## 📂 Provider Files

* **[`query-provider.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/providers/query-provider.tsx)**: Instantiates the TanStack `QueryClient` and exports the provider container enabling asynchronous caching, refetching, and query hooks.
* **[`supabase-provider.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/providers/supabase-provider.tsx)**: Creates the Supabase browser API client instance and distributes it via React Context (`SupabaseProvider` and `useSupabase()`) for client-side authentication and queries.

---

## 🔌 External Dependencies

* **`react`**: Core React Context engine (`createContext`, `useContext`, `useState`).
* **`@tanstack/react-query`**: Client side query client and provider context.
* **`@supabase/supabase-js`**: Supabase client connection bindings.
