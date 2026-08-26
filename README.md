# GiftMoji

A Spring Boot service, deployed to Azure App Service (Linux, F1 free tier).

See [`docs/DOMAIN_MODEL.md`](docs/DOMAIN_MODEL.md) for the domain model and
the send → receive → redeem gifting flow (with diagrams).

## Stack
- Java 21
- Spring Boot 3.3.4
- Gradle
- Azure App Service (Linux, F1 free tier)
- GitHub Actions (OIDC login to Azure, no stored client secret)

## Local development

Generate the Gradle wrapper once (requires a local Gradle install, since it
can't be generated inside this sandbox):

```bash
gradle wrapper --gradle-version 8.10
```

Then run:

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`. `GET /` returns a small JSON
status payload.

## Azure setup (one-time)

1. Create a resource group and an App Service on the F1 (free) tier:
   ```bash
   az group create --name giftmoji-rg --location australiaeast
   az appservice plan create --name giftmoji-plan --resource-group giftmoji-rg --sku F1 --is-linux
   az webapp create --name giftmoji --resource-group giftmoji-rg --plan giftmoji-plan --runtime "JAVA:21-java21"
   ```
   Note: App Service names are globally unique — you may need to change
   `giftmoji` to something else, and update `AZURE_WEBAPP_NAME` in
   `.github/workflows/deploy.yml` to match.

2. Set up OIDC federated credentials so GitHub Actions can log in to Azure
   without a stored secret (same approach used for personal-app):
   ```bash
   az ad app create --display-name giftmoji-github-oidc
   # note the appId, then create a federated credential scoped to this repo/branch
   az ad app federated-credential create --id <appId> --parameters '{
     "name": "giftmoji-main-branch",
     "issuer": "https://token.actions.githubusercontent.com",
     "subject": "repo:<your-github-username>/GiftMoji:ref:refs/heads/main",
     "audiences": ["api://AzureADTokenExchange"]
   }'
   ```
   Then assign the app a Contributor role on the resource group, and add
   `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, and `AZURE_SUBSCRIPTION_ID` as
   repository secrets in GitHub.

3. Push to `main` — the workflow builds the fat jar with Gradle and deploys
   it to the App Service.

## Notes
- `server.port` reads the `PORT` env var, which Azure App Service (Linux)
  sets automatically; it falls back to `8080` for local runs.
- The plain `jar` task is disabled and `bootJar` has an empty classifier,
  so `build/libs/` contains exactly one jar — this avoids the "which jar do
  I deploy" ambiguity that came up while setting up personal-app.
