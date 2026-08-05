FROM node:18-alpine
WORKDIR /app
COPY package.json tsconfig.json ./
COPY src ./src
COPY knowledge ./knowledge
RUN npm install
RUN npm run build
EXPOSE 4001
CMD ["node", "dist/index.js"]
