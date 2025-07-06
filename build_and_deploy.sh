#!/bin/bash

echo "🛠️ Checkout develop branch và cập nhật code mới nhất"
git checkout develop
git reset --hard origin/develop
git pull origin develop

echo "🐳 Dừng container cũ (nếu có)"
docker compose down

echo "🐳 Build và chạy lại container mới"
docker compose up --build -d

echo "✅ Deploy hoàn thành!"
