#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <tag> (e.g. v1.0.0)"
  exit 1
fi

git tag "$1" && git push origin "$1"
