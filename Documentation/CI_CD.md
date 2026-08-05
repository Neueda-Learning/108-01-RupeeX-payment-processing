# Jenkins CI/CD setup

This document describes how to wire Jenkins to automatically build, publish and deploy this project when changes are pushed to `main`.

Prerequisites
- A Jenkins server with Docker and Maven installed (agent/node that runs builds must have Docker CLI and Docker daemon access).
- Jenkins plugins: *Pipeline*, *Git*, *GitHub*, *Credentials Binding*, *SSH Agent* (or use `sshUserPrivateKey` credential), *Docker Pipeline* (optional).
- A Docker registry account (Docker Hub or private registry) and credentials stored in Jenkins.
- A target host where the service will be deployed, with Docker Engine and `docker compose` available.

Recommended Jenkins credentials
- `DOCKERHUB_CREDENTIALS` — Username/password credential for Docker registry.
 - `REGISTRY_CREDENTIALS` — Username/password credential for your private Docker registry (used by the pipeline).
 - `REGISTRY_CREDENTIALS` — Username/password credential for your private Docker registry (used by the pipeline).
 - `SSH_CREDENTIALS` — SSH key credential used for direct image transfer and remote deploy (already documented).
- `SSH_CREDENTIALS` — SSH private key credential for the remote deploy user (set username in the credential or provide in job env).

Pipeline (Jenkinsfile)
- The repository includes a `Jenkinsfile` that:
  - checks out source
  - runs `mvn clean package`
  - builds and tags a Docker image
  - pushes the image to the configured Docker registry
  - SSHes to the remote host and runs `docker compose pull` and `docker compose up -d`

How to configure
1. Create a new Pipeline job in Jenkins and point it to your Git repository (branch `main`).
2. Add the Jenkins credentials mentioned above.
3. Configure the job's Pipeline to use the `Jenkinsfile` from SCM.
4. Set these environment variables in the job configuration (or in a Jenkins folder/global config):
   - `DOCKER_IMAGE` — e.g. `your-dockerhub-username/rupeex-app`
   - `DOCKER_REGISTRY` — (optional) registry host, default `docker.io`
    - For private registries set `DOCKER_REGISTRY` to `registry.example.com` and `DOCKER_IMAGE` to `namespace/rupeex-app` (the pipeline constructs the full image name).

  - Note about deploy flow
   - The pipeline can operate in two modes controlled by the `PUSH_TO_REGISTRY` environment variable in the Jenkins job:
     - `PUSH_TO_REGISTRY=true`: CI builds and pushes the image to the configured registry (`REGISTRY_CREDENTIALS` required). Remote host pulls via `docker compose pull`.
     - `PUSH_TO_REGISTRY=false`: CI builds the image, `docker save`s it to `image.tar`, SCPs it to the remote host and runs `docker load` there. This avoids any registry but requires SSH access and frees you from publishing the image.

  Local on-host deployment (no registry)
  - The default `Jenkinsfile` now supports a simple flow that runs entirely on the Jenkins agent (recommended for single-host setups):
    - Checkout source on push
    - `docker compose down`
    - `docker compose build --no-cache`
    - `docker compose up -d`
  - This mode does NOT require a registry or SSH transfers. Ensure the Jenkins agent that runs the job is the deploy host and has `docker` and `docker compose` installed and usable by the Jenkins user.

  - The repo contains `docker-compose.prod.yml` which references `${IMAGE_FULL}:${IMAGE_TAG}`. The pipeline constructs `IMAGE_FULL=${DOCKER_REGISTRY}/${DOCKER_IMAGE}` and sets `IMAGE_TAG` (build number + commit) before pushing. During deploy the pipeline exports `IMAGE_FULL` and `IMAGE_TAG` for the remote `docker compose` invocation so the remote host pulls the exact pushed image.
   - `REMOTE_HOST` — IP or hostname of the server to deploy to
   - `REMOTE_USER` — user on the remote host (must have Docker permissions)
   - `REMOTE_DOCKER_COMPOSE_PATH` — path on the remote host containing `docker-compose.prod.yml` (the pipeline will copy the `docker-compose.prod.yml` file there)

Triggering from GitHub
- Install GitHub webhook: In your GitHub repo settings → Webhooks, add a webhook pointing to `http://<JENKINS_HOST>/github-webhook/` and select push events. Alternatively, use the *GitHub Branch Source* plugin and GitHub App for more advanced integration.

On the remote server
- Create a directory (e.g. `/opt/rupeex`) and ensure the deploy user can write to it.
- Place a `.env` file there (the pipeline will copy `docker-compose.prod.yml` but not your `.env` — you should manage secrets securely on the host).
- The `docker-compose.prod.yml` in this repo will use the pushed image to start the `app` service.

Security notes
- Do NOT store secrets in the repo. Use Jenkins credentials and `.env` on the target host.
- Consider using Docker registry access tokens and rotate them periodically.
- For production secrets, consider using Docker secrets or a secret manager and update the compose file accordingly.
