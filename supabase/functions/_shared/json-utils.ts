/**
 * Robustly extracts a JSON object from a Gemini response text.
 *
 * Handles three common response formats:
 *   1. Clean JSON: `{"foods": [...]}`
 *   2. Markdown-fenced: ` ```json\n{"foods": [...]}\n``` `
 *   3. Text with embedded JSON: `Here is the result: {"foods": [...]}`
 *
 * Falls back to an empty object on parse failure.
 * Direct port of AiRepositoryImpl.extractJson() from Kotlin.
 */
// deno-lint-ignore no-explicit-any
export function extractJson(text: string): any {
  const trimmed = text.trim();

  // 1. Try direct parse first (clean JSON)
  try {
    return JSON.parse(trimmed);
  } catch {
    // continue to fallbacks
  }

  // 2. Strip markdown fences: ```json ... ``` or ``` ... ```
  const fenceMatch = trimmed.match(/```(?:json)?\s*\n?([\s\S]*?)\n?\s*```/);
  if (fenceMatch) {
    try {
      return JSON.parse(fenceMatch[1].trim());
    } catch {
      // continue
    }
  }

  // 3. Find first { to last } (embedded JSON in prose)
  const firstBrace = trimmed.indexOf("{");
  const lastBrace = trimmed.lastIndexOf("}");
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    try {
      return JSON.parse(trimmed.substring(firstBrace, lastBrace + 1));
    } catch {
      // continue
    }
  }

  // 4. Fallback — return empty object
  console.warn(
    "[extractJson] Failed to parse response:",
    trimmed.substring(0, 200)
  );
  return {};
}
