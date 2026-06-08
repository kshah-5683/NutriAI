# Insights & Analytics Components (`webapp/components/insights/`)

This directory contains visual modules and charting components used to display historical macronutrient intake, progress trends, and nutritional analytics.

## 🎯 Major Function & Purpose

The `insights` folder houses the UI representation layers for user analytics. These components ingest pre-aggregated progress data (from custom hooks querying historical daily logs) and render interactive bar, line, and monthly summary charts. They mirror the data visuals from the Android application (e.g., progress lines, daily breakdown blocks) to allow users to review their nutritional metrics over long periods (7 days, 30 days, or 1 year).

---

## 📂 Components

* **[`daily-average-card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/insights/daily-average-card.tsx)**: Displays a grid of summary cards showing the user's daily averages for calories, protein, carbs, and fat alongside achievement percentages relative to target goals.
* **[`macro-bar-chart.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/insights/macro-bar-chart.tsx)**: Renders a weekly stacked bar chart showing the relative proportions of protein, carbs, and fat logged per day.
* **[`macro-line-chart.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/insights/macro-line-chart.tsx)**: Renders a monthly trend line chart mapping daily intake lines (Protein, Carbs, Fat) alongside target goal threshold limits.
* **[`macro-year-chart.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/insights/macro-year-chart.tsx)**: Renders a yearly vertical bar chart displaying aggregated average calorie targets month-by-month.
* **[`period-selector.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/insights/period-selector.tsx)**: Provides interactive tab toggles for switching between analytics intervals (e.g., Week, Month, Year).

---

## 🔌 External Dependencies

* **`recharts`**: Lightweight charting library built on SVG elements for rendering responsive lines, bars, legends, and hover tooltips.
* **`react`**: Core rendering library.
