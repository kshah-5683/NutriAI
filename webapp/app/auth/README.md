# Authentication Route Segment (`webapp/app/auth/`)

This directory coordinates the user authentication pages, registration flows, and redirect callbacks for the web application.

## 🎯 Major Function & Purpose

The `auth` folder manages the application's secure entry gates. It hosts the sign-in and sign-up interfaces, registers callback routes to process verification links, and exposes Server Actions that authenticate sessions and set secure HTTP-only cookies via Supabase.

---

## 📂 Files & Nested Routes

* **[`actions.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/auth/actions.ts)**: Declares Next.js Server Actions (`login`, `signup`, and `signOut`) executed server-side to initiate user authentication sessions.
* **[`layout.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/auth/layout.tsx)**: Structural container displaying branding backgrounds and formatting for auth pages.
* **[`confirm/route.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/auth/confirm/route.ts)**: API Route Handler serving as the callback endpoint to verify sign-up email tokens and redirect users to the dashboard.
* **[`sign-in/page.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/auth/sign-in/page.tsx)**: Login page UI with form validation and loading states.
* **[`sign-up/page.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/app/auth/sign-up/page.tsx)**: Registration page UI including confirmation messages.

---

## 🔌 External Dependencies

* **`@supabase/supabase-js`** & **`@supabase/ssr`**: Backend auth endpoints integration and token/session validation.
* **`next/navigation`**: Client and server redirection hooks (`redirect`, `useRouter`).
* **`next/headers`**: Reads and writes cookies in server actions.
