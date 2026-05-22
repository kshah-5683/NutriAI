/**
 * Standard CORS headers for all Edge Functions.
 * Every Edge Function must:
 *   1. Import corsHeaders + handleCors
 *   2. Return corsHeaders in every response
 *   3. Handle OPTIONS preflight via handleCors()
 */

export const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": Deno.env.get("ALLOWED_ORIGIN") ?? "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

/** Pre-flight OPTIONS handler. */
export function handleCors(): Response {
  return new Response("ok", { headers: corsHeaders });
}
