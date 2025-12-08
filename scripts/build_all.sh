#!/bin/bash

services=(
  gateway
  user
  activity
  content
  interaction
  file
  admin
  web-ui
)

for s in "${services[@]}"; do
  docker build -f sword-shield-$s/Dockerfile -t ss-$s:latest .
done