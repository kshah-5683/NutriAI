import { type InputHTMLAttributes, forwardRef } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

/**
 * Styled text input with label and optional error message.
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, id, className = "", ...props }, ref) => {
    const inputId = id || label.toLowerCase().replace(/\s+/g, "-");

    return (
      <div className="flex flex-col gap-1.5">
        <label
          htmlFor={inputId}
          className="text-sm font-medium"
          style={{ color: "var(--text-secondary)" }}
        >
          {label}
        </label>
        <input
          ref={ref}
          id={inputId}
          className={`rounded-sm border px-3 py-2 text-sm outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary ${
            error ? "border-error-red" : ""
          } ${className}`}
          style={{
            backgroundColor: "var(--bg-surface)",
            borderColor: error ? undefined : "var(--border-outline)",
            color: "var(--text-primary)",
          }}
          {...props}
        />
        {error && (
          <p className="text-xs text-error-red">{error}</p>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";
