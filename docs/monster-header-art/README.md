# Monster header images

The Android app currently packages one final WebP header for each of its 40 large monsters in `app/src/main/assets/monster-headers/`.

When maintaining these images, use the monster ID as the filename (for example, `monster_agnaktor.webp`) and provide artwork you have permission to use. The app places the image behind the existing header content, fades it toward the text side, and applies a subdued treatment for readability. Images are packaged locally; the app does not download artwork.

The source images and local crop decisions are intentionally not part of this public repository. Only the final runtime assets are included.
