"use server";

import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

/**
 * Server Action: Sign in with email and password.
 * Called from the sign-in form via useActionState.
 */
export async function signIn(
  _prevState: { error: string } | undefined,
  formData: FormData
): Promise<{ error: string } | undefined> {
  const email = formData.get("email") as string;
  const password = formData.get("password") as string;

  if (!email?.trim() || !password?.trim()) {
    return { error: "Email and password are required." };
  }

  const supabase = await createClient();

  const { error } = await supabase.auth.signInWithPassword({
    email: email.trim(),
    password,
  });

  if (error) {
    return { error: error.message };
  }
  // redirect() throws internally — must be called outside try/catch
  redirect("/");
}

/**
 * Server Action: Sign up with email and password.
 * After sign-up, Supabase sends a confirmation email.
 * The user clicks the link → /auth/confirm route handler exchanges the code.
 */
export async function signUp(
  _prevState: { error: string; success?: boolean } | undefined,
  formData: FormData
): Promise<{ error: string; success?: boolean } | undefined> {
  const email = formData.get("email") as string;
  const password = formData.get("password") as string;

  if (!email?.trim()) {
    return { error: "Email is required." };
  }
  if (!password || password.length < 6) {
    return { error: "Password must be at least 6 characters." };
  }

  const supabase = await createClient();

  const { error } = await supabase.auth.signUp({
    email: email.trim(),
    password,
  });

  if (error) {
    return { error: error.message };
  }

  // Don't redirect — show "check your email" message
  return { error: "", success: true };
}

/**
 * Server Action: Sign out the current user.
 * Called from the dashboard layout's sign-out button.
 */
export async function signOut(): Promise<void> {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/auth/sign-in");
}
