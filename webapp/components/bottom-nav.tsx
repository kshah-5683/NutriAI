"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_ITEMS = [
  { href: "/", label: "Home", icon: HomeIcon },
  { href: "/insights", label: "Insights", icon: InsightsIcon },
  { href: "/catalog", label: "Catalog", icon: CatalogIcon },
  { href: "/settings", label: "Settings", icon: SettingsIcon },
] as const;

/**
 * Bottom navigation bar — 4 tabs: Home, Insights, Catalog, Settings.
 * Sticky to the bottom of the viewport.
 */
export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav
      className="sticky bottom-0 flex items-center justify-around border-t py-2"
      style={{
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border-variant)",
      }}
    >
      {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
        const isActive = pathname === href;
        return (
          <Link
            key={href}
            href={href}
            className="flex flex-col items-center gap-0.5 px-4 py-1"
          >
            <Icon active={isActive} />
            <span
              className="text-xs font-medium"
              style={{
                color: isActive
                  ? "var(--color-primary)"
                  : "var(--text-secondary)",
              }}
            >
              {label}
            </span>
          </Link>
        );
      })}
    </nav>
  );
}

function HomeIcon({ active }: { active: boolean }) {
  const color = active ? "var(--color-primary)" : "var(--text-secondary)";
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <path
        d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill={active ? `${color}20` : "none"}
      />
      <path
        d="M9 22V12H15V22"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function InsightsIcon({ active }: { active: boolean }) {
  const color = active ? "var(--color-primary)" : "var(--text-secondary)";
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <path
        d="M18 20V10M12 20V4M6 20V14"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CatalogIcon({ active }: { active: boolean }) {
  const color = active ? "var(--color-primary)" : "var(--text-secondary)";
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <path
        d="M4 19.5C4 18.837 4.26339 18.2011 4.73223 17.7322C5.20107 17.2634 5.83696 17 6.5 17H20"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M6.5 2H20V22H6.5C5.83696 22 5.20107 21.7366 4.73223 21.2678C4.26339 20.7989 4 20.163 4 19.5V4.5C4 3.83696 4.26339 3.20107 4.73223 2.73223C5.20107 2.26339 5.83696 2 6.5 2Z"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill={active ? `${color}20` : "none"}
      />
    </svg>
  );
}

function SettingsIcon({ active }: { active: boolean }) {
  const color = active ? "var(--color-primary)" : "var(--text-secondary)";
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <path
        d="M12.22 2H11.78C11.2496 2 10.7409 2.21071 10.3658 2.58579C9.99072 2.96086 9.78 3.46957 9.78 4V4.18C9.77964 4.53073 9.69044 4.87519 9.52121 5.17884C9.35198 5.48248 9.10898 5.73464 8.81 5.91L8.35 6.18C8.04925 6.35341 7.70869 6.44418 7.36179 6.44334C7.01489 6.4425 6.6748 6.35007 6.375 6.175L6.22 6.085C5.76297 5.82101 5.22334 5.74317 4.71328 5.86718C4.20322 5.99119 3.76206 6.30771 3.48 6.755L3.26 7.135C2.97723 7.58176 2.87785 8.11887 2.98219 8.63464C3.08653 9.1504 3.38651 9.60451 3.82 9.895L3.97 9.995C4.2669 10.1716 4.50754 10.4241 4.67478 10.7273C4.84201 11.0306 4.92975 11.3741 4.92975 11.7238V12.2763C4.92975 12.6259 4.84201 12.9694 4.67478 13.2727C4.50754 13.5759 4.2669 13.8284 3.97 14.005L3.82 14.105C3.38651 14.3955 3.08653 14.8496 2.98219 15.3654C2.87785 15.8811 2.97723 16.4182 3.26 16.865L3.48 17.245C3.76206 17.6923 4.20322 18.0088 4.71328 18.1328C5.22334 18.2568 5.76297 18.179 6.22 17.915L6.375 17.825C6.6748 17.6499 7.01489 17.5575 7.36179 17.5567C7.70869 17.5558 8.04925 17.6466 8.35 17.82L8.81 18.09C9.10898 18.2654 9.35198 18.5175 9.52121 18.8212C9.69044 19.1248 9.77964 19.4693 9.78 19.82V20C9.78 20.5304 9.99072 21.0391 10.3658 21.4142C10.7409 21.7893 11.2496 22 11.78 22H12.22C12.7504 22 13.2591 21.7893 13.6342 21.4142C14.0093 21.0391 14.22 20.5304 14.22 20V19.82C14.2204 19.4693 14.3096 19.1248 14.4788 18.8212C14.648 18.5175 14.891 18.2654 15.19 18.09L15.65 17.82C15.9508 17.6466 16.2913 17.5558 16.6382 17.5567C16.9851 17.5575 17.3252 17.6499 17.625 17.825L17.78 17.915C18.237 18.179 18.7767 18.2568 19.2867 18.1328C19.7968 18.0088 20.2379 17.6923 20.52 17.245L20.74 16.865C21.0228 16.4182 21.1222 15.8811 21.0178 15.3654C20.9135 14.8496 20.6135 14.3955 20.18 14.105L20.03 14.005C19.7331 13.8284 19.4925 13.5759 19.3252 13.2727C19.158 12.9694 19.0703 12.6259 19.0703 12.2763V11.7238C19.0703 11.3741 19.158 11.0306 19.3252 10.7273C19.4925 10.4241 19.7331 10.1716 20.03 9.995L20.18 9.895C20.6135 9.60451 20.9135 9.1504 21.0178 8.63464C21.1222 8.11887 21.0228 7.58176 20.74 7.135L20.52 6.755C20.2379 6.30771 19.7968 5.99119 19.2867 5.86718C18.7767 5.74317 18.237 5.82101 17.78 6.085L17.625 6.175C17.3252 6.35007 16.9851 6.4425 16.6382 6.44334C16.2913 6.44418 15.9508 6.35341 15.65 6.18L15.19 5.91C14.891 5.73464 14.648 5.48248 14.4788 5.17884C14.3096 4.87519 14.2204 4.53073 14.22 4.18V4C14.22 3.46957 14.0093 2.96086 13.6342 2.58579C13.2591 2.21071 12.7504 2 12.22 2Z"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill={active ? `${color}20` : "none"}
      />
      <path
        d="M12 15C13.6569 15 15 13.6569 15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15Z"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
