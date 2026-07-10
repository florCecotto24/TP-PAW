#!/usr/bin/env sh
# Adds the Node.js binary installed by frontend-maven-plugin to PATH, then runs the given command.
# Usage: ./use-node.sh npm run dev
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
export PATH="$DIR/node:$PATH"
exec "$@"
