# Deploying to Cloud Run (with Cloud SQL for MySQL)

Recommended approach: **Cloud SQL Auth Proxy as a sidecar container** on Cloud
Run (2nd gen execution environment). This is the simplest path because your
JDBC URLs stay exactly what they already are in `application.yml`
(`jdbc:mysql://localhost:3306/...` / `127.0.0.1:3306`) — no socket-factory
dependency, no code change. The proxy just makes "localhost:3306" actually
reach your Cloud SQL instance securely.

## 1. Create the Cloud SQL instance + database + app user

```bash
gcloud sql instances create hms-mysql \
  --database-version=MYSQL_8_0 \
  --tier=db-f1-micro \
  --region=asia-south1 \
  --root-password=<CHOOSE_A_ROOT_PASSWORD>

gcloud sql databases create hms_master --instance=hms-mysql

gcloud sql users create hms_app \
  --instance=hms-mysql \
  --password=<CHOOSE_AN_APP_PASSWORD>
```

Then connect once (via `gcloud sql connect hms-mysql --user=root`) and run the
same grants as local setup (`local-setup/local-mysql-setup.sql`, skipping the
`CREATE USER` lines since you just made the user above) so `hms_app` can
create tenant schemas.

Note the **instance connection name**: `gcloud sql instances describe hms-mysql --format='value(connectionName)'`
→ looks like `your-project:asia-south1:hms-mysql`.

## 2. Build & push the image

```bash
gcloud auth configure-docker asia-south1-docker.pkg.dev

docker build -t asia-south1-docker.pkg.dev/YOUR_PROJECT/hms/hms-backend:latest .
docker push asia-south1-docker.pkg.dev/YOUR_PROJECT/hms/hms-backend:latest
```

(Create the Artifact Registry repo first if you haven't: `gcloud artifacts repositories create hms --repository-format=docker --location=asia-south1`.)

## 3. Store secrets

```bash
echo -n "<YOUR_APP_DB_PASSWORD>" | gcloud secrets create hms-db-password --data-file=-
echo -n "$(openssl rand -base64 32)" | gcloud secrets create hms-jwt-secret --data-file=-
```

## 4. Deploy with the Cloud SQL Auth Proxy sidecar

Save this as `service.yaml`:

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: hms-backend
spec:
  template:
    metadata:
      annotations:
        run.googleapis.com/execution-environment: gen2
    spec:
      containers:
        - name: app
          image: asia-south1-docker.pkg.dev/YOUR_PROJECT/hms/hms-backend:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_MASTER_JDBC-URL
              value: "jdbc:mysql://localhost:3306/hms_master?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            - name: SPRING_DATASOURCE_TENANT_JDBC-URL
              value: "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            - name: SPRING_DATASOURCE_MASTER_USERNAME
              value: "hms_app"
            - name: SPRING_DATASOURCE_TENANT_USERNAME
              value: "hms_app"
            - name: SPRING_DATASOURCE_MASTER_PASSWORD
              valueFrom: { secretKeyRef: { name: hms-db-password, key: latest } }
            - name: SPRING_DATASOURCE_TENANT_PASSWORD
              valueFrom: { secretKeyRef: { name: hms-db-password, key: latest } }
            - name: HMS_JWT_SECRET
              valueFrom: { secretKeyRef: { name: hms-jwt-secret, key: latest } }
            - name: HMS_CORS_ALLOWED-ORIGINS
              value: "https://your-frontend-domain.com"
        - name: cloud-sql-proxy
          image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.14.0
          args:
            - "--structured-logs"
            - "--port=3306"
            - "YOUR_PROJECT:asia-south1:hms-mysql"
```

```bash
gcloud run services replace service.yaml --region=asia-south1

# Grant the Cloud Run service account access to Cloud SQL
gcloud projects add-iam-policy-binding YOUR_PROJECT \
  --member="serviceAccount:YOUR_PROJECT_NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

## Notes

- `SPRING_DATASOURCE_MASTER_JDBC-URL` etc. work because Spring Boot's relaxed
  env-var binding maps `SCREAMING_SNAKE_WITH_HYPHENS` back to
  `spring.datasource.master.jdbc-url` — matching the custom keys already used
  in `application.yml`.
- Rotate `hms.platform.seed-admin.*` (disable it in Cloud Run — set
  `HMS_PLATFORM_SEED-ADMIN_ENABLED=false` — and create your real platform
  admin manually) once you've done initial setup, since the seeded credential
  is meant for local dev only.
- For a hospital-facing production deploy, also tighten
  `hms.cors.allowed-origins` to your real frontend domain(s) only.
- Cloud Run scales to zero by default; the first request after idle will pay
  the Cloud SQL Auth Proxy + JVM cold-start cost. Set `--min-instances=1` if
  that matters for your hospitals' peak-hour experience.
