#!/usr/bin/env bash
set -euo pipefail

# Ensure we're in the script's directory
cd "$(dirname "$0")"

# Activate virtual environment if present
if [ -f "ai/bin/activate" ]; then
    . ai/bin/activate
fi

# Run preprocessing
cd data
python process.py
cd ..

# Start server (equivalent to: uvicorn main:app --reload)
<<<<<<< HEAD:AI_server/run.sh
exec uvicorn main:app --reload
=======
exec uvicorn main:app --reload --root-path /api/ai
>>>>>>> 8b5e0d6a6caf901670d17719b2845c67ec8459e4:AI/AI_server/run.sh


