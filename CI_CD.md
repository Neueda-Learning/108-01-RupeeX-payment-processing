# Jenkins CI/CD setup

This document describes how to wire Jenkins to automatically build, publish and deploy this project when changes are pushed to `main`, and how GitHub Actions gates pull requests on tests passing before they can be merged.

## GitHub Actions: tests required before merge

The workflow at [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) runs on every push to `main` and on every pull request targeting `main`. It runs three independent jobs in parallel:

- `backend-tests` — `./mvnw test` for the `backend/` Spring Boot service (against the in-memory H2 database used by `backend/src/test/resources/application.properties`, so no external MySQL is needed in CI).
- `onboarding-service-tests` — `mvn test` for `onboarding-service/`.
- `frontend-checks` — `npm ci`, `npm run lint`, and `npm run build` for `frontend/`.

A final `ci-success` job depends on all three and fails if any of them failed or were cancelled, giving you one stable status check name (`CI Success`) to require.

### Enforcing "tests must pass before merge"

GitHub Actions running the workflow is not enough on its own — by default a PR can still be merged even if a check fails, unless a **branch protection rule** requires it. To turn that on (requires repo admin access; there is no way to do this from a workflow file):

1. On GitHub, go to the repository's **Settings → Branches → Branch protection rules** and add/edit the rule for `main`.
2. Enable **"Require a pull request before merging"**.
3. Enable **"Require status checks to pass before merging"**, then search for and select:
   - `Backend tests (Spring Boot)`
   - `Onboarding service tests (Spring Boot)`
   - `Frontend lint & build (Next.js)`
   - `CI Success`
4. Enable **"Require branches to be up to date before merging"** so PRs are re-tested against the latest `main`.
5. Save. The workflow needs to have run at least once (e.g. via an initial PR) before its job names appear in the status-check picker.

Once configured, GitHub will block the merge button on any PR until all of the jobs above report success.

## Jenkins deployment pipeline

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
  - runs `backend/mvnw test` and `onboarding-service` `mvn test` — the pipeline
    stops here (no deploy) if either test suite fails
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
