"use client";

import { useActionState } from "react";
import Link from "next/link";
import { signIn } from "@/app/auth/actions";

/**
 * Sign-in page — uses the signIn server action for authentication.
 *
 * Server actions set cookies via the server client (same mechanism the proxy reads),
 * ensuring the session is visible to proxy.ts on redirect. The browser client
 * (`createBrowserClient`) sets cookies that can be out of sync with the server.
 */
export default function SignInPage() {
  const [state, formAction, pending] = useActionState(signIn, undefined);

  return (
    <div className="w-full max-w-sm">
      <h2 className="mb-6 text-center text-xl font-semibold" style={{ color: "var(--text-primary)" }}>
        Sign in to your account
      </h2>

      <form action={formAction} className="flex flex-col gap-4">
        {/* Email */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="email"
            className="text-sm font-medium"
            style={{ color: "var(--text-secondary)" }}
          >
            Email
          </label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            placeholder="you@example.com"
            className="rounded-sm border px-3 py-2 text-sm outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-outline)",
              color: "var(--text-primary)",
            }}
          />
        </div>

        {/* Password */}
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="password"
            className="text-sm font-medium"
            style={{ color: "var(--text-secondary)" }}
          >
            Password
          </label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            placeholder="••••••••"
            className="rounded-sm border px-3 py-2 text-sm outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-outline)",
              color: "var(--text-primary)",
            }}
          />
        </div>

        {/* Error message */}
        {state?.error && (
          <p className="rounded-xs px-3 py-2 text-sm text-error-red" style={{ backgroundColor: "#FFB4AB20" }}>
            {state.error}
          </p>
        )}

        {/* Submit */}
        <button
          type="submit"
          disabled={pending}
          className="mt-2 rounded-sm bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {pending ? "Signing in..." : "Sign In"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm" style={{ color: "var(--text-secondary)" }}>
        Don&apos;t have an account?{" "}
        <Link href="/auth/sign-up" className="font-medium text-primary hover:underline">
          Sign Up
        </Link>
      </p>
    </div>
  );
}
