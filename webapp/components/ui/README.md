# Reusable UI Primitives (`webapp/components/ui/`)

This directory houses the low-level, reusable design system components used to build layout screens in the web application.

## 🎯 Major Function & Purpose

The `ui` folder acts as the local UI library (design system primitives) for the application. These components are kept stateless, customizable, and agnostic of business logic. They represent the foundational design elements (buttons, inputs, cards, dialogs) styled with Tailwind CSS, ensuring visual consistency across all feature modules.

---

## 📂 UI Components

* **[`button.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/ui/button.tsx)**: A customizable button component with support for different visual styles (primary, outline, ghost) and loading states.
* **[`card.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/ui/card.tsx)**: A structural layout card with header, body, and footer containers.
* **[`dialog.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/ui/dialog.tsx)**: A modal dialog wrapper backed by the native HTML `<dialog>` element, supporting overlay blurring, escape-key closing, and click-outside closing.
* **[`input.tsx`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/components/ui/input.tsx)**: A stylized text input field with standard focus, error, and placeholder states.

---

## 🔌 External Dependencies

* **`react`**: Core rendering library.
* **Tailwind CSS**: Utility-first CSS classes for layout and visual styling.
