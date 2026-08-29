# Memory Lane — Setup Guide (for beginners)

This is a working Android Studio project. It lets you:
1. Create a **project** (a name + a folder on your phone).
2. See only the photos/videos inside that folder, in a grid.
3. Tap any photo/video to view it full-screen (video plays inline).

## How to open this in Android Studio

1. **Unzip** this folder anywhere on your computer.
2. Open **Android Studio** → `File > Open` → select the unzipped `MemoryLaneApp` folder (the one containing `settings.gradle`).
3. Let Gradle sync (it will download dependencies the first time — needs internet).
4. If it asks about the Gradle version / Android Gradle Plugin, click "Update" / "OK" — it's already configured for recent versions (AGP 8.5.1, Gradle 8.7, Kotlin 1.9.24), but Android Studio may offer a newer compatible one.
5. Plug in your phone (with USB debugging on) or start an emulator, then click **Run ▶**.

## How it works, file by file

- `data/Project.kt` — the Room database "row": project name + folder location.
- `data/AppDatabase.kt`, `data/ProjectDao.kt`, `data/ProjectRepository.kt` — local storage for your list of projects (survives app restarts).
- `data/MediaScanner.kt` — reads a folder's photos/videos using Storage Access Framework (`DocumentFile`). This is what makes "only show this folder's photos" work.
- `model/MediaItem.kt` — one photo or video.
- `ui/MainActivity.kt` — home screen, list of your projects, "+" button to add one.
- `ui/CreateProjectActivity.kt` — name field + "Choose Folder" button (opens Android's built-in folder picker).
- `ui/GalleryActivity.kt` — the grid of photos/videos for a chosen project.
- `ui/MediaViewerActivity.kt` — full-screen viewer (images via Glide, video via ExoPlayer/media3).

## Important notes

- **No storage permission needed.** Because folders are picked through the system's folder picker (Storage Access Framework), Android automatically grants your app permanent read access to just that folder — nothing broader. This is the modern, Play-Store-friendly way to do this (works from Android 8 / minSdk 26 up).
- **Folders don't auto-refresh in real time.** If you add new photos to a folder after opening a project, just leave and re-enter the project screen — it re-scans each time it opens.
- **Sub-folders are not scanned.** Each project maps to one flat folder. If you want sub-folder support, `MediaScanner.scan()` is the place to make it recursive.
- This uses **view binding**, so if you rename/add layout files, just rebuild — Android Studio auto-generates the `...Binding` classes, you don't write them.

## Where to take this next ("AI-powered" ideas)

You mentioned wanting this to be AI-powered — this base app is the scaffolding (folder-scoped projects + gallery + viewer). Natural next steps once this is running:
- **On-device face/scene grouping** using Google's ML Kit (free, runs offline) to auto-cluster photos within a project.
- **Auto-captioning / trip summaries** by sending a batch of photos (or their EXIF data) to a multimodal API.
- **Duplicate/blurry photo detection** to help you curate before making a project.

Happy to help build any of these once the base app is running for you.
