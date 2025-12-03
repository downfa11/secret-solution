# Stage 1: Build both API and CLI executables
FROM golang:1.25 AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-s -w" -o /app/go-secrets-api ./cmd/api
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-s -w" -o /app/go-secrets-cli ./cmd/cli

# Stage 2: Production image
FROM alpine:3.18

WORKDIR /root/
COPY --from=builder /app/go-secrets-api .
COPY --from=builder /app/go-secrets-cli .

RUN apk add --no-cache bash curl

EXPOSE 8080
EXPOSE 50051

COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

ENTRYPOINT ["/root/entrypoint.sh"]
CMD []
