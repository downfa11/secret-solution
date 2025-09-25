#!/bin/bash
set -e

/root/go-secrets-api &
API_PID=$!

trap "echo 'Stopping API...'; kill $API_PID; wait $API_PID; exit 0" SIGTERM SIGINT

while true; do
    sleep 1
done
