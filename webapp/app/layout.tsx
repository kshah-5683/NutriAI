import type { Metadata } from "next";
import { Outfit } from "next/font/google";
import { QueryProvider } from "@/components/providers/query-provider";
import { SupabaseProvider } from "@/components/providers/supabase-provider";
import { ThemeInitScript } from "@/components/theme-init-script";
import "./globals.css";

// Outfit is a variable font — no weight array needed.
// The variable prop wires it up as a CSS custom property
// so globals.css can reference var(--font-outfit-var).
const outfit = Outfit({
  subsets: ["latin"],
  variable: "--font-outfit-var",
  display: "swap",
});

export const metadata: Metadata = {
  title: "NutriAI",
  description: "AI-powered nutrition tracking",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={outfit.variable} suppressHydrationWarning>
      <head>
        {/* Blocking script to prevent flash of wrong theme on page load */}
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var t=localStorage.getItem("nutriai-theme")||"system";var d=t==="dark"||(t==="system"&&window.matchMedia("(prefers-color-scheme:dark)").matches);if(d)document.documentElement.classList.add("dark")}catch(e){}})()`,
          }}
        />
      </head>
      <body className="min-h-full flex flex-col antialiased">
        <SupabaseProvider>
          <QueryProvider>
            <ThemeInitScript />
            {children}
          </QueryProvider>
        </SupabaseProvider>
      </body>
    </html>
  );
}
