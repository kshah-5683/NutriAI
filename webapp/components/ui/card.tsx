import type { HTMLAttributes } from "react";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** Remove default padding */
  noPadding?: boolean;
}

/**
 * Surface card — Forest & Cream themed container.
 */
export function Card({
  noPadding = false,
  className = "",
  children,
  ...props
}: CardProps) {
  return (
    <div
      className={`rounded-md border ${noPadding ? "" : "p-4"} ${className}`}
      style={{
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border-variant)",
      }}
      {...props}
    >
      {children}
    </div>
  );
}
