"use client";

import { useState, useEffect, useRef } from "react";
import { Button } from "@/components/ui/button";
import { useUserProfile, useUpdateProfile } from "@/lib/hooks/use-user-profile";
import type { UserProfile } from "@/lib/types/domain";

// ─── Option lists ────────────────────────────────────────────────────────────

const DIET_OPTIONS = [
  { value: "vegetarian", label: "Vegetarian" },
  { value: "veg_eggs", label: "Veg + Eggs" },
  { value: "non_veg", label: "Non-Vegetarian" },
  { value: "pescatarian", label: "Pescatarian" },
  { value: "vegan", label: "Vegan" },
] as const;

const WEIGHT_GOAL_OPTIONS = [
  { value: "lose", label: "Lose" },
  { value: "maintain", label: "Maintain" },
  { value: "gain", label: "Gain" },
] as const;

const GENDER_OPTIONS = [
  { value: "male", label: "Male" },
  { value: "female", label: "Female" },
  { value: "other", label: "Other" },
  { value: "prefer_not_to_say", label: "Prefer not to say" },
] as const;

const CUISINE_OPTIONS = [
  "Indian",
  "South Indian",
  "Maharashtrian",
  "Gujarati",
  "Italian",
  "French",
  "Mexican",
  "Japanese",
  "Mediterranean",
  "Chinese",
  "Thai",
  "Korean",
] as const;

const ALLERGY_OPTIONS = [
  "Gluten",
  "Dairy",
  "Nuts",
  "Soy",
  "Shellfish",
  "Eggs",
  "Fish",
  "Sesame",
] as const;

// ─── Component ───────────────────────────────────────────────────────────────

/**
 * AI Recommendations profile card — renders between Daily Goals and Appearance
 * on the Settings page.
 *
 * Phase R4: Part of the AI Recommendations infrastructure.
 */
