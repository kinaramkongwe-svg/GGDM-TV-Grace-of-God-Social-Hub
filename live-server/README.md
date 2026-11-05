# GGDM TV independent live server

This is the first independent media-server layer for GGDM TV. It does not use LiveKit Cloud.

## Data path

Android camera / encoder -> RTMP ingest (MediaMTX) -> RTSP internal source -> FFmpeg tee relay -> Facebook Live + YouTube Live.

The relay copies the incoming video stream to both destinations, avoiding a second camera capture. Audio is normalized to AAC for platform compatibility.

## Server requirements

Deploy on a Linux VPS with Docker Engine and the Compose plugin. Allow inbound TCP 1935 for RTMP ingest. Keep RTSP 8554 and HLS 8889 private to the Docker network/firewall; they are not needed from the public internet. Use a stable public IP or DNS name and sufficient upstream bandwidth for the incoming stream plus the two outgoing platform feeds.

## Configure

1. Copy `.env.example` to `.env`.
2. Set a long random alphanumeric `GGDM_INGEST_PASSWORD`.
3. Add the complete Facebook and YouTube RTMPS publish URLs, including stream keys, to `.env`.
4. Run `docker compose up -d`.
5. Configure the Android publisher to send to `rtmp://YOUR_SERVER:1935/ggdm-live` with the configured ingest username and password (the app must URL-encode credentials if it uses URL parameters).
6. Confirm MediaMTX logs show the publisher connected, then confirm both platforms receive the feed.

## Important

- This is the media relay foundation, not a deployed server. It needs a VPS and platform stream destinations before a real broadcast can be tested.
- Keep `.env` private. Never put platform stream keys in Android source code or commit them to GitHub.
- Facebook and YouTube must each have an active/ready live broadcast destination. OAuth provisioning and secure destination refresh are a separate backend integration step.
- The tee relay is configured to tolerate one destination failing while keeping the other output alive. Monitor logs and test both destinations before production use.
- RTMP ingest uses a password-protected MediaMTX path. Restrict public ports at the firewall and use TLS/VPN or a trusted private network for administration.
