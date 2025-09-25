# Stage 1: Build both API and CLI executables
FROM golang:1.25 AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-s -w" -o /go-secrets-api ./cmd/api
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-s -w" -o /go-secrets-cli ./cmd/cli

# Stage 2: Create the final production image
FROM alpine:3.18

WORKDIR /root/
COPY --from=builder /go-secrets-api .
COPY --from=builder /go-secrets-cli .

EXPOSE 8080
EXPOSE 50051

ENTRYPOINT ["./go-secrets-api"]
CMD ["api"]
