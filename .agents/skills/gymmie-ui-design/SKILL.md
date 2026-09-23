---
name: gymmie-ui-design
description: Design and refine Gymmie's JavaFX screens with a coherent visual direction, readable typography, and thoughtful interactions. Use for Gymmie UI design and visual implementation requests; not for backend-only work or behavior fixes with no visual design changes.
---

# Gymmie UI Design

Create a distinctive, usable desktop experience for gym facility management and workout tracking across Manager, Trainer, and Gym User roles. Let the task, content, and existing application guide the design. Exercise creative judgment rather than following a fixed visual formula.

## Quick Start

- Understand the screen's purpose, audience, primary actions, and desktop constraints. Inspect the relevant FXML, stylesheet, controller, and assets.
- Choose a coherent aesthetic direction and, where useful, a memorable gesture: an editorial hierarchy, a strong activity rail, restrained depth, or an expressive progress display.
- Establish reusable typography, palette, spacing, radii, and effects in the implementation. Load any custom fonts early.
- Plan the content hierarchy, resizable layout, and relevant interaction states.
- Build within JavaFX, then check usability, accessibility, and visual quality.

Adapt this workflow to the scope. A small visual adjustment should stay small.

## Workflow

### 1) Understand intent and choose a direction

- Read applicable repository instructions and inspect the current implementation. Identify the target role, workflow, content density, and existing states without assuming screens or features exist.
- Infer tone and visual direction from the request and application context. Ask only when a missing requirement would materially change the result; otherwise choose a sensible direction and proceed.
- Use the existing design language as context. Refine it for focused edits, and explore a stronger direction when the request calls for a redesign.
- Keep the composition coherent. Distinctiveness can come from typography, spacing, information layout, color, or a well-chosen detail; decorative effects are optional.

### 2) Plan structure and styling

- Organize the screen around the user's primary task. Establish a clear hierarchy for navigation, actions, forms, records, and feedback.
- Choose fonts and a type scale that suit the content. A versatile family or a complementary pairing can work; use judgment rather than a prescribed font list. Keep dense records and numeric data readable.
- Define reusable styling for surfaces, text, actions, borders, focus, and relevant status feedback. Prefer a restrained accent strategy while allowing the chosen aesthetic to guide the palette.
- Plan spacing, resizing, and interaction states together so the design remains useful with real content, long labels, and constrained window sizes.
- Make these choices as part of implementation. Do not require a pre-code design brief, font rationale, size inventory, hex-code table, or approval checkpoint. Explain exact design values only when the user asks or a concrete decision needs their input.

### 3) Build and refine

- Use JavaFX layout containers, FXML, and JavaFX CSS. Define palette tokens with looked-up colors and reuse style classes; browser CSS variables, web font imports, and web layout APIs do not apply.
- Preserve controller bindings, field identifiers, and service behavior unless the requested change requires otherwise. Keep shared visual conventions consistent across roles.
- Inspect existing font assets before adding more. When using custom fonts, bundle redistributable files with their license notices, load them with `Font.loadFont` before styling, and verify the actual family names and successful loading. Choose a reliable fallback where needed.
- Use layout constraints, wrapping, and appropriate scrolling to support a resizable desktop window. Let content determine when cards, rails, tables, or simpler groupings are useful.
- Add depth, texture, or motion when it strengthens hierarchy or feedback. Keep JavaFX transitions subtle and inexpensive, respect available reduced-motion preferences, and never make essential information depend on animation.
- Preserve service-level RBAC and account ownership rules. Visible or disabled controls communicate permissions but do not enforce them.

### 4) Polish and verify

- Check actual font rendering, content fit, window resizing, and the affected hover, focus, pressed, selected, disabled, validation, empty, and success states as applicable.
- Preserve logical keyboard traversal, visible focus, input labels, and accessible names. Pair status colors with text or recognizable symbols.
- Check contrast for the foreground/background pairs being changed: aim for at least 4.5:1 for ordinary text and 3:1 for large text and meaningful control boundaries or focus indicators.
- Keep effects and animations responsive with realistic data. Run repository checks appropriate to the changes and inspect the running JavaFX UI when available.
- Report what changed and the verification performed, with any concrete limitations. Keep the report focused on the result; do not routinely list fonts, sizes, hex codes, or their rationale. State when visual checks could not be performed.
