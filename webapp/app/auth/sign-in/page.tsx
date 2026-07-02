"use client";

import { useActionState, useState } from "react";
import Link from "next/link";
import { signIn, signInWithGoogle } from "@/app/auth/actions";

/**
 * Sign-in page — uses the signIn server action for authentication.
 *
 * Server actions set cookies via the server client (same mechanism the proxy reads),
 * ensuring the session is visible to proxy.ts on redirect. The browser client
 * (`createBrowserClient`) sets cookies that can be out of sync with the server.
 */
export default function SignInPage() {
  const [state, formAction, pending] = useActionState(signIn, undefined);
  const [googleError, setGoogleError] = useState<string | null>(null);

  const handleGoogleSignIn = async () => {
    setGoogleError(null);
    const result = await signInWithGoogle();
    if (result.error) {
      setGoogleError(result.error);
    } else if (result.url) {
      window.location.href = result.url;
    }
  };

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
        {(state?.error || googleError) && (
          <p className="rounded-xs px-3 py-2 text-sm text-error-red" style={{ backgroundColor: "#FFB4AB20" }}>
            {state?.error || googleError}
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

      <div className="relative my-4 flex items-center justify-center">
        <div className="w-full border-t" style={{ borderColor: "var(--border-outline)" }} />
        <span className="absolute px-3 text-xs" style={{ backgroundColor: "var(--bg-surface)", color: "var(--text-secondary)" }}>
          OR
        </span>
      </div>

      <button
        type="button"
        onClick={handleGoogleSignIn}
        className="w-full flex items-center justify-center gap-3 rounded-sm border px-4 py-2.5 text-sm font-semibold transition-all hover:bg-neutral-50 dark:hover:bg-neutral-900 focus:outline-none cursor-pointer"
        style={{
          backgroundColor: "var(--bg-surface)",
          borderColor: "var(--border-outline)",
          color: "var(--text-primary)",
        }}
      >
        <svg className="h-5 w-5" viewBox="0 0 24 24" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            fill="#4285F4"
          />
          <path
            d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            fill="#34A853"
          />
          <path
            d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22-.19-.63z"
            fill="#FBBC05"
          />
          <path
            d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
            fill="#EA4335"
          />
        </svg>
        Continue with Google
      </button>

      <p className="mt-6 text-center text-sm" style={{ color: "var(--text-secondary)" }}>
        Don&apos;t have an account?{" "}
        <Link href="/auth/sign-up" className="font-medium text-primary hover:underline">
          Sign Up
        </Link>
      </p>
    </div>
  );
}
