---
name: implementation-options
description: Suggest three distinct ways to implement a requested feature, compare their pros and cons, and recommend the best fit for the application's architecture and constraints. Use when the user asks for feature implementation suggestions, approaches, or tradeoffs; do not automatically delay a direct implementation request with an options exercise.
---

# Implementation Options

Provide three concrete implementation approaches for the requested feature and an evidence-based recommendation for the current application. Interpret “pros and cons” as the tradeoffs of each implementation approach.

## When to use it

Use this skill when the user asks for:

- Suggestions for how to implement a feature.
- Alternative implementation approaches and their pros and cons.
- A recommendation on which approach best fits the current application.

Example requests include “Suggest ways to add notifications” and “Compare approaches for implementing search and recommend the best fit for this app.”

Do not automatically apply this comparison workflow to a direct request to implement an already chosen approach, unless the user also asks for alternatives.

## Establish application context

Read applicable repository instructions and inspect the relevant parts of the application: dependencies, existing feature patterns, data flow, and integration points. Keep inspection proportional to the feature. Use the user's stated requirements, scale, timeline, and deployment constraints; distinguish observed facts from assumptions.

If application context is unavailable, provide a provisional comparison with explicit assumptions. Ask only for missing information that could materially change the recommendation. Never invent file paths, existing capabilities, performance measurements, or application requirements.

## Develop three alternatives

By default, present exactly three meaningfully different approaches that solve the same feature requirements. Respect an explicit request for a different number or a specific technology.

Choose differences that affect engineering decisions, such as extending existing architecture, adopting a library or service, or introducing a dedicated component. Do not force these categories when other alternatives fit better. Avoid cosmetic variations and deliberately weak options included just to make the recommendation look better. If fewer than three approaches satisfy a hard constraint, explain that honestly and identify what constraint would need to change for each remaining alternative.

For each approach, include:

- **How it works:** Concrete components, data flow, and where it integrates with the application. Name relevant existing files or modules when verified.
- **Pros:** Benefits specific to this feature and application.
- **Cons:** Material costs, limitations, failure modes, and maintenance burden.
- **Effort and fit:** Relative implementation effort, key dependencies or migration work, and the conditions under which this approach is a good fit. Avoid unsupported time estimates.

Compare the approaches against the same relevant criteria. Prioritize compatibility with current architecture, implementation complexity, maintainability, user experience, and operational cost; include performance, security, privacy, scale, or vendor dependency when they affect this decision. Verify current third-party capabilities or pricing before relying on them.

## Recommend

Lead with the recommended approach and the main reason it fits this application. Follow with a compact comparison table or three short option descriptions; use enough explanation to make the implementations concrete.

Explain why the recommendation beats the other two under the known constraints, the main tradeoff being accepted, and what change in requirements would favor another approach. Prefer proportional solutions; do not assume future scale justifies additional architecture.