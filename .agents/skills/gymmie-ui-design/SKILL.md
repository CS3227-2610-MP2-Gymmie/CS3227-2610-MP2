---
name: gymmie-ui-design
description: Establish Gymmie's visual direction before creating or restyling JavaFX screens. Requires a pre-code design brief, deliberate typography without overused default fonts, and an explicit semantic color palette. Use for Gymmie UI design and visual implementation requests; not for backend-only work or behavior fixes with no visual design changes.
---

# Gymmie UI Design

Replace automatic aesthetic choices with explicit design decisions before writing UI code. Design for a gym facility management and workout tracking desktop application, with Manager, Trainer, and Gym User roles. Prioritize readable operational data, clear actions, and a recognizable fitness identity.

## Establish the actual context

Read applicable repository instructions and inspect the target FXML, JavaFX stylesheet, controller, and any existing design brief or bundled fonts. Resolve paths from the current repository root. Starting points in Gymmie are:

- `src/main/resources/gymmie/view/MainWindow.fxml`
- `src/main/resources/gymmie/css/gymmie.css`
- `src/main/java/gymmie/MainController.java`
- `src/main/java/gymmie/App.java`
- `README.md` and `build.gradle`

Recheck the current implementation rather than treating this skill as a snapshot. Treat existing styling as a starting point, not a finished design system. Do not invent implemented screens, authentication, or permissions.

Identify the screen's primary role, task, data density, and existing states. Base design decisions on the requested workflow and its actual content. Keep shared typography and color semantics across roles.

## Mandatory design brief before code

Before the first Java, FXML, or CSS edit for a visual change, present a concise design brief in the conversation. Reading files and inspecting assets may happen first. The brief must include:

1. **Purpose and direction:** Name the target role and primary task. Describe the visual character in concrete terms and identify a distinguishing choice, such as strong typographic hierarchy, measured spacing, or a deliberate information layout. Avoid unsupported adjectives like “modern” or “premium.”
2. **Typography rationale:** Choose exact font families, real available weights, and sizes for headings, body, labels, and numeric data where relevant. Explain why their letterforms, density, and legibility suit this screen, how the families work together, and how fonts will be supplied.
3. **Color palette:** Provide a table of semantic token names, exact hex values, intended uses, and foreground/background pairings. Explain how the palette supports Gymmie's identity and separates primary actions from status feedback.
4. **Layout and interaction:** Specify content hierarchy, spacing, sizing behavior, and relevant focus, hover, pressed, disabled, validation, empty, and success states. For existing screens, distinguish what is being retained from what is changing.

Complete the brief before implementation; do not promise to select fonts or colors later. This is a design decision gate, not an approval gate. Continue with already requested implementation after presenting the brief unless a material requirement is missing. If the user requested design only, stop at the design deliverable.

For small visual changes, keep the brief short and reuse an existing compliant design system. State which typography and palette decisions carry forward and why. Do not use the skill to expand a narrow edit into an unrelated redesign. Explicit user font, palette, or brand choices take precedence over these defaults; identify the exception and apply the remaining requirements.

## Typography rules

- Do not choose `System`, `system-ui`, the JavaFX default font, Arial, Helvetica, Helvetica Neue, Roboto, Inter, Segoe UI, San Francisco/SF Pro, or Times New Roman as the designed typeface. Do not hide these choices behind generic sans-serif declarations or accidental fallback. This ban concerns authored visual choices, not characters rendered by unavoidable platform glyph fallback.
- Choose at most two intentional families. A single versatile family with justified weights is sufficient. Avoid novelty, stencil, or condensed display type for long text, form fields, and dense records.
- The rationale must connect the selection to actual content: long descriptions, form labels, readable names, or distinguishable numeric data. “Looks clean” is insufficient. Verify number alignment support when relying on it; do not assume every font provides tabular figures.
- Specify a practical scale in JavaFX logical pixels. Keep body and form text comfortably readable; begin around 14–16px and adapt to verified layout needs. Establish hierarchy through size, weight, and space instead of making every label bold.
- Use redistributable font files and retain their license notices. Inspect existing assets first; verify the source and license before adding a font. Prefer static TTF/OTF weights unless runtime support for another format has been verified. Do not name an unbundled font and assume it exists on the user's machine.
- Bundle required faces under a suitable classpath resource directory, such as `src/main/resources/gymmie/fonts/`, and load them with JavaFX `Font.loadFont` before applying the UI stylesheet. Use the actual family names reported by the loaded fonts. Verify loading succeeds and inspect the resulting UI for substitution; do not silently accept a return to `System` as completed design work.
- If a selected font cannot be obtained, choose a verified available non-banned alternative and revise the rationale. If none is available, report the concrete dependency instead of pretending the typography has been implemented.

## Color rules

Choose and commit to one coherent palette per design brief. Do not list several palettes and proceed without selecting one. Existing cream-and-green styling may be refined when justified; do not replace it merely to make the screen look different.

Define at least these semantic roles, including actual hex values:

- Canvas and surface backgrounds.
- Primary text and muted text.
- Borders or dividers.
- Primary action background and its foreground.
- Keyboard focus indicator.
- Success, warning, and error feedback, with explicit backgrounds where used.

Specify hover, pressed, selected, and disabled variants for the controls being designed. Some tokens may deliberately share a value; explain reuse when it could blur meanings. Do not scatter new hex literals through individual controls without assigning them a palette role.

Use a restrained accent strategy. Avoid automatic purple gradients, neon-on-black gym styling, glass effects, and unrelated multicolor cards unless the chosen direction has a concrete reason for them. Distinctiveness must come from the screen's needs and the selected visual system.

Calculate contrast for the actual foreground/background pairs. Target at least 4.5:1 for ordinary text, 3:1 for large text, and 3:1 for meaningful control boundaries and focus indicators against adjacent colors. Do not label the design accessible on the basis of palette inspection alone. Pair status colors with text or recognizable symbols; role and permission distinctions must not depend on color alone.

## Implement within JavaFX and Gymmie's boundaries

- Use JavaFX CSS properties and looked-up colors defined at the root for palette tokens. Do not introduce browser CSS variables, web font imports, HTML, Tailwind, or a new frontend stack.
- Keep reusable styles in the existing stylesheet and structural layout in FXML. Preserve controller bindings, field identifiers, label associations, and service behavior unless the requested work requires changing them.
- Design for a resizable desktop window. Use layout constraints and wrapping rather than absolute positioning. Check long labels and text, constrained window sizes, and keyboard traversal. Avoid decorative card nesting that wastes space needed for forms or records.
- Make role-appropriate actions clear while preserving service-level RBAC. Hiding or disabling a control does not enforce authorization. A visual refresh must not add permission-switching controls or weaken account ownership rules. Preserve honest demo and persistence messaging where applicable.
- Preserve the target screen's existing interaction and feedback states. Do not let status text disappear against the new colors or styling obscure whether fields are editable.

## Verify and report

When implementation is requested, run the repository checks appropriate to the changes. Inspect the running JavaFX UI when available to confirm actual font rendering, layout, contrast pairings, focus visibility, and the affected states. Build success alone does not verify visual quality. If GUI verification is unavailable, say which visual checks remain unverified.

Report the chosen typography and palette, what was implemented, and verification limits. Do not claim fonts were bundled, contrast was calculated, or screens were inspected unless those actions were completed. Do not commit or push unless explicitly requested.
