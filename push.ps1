param([string]$tag)

if (-not $tag) {
    Write-Host "Nhập tag"
    exit
}

docker build -t truongikpk/bookstore-promotion-service:$tag .
docker push truongikpk/bookstore-promotion-service:$tag

# .\push.ps1 v1.0.0
# ./mvnw compile