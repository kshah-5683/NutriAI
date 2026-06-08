# React Components (`webapp/components/`)

This directory contains the reusable React components, dashboard sections, modals, drawers, and context wrappers that construct the user interface of the web companion application.

## 🎯 Major Function & Purpose

The `components` folder houses the entire presentational layer of the frontend. It translates state stores and TanStack Query hooks into interactive visual layouts (like macro dashboard rings, AI parsing logs, manual recipe builders, and insights charts), ensuring modular and reusable layouts across screen templates.

---

## 📂 Subdirectories

* **[`insights/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/insights)**: Visual charts and historical progress analytics modules.
* **[`providers/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/providers)**: React Context containers that initialize and inject Supabase clients and Query Client caching engines.
* **[`ui/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/ui)**: Pure, stateless design system primitives (buttons, dialogs, inputs, cards) styled with Tailwind CSS.

---

## 📂 Top-Level Components

### Dashboard & Navigation
* **[`dashboard-shell.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/dashboard-shell.tsx)**: The main layout structure containing sidebars, page headers, and user navigation menus.
* **[`bottom-nav.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/bottom-nav.tsx)**: Navigation bar for mobile viewports.
* **[`date-navigation-header.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/date-navigation-header.tsx)**: Controls to paginate or jump between calendar tracking dates.
* **[`macro-summary-card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/macro-summary-card.tsx)**: Displays the daily progress dashboard with visual macro goal status rings (Calories, Protein, Carbs, Fat).

### Food Logging Interface
* **[`input-mode-tabs.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/input-mode-tabs.tsx)**: Tab switches to toggle food entry methods (AI, Scan, Manual).
* **[`ai-input-section.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/ai-input-section.tsx)**: Text interface for natural language logging and displaying temporary parsed entries.
* **[`scan-input-section.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/scan-input-section.tsx)**: Drag-and-drop panel to upload and scan food labels via vision processing.
* **[`manual-input-section.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/manual-input-section.tsx)**: Form fields for manual logging or compiling custom, multi-ingredient recipes.
* **[`manual-recipe-ingredient-row.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/manual-recipe-ingredient-row.tsx)**: Individual rows for inputting, searching, and calculating macros per recipe ingredient.
* **[`meal-type-selector.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/meal-type-selector.tsx)**: Selector to assign entries to target meal categories.
* **[`clarification-input.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/clarification-input.tsx)**: Secondary question prompts resolving ambiguities during AI logs.
* **[`macro-preview-card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/macro-preview-card.tsx)**: Inline summary of macros before logging items.
* **[`parsed-food-card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/parsed-food-card.tsx)**: Temporary cards displaying parsed foods with status details and macro breakdowns.

### Lists & Recommendation Cards
* **[`food-log-list.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/food-log-list.tsx)**: Grouped daily log records categorized by meal slot.
* **[`food-log-item.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/food-log-item.tsx)**: A single logged food occurrence displaying serving amounts, scaled macros, and management actions.
* **[`catalog-food-card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/catalog-food-card.tsx)**: Catalog item card providing quick-log triggers.
* **[`recommendation-card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/recommendation-card.tsx)**: Suggests meals and recipes customized to remaining macro limits, prioritizing saved catalog options.

### Sheets & Confirm Dialogs
* **[`edit-log-sheet.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/edit-log-sheet.tsx)**: Slide-over editor sheet to modify portion sizes or serving units of logged foods.
* **[`edit-food-sheet.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/edit-food-sheet.tsx)**: Slide-over editor sheet to edit catalog food descriptors and base nutrition facts.
* **[`confirm-delete-dialog.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/confirm-delete-dialog.tsx)**: Confirmation dialog to delete a food diary log entry.
* **[`confirm-delete-food-dialog.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/confirm-delete-food-dialog.tsx)**: Confirmation dialog to delete a catalog item.

### Theming
* **[`theme-init-script.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/theme-init-script.tsx)**: Injected blocking script resolving dark mode values prior to component rendering to eliminate layout flashes.
* **[`theme-selector.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/theme-selector.tsx)** & **[`theme-toggle-button.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/theme-toggle-button.tsx)**: Controls to set UI themes (light, dark, or automatic system modes).

---

## 🔌 External Dependencies

* **`react`** & **`react-dom`**: Core user interface rendering library.
* **`recharts`** (indirectly via `insights/`): Data visualization charting library.
* **Tailwind CSS**: styling and utility classes.
