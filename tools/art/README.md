# Art pipeline

Starter avatars, frames and the launcher icon are drawn as SVG/CSS and rendered to PNG with
headless Chromium (Playwright), so they can be previewed and iterated without Android.

    python3 tools/art/build_avatars.py app/src/main/assets/art      # avatar_0..5.png
    python3 tools/art/build_frames.py  app/src/main/assets/art      # frame_0..7.png
    python3 tools/art/icon.py          app/src/main/res/drawable-nodpi  # adaptive icon layers

Frames keep a transparent circular opening of radius 190px (of 512); the app measures it
automatically (`AvatarRender.holeRatio`) and clips the avatar inside it.
