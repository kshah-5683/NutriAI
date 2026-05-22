"use client";

import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useDeleteFood } from "@/lib/hooks/use-delete-food";
import type { FoodItem } from "@/lib/types/domain";

interface ConfirmDeleteFoodDialogProps {
  food: FoodItem | null;
  onClose: () => void;
}

/**
 * Confirmation dialog for soft-deleting a catalog food item.
 * Port of Android's ConfirmDeleteDialog (catalog variant).
 *
 * Message matches Android: "Remove from your catalog? Existing log entries won't be affected."
 *
 * CRITICAL PARITY: Soft-delete sets both deleted_at AND last_modified_at
 * so the LWW guard trigger resolves correctly when syncing with Android.
 * Never hard-deletes — pg_cron handles tombstone purge after 15 days.
 */
export function ConfirmDeleteFoodDialog({ food, onClose }: ConfirmDeleteFoodDialogProps) {
  const deleteFood = useDeleteFood();

  const handleDelete = async () => {
    if (!food) return;

    try {
      await deleteFood.mutateAsync(food.id);
      onClose();
    } catch {
      // Error is handled by TanStack Query — the mutation stays in error state.
    }
  };

  return (
    <Dialog open={!!food} onClose={onClose} title="Delete from Catalog">
      {food && (
        <div className="flex flex-col gap-4">
          <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
            Remove{" "}
            <strong style={{ color: "var(--text-primary)" }}>&quot;{food.name}&quot;</strong>{" "}
            from your catalog? Existing log entries won&apos;t be affected.
          </p>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              loading={deleteFood.isPending}
              onClick={handleDelete}
            >
              Delete
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
