# Frontend multi-stage build for Vue (Vite)
FROM node:20-alpine AS build
WORKDIR /app
# Install deps
COPY package*.json yarn.lock* pnpm-lock.yaml* ./
RUN npm install --no-audit --no-fund
# Copy source
COPY . .
# Build static assets (allow build-time env for Vite)
ARG VITE_GRAPHQL_BASE
ENV VITE_GRAPHQL_BASE=${VITE_GRAPHQL_BASE}
RUN echo "Building with VITE_GRAPHQL_BASE=$VITE_GRAPHQL_BASE" && npm run build

# Runtime stage
FROM nginx:1.25-alpine
# Remove default config and add our SPA config
RUN rm /etc/nginx/conf.d/default.conf || true
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
