#!/bin/sh
set -eu

SOURCE="rtsp://relay-reader:${GGDM_INGEST_PASSWORD}@mediamtx:8554/ggdm-live"

echo "GGDM relay waiting for the Android studio stream..."
while :; do
  if [ -z "${FACEBOOK_RTMP_URL:-}" ] || [ -z "${YOUTUBE_RTMP_URL:-}" ]; then
    echo "Set both FACEBOOK_RTMP_URL and YOUTUBE_RTMP_URL in .env before going live."
    sleep 10
    continue
  fi

  echo "Starting independent GGDM fan-out to Facebook and YouTube."
  ffmpeg -hide_banner -loglevel info \
    -rtsp_transport tcp -i "$SOURCE" \
    -map 0:v:0 -map '0:a:0?' \
    -c:v copy -c:a aac -b:a 128k -ar 44100 \
    -f tee "[f=flv:onfail=ignore]${FACEBOOK_RTMP_URL}|[f=flv:onfail=ignore]${YOUTUBE_RTMP_URL}" || true

  echo "Stream ended or relay disconnected; retrying in 3 seconds."
  sleep 3
done
