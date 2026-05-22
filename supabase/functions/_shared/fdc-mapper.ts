/**
 * USDA FoodData Central response mapping utilities.
 * Port of FoodDataCentralResponse.kt (FdcFood.toNutritionInfo + scoring).
 *
 * Nutrient ID reference (USDA standard nutrient numbers):
 *   1008 = Energy (kcal)
 *   1003 = Protein (g)
 *   1005 = Carbohydrate, by difference (g)
 *   1004 = Total lipid (fat) (g)
 *   1079 = Fiber, total dietary (g)
 *
 * All values are per 100g unless unitName indicates otherwise.
 */

const NUTRIENT_ENERGY_KCAL = 1008;
const NUTRIENT_PROTEIN = 1003;
const NUTRIENT_CARBS = 1005;
const NUTRIENT_FAT = 1004;
const NUTRIENT_FIBER = 1079;

export interface NutritionInfo {
  productName: string;
  brand: string | null;
  caloriesPer100g: number;
  proteinPer100g: number;
  carbsPer100g: number;
  fatPer100g: number;
  fiberPer100g: number | null;
  source: string;
  externalId: string | null;
  /**
   * Gram weight of a single discrete serving unit (piece, slice, bowl).
   * Extracted from FDC `servingSize` when `servingSizeUnit` is "g".
   * Used by the Android app to compute correct macros for "1 piece egg" etc.
   * Null when FDC does not report a gram-based serving size.
   */
  servingWeightG: number | null;
}

/** Shape of a single food from the FDC /foods/search response. */
// deno-lint-ignore no-explicit-any
type FdcFood = Record<string, any>;

/**
 * Extracts a nutrient value by ID from a FDC food's foodNutrients array.
 */
function nutrientValue(
  // deno-lint-ignore no-explicit-any
  foodNutrients: any[],
  nutrientId: number
): number | null {
  // deno-lint-ignore no-explicit-any
  const entry = foodNutrients.find((n: any) => n.nutrientId === nutrientId);
  return entry?.value ?? null;
}

/**
 * Maps a FDC food object to our NutritionInfo domain model.
 * Returns null if calories are missing (minimum viable data).
 *
 * Port of FdcFood.toNutritionInfo() from FoodDataCentralResponse.kt.
 */
export function mapFdcToNutritionInfo(food: FdcFood): NutritionInfo | null {
  const nutrients = food.foodNutrients ?? [];
  const kcal = nutrientValue(nutrients, NUTRIENT_ENERGY_KCAL);
  if (kcal == null) return null; // Must have calories

  // Resolve brand: brandName preferred over brandOwner.
  // For Foundation/SR Legacy foods these are typically null (generic foods).
  const brandName = food.brandName?.trim() || null;
  const brandOwner = food.brandOwner?.trim() || null;
  const brand = brandName ?? brandOwner;

  // Extract gram-based serving weight for discrete units (piece, slice, bowl).
  // Only valid when servingSizeUnit is "g"; volumetric units (ml, oz) are ignored.
  const rawServingSize: number | null = food.servingSize ?? null;
  const servingSizeUnit: string | null = food.servingSizeUnit ?? null;
  const servingWeightG: number | null =
    rawServingSize != null &&
    rawServingSize > 0 &&
    servingSizeUnit?.trim().toLowerCase() === "g"
      ? rawServingSize
      : null;

  return {
    productName: food.description?.trim() || "Unknown",
    brand,
    caloriesPer100g: Math.max(kcal, 0),
    proteinPer100g: Math.max(nutrientValue(nutrients, NUTRIENT_PROTEIN) ?? 0, 0),
    carbsPer100g: Math.max(nutrientValue(nutrients, NUTRIENT_CARBS) ?? 0, 0),
    fatPer100g: Math.max(nutrientValue(nutrients, NUTRIENT_FAT) ?? 0, 0),
    fiberPer100g:
      nutrientValue(nutrients, NUTRIENT_FIBER) != null
        ? Math.max(nutrientValue(nutrients, NUTRIENT_FIBER)!, 0)
        : null,
    source: "USDA FoodData Central",
    externalId: food.fdcId?.toString() ?? null,
    servingWeightG,
  };
}

/**
 * Ranks a NutritionInfo by macro completeness.
 * Higher score = more complete data. Used to sort FDC results so
 * Foundation/SR Legacy foods (comprehensive macros) rank above sparse Branded entries.
 *
 * Port of the scoring logic in NutritionRepositoryImpl.tryFdc().
 */
export function macroScore(info: NutritionInfo): number {
  let score = 0;
  if (info.caloriesPer100g > 0) score += 4;
  if (info.proteinPer100g > 0) score += 1;
  if (info.carbsPer100g > 0) score += 1;
  if (info.fatPer100g > 0) score += 1;
  return score;
}
