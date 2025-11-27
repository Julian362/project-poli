FROM node:20-alpine
WORKDIR /app
COPY package.json tsconfig.json /app/
RUN npm install && npm cache clean --force
COPY src /app/src
RUN npm run build
ENV PORT=8090
EXPOSE 8090
CMD ["node", "dist/index.js"]
