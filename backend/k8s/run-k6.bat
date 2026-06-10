@echo off
echo ==============================================
echo Running k6 Load Test using Docker
echo Make sure Docker Desktop is running!
echo ==============================================
docker run --rm -i grafana/k6 run - < load-test.js
pause
