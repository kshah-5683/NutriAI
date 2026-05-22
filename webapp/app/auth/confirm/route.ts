import { type NextRequest, NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";

/**
 * Route Handler for Supabase PKCE email confirmation callback.
 *
 * Supabase sends the user here after clicking the confirmation link.
 * The `code` query parameter is exchanged for a session, which is
 * written to cookies by the Supabase server client.
 */
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const code = searchParams.get("code");

  if (!code) {
    // Missing code — redirect to sign-in with error
    return NextResponse.redirect(
      new URL("/auth/sign-in?error=missing_code", request.url)
    );
  }

  const supabase = await createClient();
  const { error } = await supabase.auth.exchangeCodeForSession(code);

  if (error) {
    console.error("[auth/confirm] Code exchange failed:", error.message);
    return NextResponse.redirect(
      new URL("/auth/sign-in?error=confirmation_failed", request.url)
    );
  }

  // Success — redirect to dashboard
  return NextResponse.redirect(new URL("/", request.url));
}
