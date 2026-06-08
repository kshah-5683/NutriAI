# Next.js App Router Root (`webapp/app/`)

This directory houses the root route configuration, page views, layouts, and global stylesheets defining the Next.js App Router structure.

## 🎯 Major Function & Purpose

The `app` folder configures the routing architecture of the web application. Using Next.js App Router conventions, each subfolder represents a URL route segment. It establishes global structures (like page headers, base fonts, and authentication contexts in layout templates), sets root redirection policies, and binds the global CSS design tokens.

---

## 📂 Subdirectories & Files

### Subfolders (Route Segments)
* **[`auth/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/auth)**: Routes managing login, registration forms, and callback email tokens.
* **[`catalog/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/catalog)**: Dashboard view to manage the user's custom catalog.
* **[`insights/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/insights)**: Trend line and bar charts tracking macro aggregates over time.
* **[`log/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/log)**: Primary daily tracker screen.
* **[`settings/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/settings)**: Preferences view coordinating goals, calorie budgets, and allergens.

### Root Files
* **[`layout.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/layout.tsx)**: The root layout wrapping the entire application in styling variables, fonts, client-side caching context providers, and dashboard structures.
* **[`page.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/page.tsx)**: Main route landing page redirecting users to `/log` or `/auth/sign-in` based on auth status.
* **[`globals.css`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/globals.css)**: Central stylesheet defining styling variables, Tailwind resets, and dark mode configuration properties.
* **[`favicon.ico`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/favicon.ico)**: Statically served icon asset displayed by browser tabs.

---

## 🔌 External Dependencies

* **`next`**: App Router framework and server components capabilities.
* **`react`**: Foundation component layouts.
* **Tailwind CSS**: Utility-first CSS variable and spacing systems.
