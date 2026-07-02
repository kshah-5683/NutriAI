/**
 * Auth layout — centered, no navigation bar.
 * Used by /auth/sign-in and /auth/sign-up pages.
 */
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4">
      {/* Logo */}
      <div className="mb-8 text-center">
        <div
          className="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-xl text-3xl"
          style={{ backgroundColor: "var(--bg-primary-container)" }}
        >
          🥗
        </div>
        <h1 className="text-2xl font-semibold text-primary dark:text-primary-light">NutriAI</h1>
      </div>

      {children}
    </div>
  );
}
