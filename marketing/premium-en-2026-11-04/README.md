# Anatomy Studio — English launch kit

## Deliverables

- `play-store/app-icon-512.png`: 512 × 512 PNG, square full-bleed listing icon.
- `play-store/feature-graphic-1024x500.png`: 1024 × 500 RGB PNG feature graphic.
- `play-store/01-explore.png` through `08-offline.png`: eight ordered 1080 × 1920 RGB screenshots.
- `anatomy-studio-launch-1080p.mp4`: 36-second, 1920 × 1080, 30 fps landscape trailer.
- `anatomy-studio-vertical-1080p.mp4`: 36-second, 1080 × 1920, 30 fps vertical companion.
- `app-icon-master-1024.png` and `banner-master-2048x1000.png`: larger design masters.
- `screenshot-collection-preview.jpg`: contact sheet for review.
- `remotion/`: editable React/Remotion project, local fonts, authentic screen captures, original music.
- `source/`: reproducible asset/audio layout scripts, asset inventory and creative prompts.

## Creative direction

Quiet engineering precision. Deep charcoal, warm ivory, pale mint, dimensional exploded-layer identity, drafting grids, large editorial typography. The first six seconds establish the question and reveal the product: **“You see a machine. Now see how it works.”** The rest moves through exploration, disassembly, simulation, learning, export, and an invitation to explore.

The soundtrack is an original synthesized instrumental score with pulses, chord movement and transition sounds. On-screen English copy carries the story without requiring narration. No licensed third-party song or stock music is included.

## Product inspection

Reviewed the local Kotlin/Compose source, app metadata, studio controls, local catalog, export implementation, and the project's saved screenshots. This is a feature and asset review, not a complete runtime QA, device compatibility test, or security audit.

Confirmed in source:

- App name: Anatomy Studio; positioning: interactive 3D technical atlas.
- Mechanical engineering content, rather than medical human anatomy.
- Explore, Cutaway, Explode, Simulate, and Learn modes.
- Local procedural 3D catalog; rotation/orbit controls and annotated explain points.
- Turbofan engines, robots, gearboxes and other engineered systems.
- PNG and SVG technical plate export.
- The core 3D collection works offline. Optional AI Lab requires a separately connected provider; the kit makes no claim that AI works offline or is included free.
- Current source lists 25 object specifications. Saved capture versions show smaller collections, so marketing copy deliberately avoids numerical catalog claims.

Screen contents are authentic saved project captures from `docs/screenshots`. Only OS bars are trimmed for the store layouts. Captures are not freshly taken from a release build. The generated engine artwork in the banner and opening shot is a promotional illustration; it is not represented as in-app rendering. No fictitious UI, reviews, rankings, awards, pricing, or availability claims have been added.

## Upload order

Use the numbered screenshot order. Upload the 512 icon and 1024 × 500 feature graphic to their respective listing fields. The PNGs are final upload files; do not upload the contact sheet.

Google Play preview video uses a YouTube URL. Upload the landscape MP4 to your YouTube account and use its supported video URL in Play Console. Nothing has been published or uploaded to any external account by this task.

Official specifications checked: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en

## Editing and rendering

In `remotion/`:

```powershell
npm ci
npm run studio
npm run render
npx remotion render src/index.tsx AnatomyVertical ../anatomy-studio-vertical-1080p.mp4 --codec h264 --crf 18 --concurrency 3
```

Remotion dependencies are pinned to 4.0.534. Every major scene lives in its own TSX file. The two master compositions are `AnatomyLaunch` and `AnatomyVertical`. The landscape Studio preview uses http://localhost:3017/AnatomyLaunch while the local preview server is running.

Original application source and existing launcher resources were left untouched. All new work is contained in this dated marketing directory.
