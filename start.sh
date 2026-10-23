#!/bin/sh
# Lost & Found - Smart Campus Asset Recovery Platform
# Run:  ./start.sh     (needs Java 17 or newer)
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
    echo "Java was not found. Please install Java 17 or newer (https://adoptium.net)."
    exit 1
fi

echo "Starting Lost & Found at http://localhost:8080 ..."
(sleep 3 && (xdg-open http://localhost:8080 >/dev/null 2>&1 || open http://localhost:8080 >/dev/null 2>&1)) &
java -jar lost-and-found.jar
