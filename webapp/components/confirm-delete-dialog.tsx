"use client";

import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useDeleteLog } from "@/lib/hooks/use-delete-log";
import type { DailyLog } from "@/lib/types/domain";

interface ConfirmDeleteDialogProps {
  log: DailyLog | null;
  onClose: () => void;
}

/**
 * Confirmation dialog for soft-deleting a daily log entry.
 *
 * CRITICAL PARITY: Soft-delete sets both deleted_at AND last_modified_at
 * so the LWW guard trigger resolves correctly when syncing with Android.
 * Never hard-deletes — pg_cron handles tombstone purge after 15 days.
 */
export function ConfirmDeleteDialog({ log, onClose }: ConfirmDeleteDialogProps) {
  const deleteLog = useDeleteLog();

  const handleDelete = async () => {
    if (!log) return;

    try {
      await deleteLog.mutateAsync(log.id);
      onClose();
    } catch {
      // Error is handled by TanStack Query — the mutation stays in error state.
      // In production, you'd show an error toast here.
    }
  };

  return (
    <Dialog open={!!log} onClose={onClose} title="Delete Entry">
      {log && (
        <div className="flex flex-col gap-4">
          <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
            Are you sure you want to delete{" "}
            <strong style={{ color: "var(--text-primary)" }}>{log.foodName}</strong>?
            This action can be undone within 15 days.
          </p>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              loading={deleteLog.isPending}
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
