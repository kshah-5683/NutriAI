import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * DIAGNOSTIC passthrough — no auth guard.
 * Temporarily disabled Supabase session check to confirm proxy is root cause of 404.
 * Restore full auth guard once deployment is confirmed working.
 */
export async function proxy(request: NextRequest) {
  return NextResponse.next({ request });
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};
