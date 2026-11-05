# GGDM TV — Native Android

Grace of God Deliverance Ministry's Android broadcast studio.

## Build
This repository includes a GitHub Actions workflow that builds a debug APK on every push and manual run. Open **Actions → Build GGDM TV APK** and download the artifact when the workflow completes.

## Current scope
- Native Android camera preview and RTMP publishing foundation
- Front/rear camera switching
- Stream URL and stream-key input
- Programme title, lower-third and ticker fields
- 720p/30fps streaming defaults

This is an initial native broadcaster foundation. Automatic Facebook/YouTube OAuth destination creation, simultaneous multi-platform fan-out, composited graphics/transitions, and background-service hardening remain integration work. Do not treat those as implemented until tested.
