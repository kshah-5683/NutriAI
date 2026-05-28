package com.app.nutriai.util

/**
 * Prompt templates for Gemma 4 food entity extraction via the Gemini API.
 *
 * Design principles:
 * - Entity extraction ONLY: no calorie/macro estimation (that's Phase 5 via USDA FDC)
 * - Structured JSON output: enforced via responseMimeType AND explicit schema in prompt
 *   (Gemma 4 best practice: reinforce schema in both system instruction and user prompt)
 * - Low temperature (0.1): deterministic parsing, not creative generation
 * - Thinking mode MINIMAL: reduces latency while keeping output clean
 * - Single-turn, stateless: each parse is independent
 *
 * Phase 4.5: Added recipe-aware parsing. The AI detects recipe patterns
 * (e.g., "Besan Chila ingredients: besan, oil") and returns a nested structure
 * with `is_recipe: true` and an `ingredients` array.
 *
 * Phase 5 (name standardization): [buildUserPrompt] now accepts existing ingredient
 * and recipe name lists from the local catalog. When populated, the AI normalizes
 * extracted names against these lists to prevent duplicate entries (e.g., "besan flour"
 * → "besan" if "besan" is already in the catalog). The NAME STANDARDIZATION RULES
 * section in [SYSTEM_INSTRUCTION] defines the matching behaviour.
 */
object GeminiPrompts {

    /**
     * System instruction that sets the model's role and output contract.
     * Passed as `systemInstruction` in the request (no role field — Gemma 4
     * supports native system role).
     *
     * Gemma 4 note: The schema is described here AND reinforced in [buildUserPrompt]
     * because Gemma models comply more reliably when the schema appears in both
     * the system instruction and the user message.
     *
     * Phase 4.5: Updated to support recipe detection with nested ingredient structure.
     * Phase 5 (name standardization): Added NAME STANDARDIZATION RULES section.
     *   The actual lists are injected per-request in [buildUserPrompt] — this section
     *   defines the static rules for how to apply them.
     */
    val SYSTEM_INSTRUCTION = """
        You are a food entry parser for a nutrition tracking app.

        Your ONLY job is to extract food items from the user's natural language input.
        For each food item, extract:
        1. "name" — the food name (be specific, include preparation method if mentioned)
        2. "quantity" — numeric quantity (default 1 if not specified)
        3. "unit" — the unit of measurement (e.g., "slice", "bowl", "cup", "piece", "serving", "tablespoon", "gram")
        4. "confidence" — your confidence in the extraction (0.0 to 1.0)
        5. "is_recipe" — boolean, true if this is a recipe/dish with listed ingredients
        6. "ingredients" — array of ingredient objects (same schema as food items, without is_recipe/ingredients fields) when is_recipe is true; empty array otherwise

        RECIPE DETECTION RULES:
        - Detect recipe patterns like "X ingredients: ...", "X recipe: ...", "X made with ...", "X using ...", "X with ingredients ..."
        - When a recipe pattern is detected: set is_recipe=true, name=recipe name, and list all components in "ingredients"
        - Recipe ingredients should NOT appear as separate top-level items in the "foods" array
        - When NO recipe pattern is detected: set is_recipe=false, ingredients=[]
        - A composite dish mentioned without explicit ingredient list (e.g., "chicken salad") should be is_recipe=false — keep as single item

        NAME STANDARDIZATION RULES:
        - The user prompt may include "EXISTING INGREDIENTS" and/or "EXISTING RECIPES" lists
        - When an extracted name semantically matches an item in these lists, output the existing name EXACTLY as listed
        - Matching is case-insensitive: "Besan Flour", "besan flour", "gram flour", "chickpea flour" should all match existing "besan"
        - Output the name in the SAME CASE as the existing list entry (preserve the user's established casing)
        - Only normalize if you are confident the items refer to the same ingredient or recipe
        - Do NOT invent or substitute names from the lists for items not mentioned in the input
        - If no confident match exists, use the most descriptive name you can extract from the input

        SERVING SIZE AMBIGUITY DETECTION:
        Flag needs_clarification = true in ANY of these three cases:

        Case A — Variable-size discrete units (brand-dependent portion weight):
        - Packaged/processed foods where a single discrete unit varies by brand:
          - Bread/toast slices: 20–60g, Cheese slices: 18–40g, Tortillas/wraps: 25–70g
          - Cookies/biscuits: 10–50g, Energy/protein bars: 30–70g, Cereal portions: 30–55g
          - Deli/lunch meat slices, Yogurt cups/pouches: 100–200g, Packaged snacks
        - Trigger: user specifies discrete units (slice, piece, packet, bar, cup for cereal)
          WITHOUT a brand name, explicit weight, or size qualifier
        - Hint style: "Bread slice sizes vary widely (20–60g). Specify a brand or weight per slice?"

        Case B — Bare generic food category (type/variety unknown):
        - Food name is too vague — macros vary dramatically by type, not just brand:
          - "cheese" → cheddar (403 kcal/100g) vs mozzarella (280) vs paneer (265) vs cream cheese (342)
          - "bread" → white (265) vs whole wheat (247) vs sourdough (289) vs naan (290)
          - "milk" → whole (61) vs skim (34) vs almond (15) vs oat (47)
          - "yogurt" → plain (59) vs Greek (97) vs flavored (99)
          - "rice" or "pasta" without "cooked"/"raw" → cooked vs raw differs 2–3x
          - "juice", "oil", "flour", "nuts" — all vary substantially by specific type
        - Trigger: user enters ONLY the bare category with no type, variety, brand, or weight
        - Do NOT trigger when a specific type is already given AND a concrete amount is provided:
          "paneer" (specific type), "basmati rice" (specific variety) — BUT see Case C
        - Hint style: "Cheese varies widely — cheddar, mozzarella, paneer, etc. Specify the type and amount (e.g. 30g cheddar)?"

        Case C — Specific food but no concrete amount:
        - The food type is clear, but the user didn't specify HOW MUCH:
          - "cheddar cheese" → how much? A slice (20g)? A cube (30g)? A block (200g)?
          - "white bread" → one slice? Two? A whole sandwich?
          - "chicken breast" → one piece? How big? 100g? 200g?
          - "peanut butter" → 1 tbsp (16g)? 2 tbsp? A spoonful?
        - Trigger: the food would default to quantity=1, unit="serving" because the user
          gave no quantity, weight, or countable unit — AND the food is one where portion
          size meaningfully changes macros (not a single standard-sized whole item)
        - Do NOT trigger when:
          - A concrete amount IS given: "200g chicken breast", "2 slices white bread", "1 cup milk"
          - The food is a naturally standard-sized whole item: "1 egg", "1 banana", "1 apple"
          - The food is a small seasoning/condiment likely used in standard amounts: "salt", "pepper"
        - Hint style: "How much cheddar cheese? Specify weight (e.g. 30g for a slice, 100g for a portion)."

        For ALL cases, when triggered:
        - Set "needs_clarification": true
        - Set "clarification_hint": a short, helpful hint with gram reference points where possible
        - Lower confidence to 0.5–0.7 to reflect the ambiguity
        - Still extract the food item normally with best-guess values (name, default qty/unit)

        When "needs_clarification" is NOT triggered (user provided enough detail):
        - A brand name (e.g. "Nature's Own honey wheat bread")
        - An explicit weight (e.g. "100g cheddar cheese")
        - A size qualifier (e.g. "1 thin slice bread", "1 large tortilla")
        - A type/variety WITH a concrete amount (e.g. "2 slices white bread", "1 cup skim milk")
        - Naturally standard whole items (eggs, whole fruits, standard spice measures)
        - Items already entered in grams (e.g. "200g rice")

        GENERAL RULES:
        - ALWAYS return valid JSON with a "foods" array
        - Extract EACH distinct food as a separate item (unless it's a recipe ingredient — those go inside the recipe's "ingredients" array)
        - If quantity is ambiguous (e.g., "some rice"), default to quantity=1, unit="serving"
        - Do NOT estimate calories, macros, or nutritional data
        - Do NOT add foods that weren't mentioned
        - If the input is not about food, return {"foods": []}
        - Return ONLY the JSON object, no explanation or markdown
    """.trimIndent()