export function ProfileSection() {
  const { data: profile, isLoading } = useUserProfile();
  const updateProfile = useUpdateProfile();

  // Local form state
  const [enabled, setEnabled] = useState(false);
  const [age, setAge] = useState("");
  const [gender, setGender] = useState("");
  const [weightKg, setWeightKg] = useState("");
  const [weightGoal, setWeightGoal] = useState("");
  const [dietType, setDietType] = useState("");
  const [cuisines, setCuisines] = useState<string[]>([]);
  const [allergies, setAllergies] = useState<string[]>([]);
  const [customCuisine, setCustomCuisine] = useState("");
  const [customAllergy, setCustomAllergy] = useState("");
  const [isExpanded, setIsExpanded] = useState(false);

  // Track the last profile snapshot we seeded from to avoid re-seeding from stale cache.
  // On save, we optimistically update this to the saved values so the async refetch
  // doesn't trigger a re-seed with old data (race condition fix).
  const seededFromRef = useRef<string | null>(null);

  // Seed form when profile loads or genuinely changes from server (e.g., cross-device sync).
  useEffect(() => {
    if (!profile) return;
    const fingerprint = JSON.stringify(profile);
    if (seededFromRef.current === fingerprint) return;
    seededFromRef.current = fingerprint;
    setEnabled(profile.recommendationsEnabled);
    setAge(profile.age != null ? String(profile.age) : "");
    setGender(profile.gender ?? "");
    setWeightKg(profile.weightKg != null ? String(profile.weightKg) : "");
    setWeightGoal(profile.weightGoal ?? "");
    setDietType(profile.dietType ?? "");
    setCuisines(profile.cuisinePreferences);
    setAllergies(profile.allergies);
  }, [profile]);

  // Detect changes against the fetched profile
  const hasChanges = (() => {
    if (!profile) return false;
    return (
      enabled !== profile.recommendationsEnabled ||
      age !== (profile.age != null ? String(profile.age) : "") ||
      gender !== (profile.gender ?? "") ||
      weightKg !== (profile.weightKg != null ? String(profile.weightKg) : "") ||
      weightGoal !== (profile.weightGoal ?? "") ||
      dietType !== (profile.dietType ?? "") ||
      JSON.stringify(cuisines) !== JSON.stringify(profile.cuisinePreferences) ||
      JSON.stringify(allergies) !== JSON.stringify(profile.allergies)
    );
  })();

  const handleSave = () => {
    const updatedProfile: UserProfile = {
      age: age ? parseInt(age, 10) : null,
      gender: gender || null,
      weightKg: weightKg ? parseFloat(weightKg) : null,
      weightGoal: weightGoal || null,
      dietType: dietType || null,
      cuisinePreferences: cuisines,
      allergies: allergies,
      recommendationsEnabled: enabled,
    };

    // Optimistically set fingerprint to saved values so the async refetch
    // won't re-seed local state from stale cache data
    seededFromRef.current = JSON.stringify(updatedProfile);

    updateProfile.mutate(updatedProfile);
  };

  const toggleChip = (
    list: string[],
    setList: (v: string[]) => void,
    value: string
  ) => {
    setList(
      list.includes(value)
        ? list.filter((v2) => v2 !== value)
        : [...list, value]
    );
  };

  /** Strips HTML, markdown, control chars and truncates — matches server-side sanitizeProfileEntry. */
  const sanitizeEntry = (raw: string): string =>
    raw
      .replace(/<[^>]*>/g, "")
      .replace(/[#*_~`[\]{}()|\\]/g, "")
      .replace(/[\x00-\x1F\x7F]/g, "")
      .trim()
      .slice(0, 40);

  const addCustomCuisine = () => {
    const sanitized = sanitizeEntry(customCuisine);
    if (sanitized && !cuisines.some((c) => c.toLowerCase() === sanitized.toLowerCase())) {
      setCuisines([...cuisines, sanitized]);
    }
    setCustomCuisine("");
  };

  const addCustomAllergy = () => {
    const sanitized = sanitizeEntry(customAllergy);
    if (sanitized && !allergies.some((a) => a.toLowerCase() === sanitized.toLowerCase())) {
      setAllergies([...allergies, sanitized]);
    }
    setCustomAllergy("");
  };

  if (isLoading) return null;

  const isProfileComplete = profile && (
    profile.recommendationsEnabled ||
    profile.age != null ||
    profile.gender != null ||
    profile.weightKg != null ||
    profile.weightGoal != null ||
    profile.dietType != null ||
    profile.cuisinePreferences.length > 0 ||
    profile.allergies.length > 0
  );

  if (!isExpanded) {
    return (
      <div
        className="rounded-md border p-4 space-y-3"
        style={{
          backgroundColor: "var(--bg-surface)",
          borderColor: "var(--border-variant)",
        }}
      >
        <div className="flex items-center justify-between">
          <div>
            <h2
              className="text-sm font-semibold"
              style={{ color: "var(--text-primary)" }}
            >
              AI Recommendations
            </h2>
            <p className="text-xs mt-1" style={{ color: "var(--text-secondary)" }}>
              {isProfileComplete ? "Profile configured" : "Set up your dietary preferences"}
            </p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsExpanded(true)}
            className="cursor-pointer font-semibold"
            style={{ color: "var(--text-branded)" }}
          >
            {isProfileComplete ? "Edit" : "Set Up"}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div
      className="rounded-md border p-4 space-y-4 animate-in fade-in slide-in-from-top-2 duration-200"
      style={{
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border-variant)",
      }}
    >
      <h2
        className="text-sm font-semibold"
        style={{ color: "var(--text-primary)" }}
      >
        AI Recommendations Profile
      </h2>

      <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
        Personalize your meal suggestions with your dietary preferences.
      </p>

      {/* Enable toggle */}
      <div className="flex items-center justify-between">
        <span
          className="text-sm font-medium"
          style={{ color: "var(--text-primary)" }}
        >
          Enable AI Recommendations
        </span>
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          onClick={() => setEnabled(!enabled)}
          className="relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors"
          style={{
            backgroundColor: enabled
              ? "var(--color-primary)"
              : "var(--border-variant)",
          }}
        >
          <span
            className="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transition-transform"
            style={{
              transform: enabled ? "translateX(20px)" : "translateX(0px)",
            }}
          />
        </button>
      </div>

      {/* Profile fields — visible only when enabled */}
      {enabled && (
        <div className="space-y-4 pt-2">
          {/* Age */}
          <div className="flex items-center gap-3">
            <span
              className="w-20 text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Age
            </span>
            <div className="flex flex-1 items-center gap-1">
              <input
                type="number"
                inputMode="numeric"
                min="1"
                max="120"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                placeholder="e.g. 25"
                className="w-full rounded-md border px-3 py-2 text-sm outline-none transition-colors focus:border-[var(--color-primary)]"
                style={{
                  backgroundColor: "var(--bg-app)",
                  borderColor: "var(--border-variant)",
                  color: "var(--text-primary)",
                }}
              />
              <span
                className="shrink-0 text-xs"
                style={{ color: "var(--text-secondary)" }}
              >
                years
              </span>
            </div>
          </div>

          {/* Gender */}
          <div className="flex items-center gap-3">
            <span
              className="w-20 text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Gender
            </span>
            <select
              value={gender}
              onChange={(e) => setGender(e.target.value)}
              className="flex-1 rounded-md border px-3 py-2 text-sm outline-none transition-colors focus:border-[var(--color-primary)]"
              style={{
                backgroundColor: "var(--bg-app)",
                borderColor: "var(--border-variant)",
                color: "var(--text-primary)",
              }}
            >
              <option value="">Select...</option>
              {GENDER_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Weight */}
          <div className="flex items-center gap-3">
            <span
              className="w-20 text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Weight
            </span>
            <div className="flex flex-1 items-center gap-1">
              <input
                type="number"
                inputMode="decimal"
                min="20"
                max="300"
                step="0.1"
                value={weightKg}
                onChange={(e) => setWeightKg(e.target.value)}
                placeholder="e.g. 70"
                className="w-full rounded-md border px-3 py-2 text-sm outline-none transition-colors focus:border-[var(--color-primary)]"
                style={{
                  backgroundColor: "var(--bg-app)",
                  borderColor: "var(--border-variant)",
                  color: "var(--text-primary)",
                }}
              />
              <span
                className="shrink-0 text-xs"
                style={{ color: "var(--text-secondary)" }}
              >
                kg
              </span>
            </div>
          </div>

          {/* Weight Goal */}
          <div className="space-y-2">
            <span
              className="text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Weight Goal
            </span>
            <div className="flex flex-wrap gap-2">
              {WEIGHT_GOAL_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() =>
                    setWeightGoal(weightGoal === opt.value ? "" : opt.value)
                  }
                  className="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                  style={{
                    backgroundColor:
                      weightGoal === opt.value
                        ? "var(--color-primary)"
                        : "transparent",
                    borderColor:
                      weightGoal === opt.value
                        ? "var(--color-primary)"
                        : "var(--border-variant)",
                    color:
                      weightGoal === opt.value
                        ? "#FFFFFF"
                        : "var(--text-primary)",
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Diet Type */}
          <div className="flex items-center gap-3">
            <span
              className="w-20 text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Diet Type
            </span>
            <select
              value={dietType}
              onChange={(e) => setDietType(e.target.value)}
              className="flex-1 rounded-md border px-3 py-2 text-sm outline-none transition-colors focus:border-[var(--color-primary)]"
              style={{
                backgroundColor: "var(--bg-app)",
                borderColor: "var(--border-variant)",
                color: "var(--text-primary)",
              }}
            >
              <option value="">Select...</option>
              {DIET_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Cuisine Preferences */}
          <div className="space-y-2">
            <span
              className="text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Preferred Cuisines
            </span>
            <div className="flex flex-wrap gap-2">
              {/* Predefined options */}
              {CUISINE_OPTIONS.map((cuisine) => (
                <button
                  key={cuisine}
                  type="button"
                  onClick={() => toggleChip(cuisines, setCuisines, cuisine)}
                  className="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                  style={{
                    backgroundColor: cuisines.includes(cuisine)
                      ? "var(--color-primary)"
                      : "transparent",
                    borderColor: cuisines.includes(cuisine)
                      ? "var(--color-primary)"
                      : "var(--border-variant)",
                    color: cuisines.includes(cuisine)
                      ? "#FFFFFF"
                      : "var(--text-primary)",
                  }}
                >
                  {cuisine}
                </button>
              ))}
              {/* Custom cuisines not in predefined list */}
              {cuisines
                .filter((c) => !CUISINE_OPTIONS.includes(c as typeof CUISINE_OPTIONS[number]))
                .map((cuisine) => (
                  <button
                    key={cuisine}
                    type="button"
                    onClick={() => toggleChip(cuisines, setCuisines, cuisine)}
                    className="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                    style={{
                      backgroundColor: "var(--color-primary)",
                      borderColor: "var(--color-primary)",
                      color: "#FFFFFF",
                    }}
                  >
                    {cuisine} ×
                  </button>
                ))}
            </div>
            {/* Add custom cuisine */}
            <div className="flex gap-2">
              <input
                type="text"
                value={customCuisine}
                onChange={(e) => setCustomCuisine(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") { e.preventDefault(); addCustomCuisine(); }
                }}
                maxLength={40}
                placeholder="Add other cuisine..."
                className="flex-1 rounded-md border px-3 py-1.5 text-xs outline-none transition-colors focus:border-[var(--color-primary)]"
                style={{
                  backgroundColor: "var(--bg-app)",
                  borderColor: "var(--border-variant)",
                  color: "var(--text-primary)",
                }}
              />
              <button
                type="button"
                onClick={addCustomCuisine}
                disabled={!customCuisine.trim()}
                className="rounded-md border px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-40"
                style={{
                  borderColor: "var(--color-primary)",
                  color: "var(--color-primary)",
                }}
              >
                Add
              </button>
            </div>
          </div>

          {/* Allergies */}
          <div className="space-y-2">
            <span
              className="text-sm font-medium"
              style={{ color: "var(--text-primary)" }}
            >
              Allergies & Restrictions
            </span>
            <div className="flex flex-wrap gap-2">
              {/* Predefined options */}
              {ALLERGY_OPTIONS.map((allergy) => (
                <button
                  key={allergy}
                  type="button"
                  onClick={() => toggleChip(allergies, setAllergies, allergy)}
                  className="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                  style={{
                    backgroundColor: allergies.includes(allergy)
                      ? "var(--color-primary)"
                      : "transparent",
                    borderColor: allergies.includes(allergy)
                      ? "var(--color-primary)"
                      : "var(--border-variant)",
                    color: allergies.includes(allergy)
                      ? "#FFFFFF"
                      : "var(--text-primary)",
                  }}
                >
                  {allergy}
                </button>
              ))}
              {/* Custom allergies not in predefined list */}
              {allergies
                .filter((a) => !ALLERGY_OPTIONS.includes(a as typeof ALLERGY_OPTIONS[number]))
                .map((allergy) => (
                  <button
                    key={allergy}
                    type="button"
                    onClick={() => toggleChip(allergies, setAllergies, allergy)}
                    className="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
                    style={{
                      backgroundColor: "var(--color-primary)",
                      borderColor: "var(--color-primary)",
                      color: "#FFFFFF",
                    }}
                  >
                    {allergy} ×
                  </button>
                ))}
            </div>
            {/* Add custom allergy */}
            <div className="flex gap-2">
              <input
                type="text"
                value={customAllergy}
                onChange={(e) => setCustomAllergy(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") { e.preventDefault(); addCustomAllergy(); }
                }}
                maxLength={40}
                placeholder="Add other allergy..."
                className="flex-1 rounded-md border px-3 py-1.5 text-xs outline-none transition-colors focus:border-[var(--color-primary)]"
                style={{
                  backgroundColor: "var(--bg-app)",
                  borderColor: "var(--border-variant)",
                  color: "var(--text-primary)",
                }}
              />
              <button
                type="button"
                onClick={addCustomAllergy}
                disabled={!customAllergy.trim()}
                className="rounded-md border px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-40"
                style={{
                  borderColor: "var(--color-primary)",
                  color: "var(--color-primary)",
                }}
              >
                Add
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Save & Cancel buttons */}
      <div className="flex gap-3 mt-4">
        <Button
          type="button"
          onClick={() => {
            if (profile) {
              setEnabled(profile.recommendationsEnabled);
              setAge(profile.age != null ? String(profile.age) : "");
              setGender(profile.gender ?? "");
              setWeightKg(profile.weightKg != null ? String(profile.weightKg) : "");
              setWeightGoal(profile.weightGoal ?? "");
              setDietType(profile.dietType ?? "");
              setCuisines(profile.cuisinePreferences);
              setAllergies(profile.allergies);
            }
            setIsExpanded(false);
          }}
          variant="ghost"
          className="flex-1 cursor-pointer border"
          style={{ borderColor: "var(--border-variant)", color: "var(--text-secondary)" }}
        >
          Cancel
        </Button>
        <Button
          onClick={() => {
            handleSave();
            setIsExpanded(false);
          }}
          disabled={!hasChanges || updateProfile.isPending}
          className="flex-1"
        >
          {updateProfile.isPending ? "Saving..." : "Save Profile"}
        </Button>
      </div>

      {updateProfile.isError && (
        <p
          className="text-xs text-center mt-2"
          style={{ color: "var(--color-error-red)" }}
        >
          {updateProfile.error instanceof Error
            ? updateProfile.error.message
            : "Failed to save profile. Please try again."}
        </p>
      )}

      {updateProfile.isSuccess && !hasChanges && (
        <p
          className="text-xs text-center mt-2"
          style={{ color: "var(--text-branded)" }}
        >
          Profile saved successfully!
        </p>
      )}
    </div>
  );
}