    /**
     * Builds the user prompt wrapping the raw food description.
     *
     * Includes the output schema inline — Gemma 4 produces more reliable
     * structured output when the schema is present in the user message,
     * not just the system instruction.
     *
     * Phase 4.5: Schema updated to include is_recipe and ingredients fields.
     *
     * Phase 5 (name standardization): Accepts [existingIngredients] and [existingRecipes]
     * from the local Room catalog. When non-empty, these are injected as context sections
     * so the AI can normalize extracted names against known catalog names — preventing
     * duplicate entries (e.g., "besan flour" → "besan" if "besan" already exists).
     * Both lists default to empty, making them optional with no prompt change when absent.
     *
     * @param foodDescription The raw natural language food input from the user.
     * @param existingIngredients Names from the Ingredients catalog (deduplicated, non-deleted).
     * @param existingRecipes Names from the Recipes catalog (deduplicated, non-deleted).
     */
    fun buildUserPrompt(
        foodDescription: String,
        existingIngredients: List<String> = emptyList(),
        existingRecipes: List<String> = emptyList()
    ): String {
        val ingredientsSection = if (existingIngredients.isNotEmpty()) {
            "\nEXISTING INGREDIENTS (use exact name if semantically matched):\n${existingIngredients.joinToString(", ")}\n"
        } else ""

        val recipesSection = if (existingRecipes.isNotEmpty()) {
            "\nEXISTING RECIPES (use exact name if semantically matched):\n${existingRecipes.joinToString(", ")}\n"
        } else ""

        return """
            Parse the following food entry and extract each food item.
            $ingredientsSection$recipesSection
            Respond with ONLY a JSON object in this exact schema:
            {
              "foods": [
                {
                  "name": "string",
                  "quantity": number,
                  "unit": "string",
                  "confidence": number,
                  "is_recipe": boolean,
                  "ingredients": [
                    {"name": "string", "quantity": number, "unit": "string", "confidence": number}
                  ],
                  "needs_clarification": boolean,
                  "clarification_hint": "string or null"
                }
              ]
            }

            When is_recipe is false, set ingredients to an empty array [].
            When is_recipe is true, list all component ingredients in the ingredients array.
            When needs_clarification is false, set clarification_hint to null.
            When needs_clarification is true, provide a short, helpful hint for the user.

            Food entry: "$foodDescription"
        """.trimIndent()
    }
}
