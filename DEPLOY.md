# Деплой Orkestro (prod)

Инструкция по развёртыванию продакшн-окружения Orkestro на одном сервере через Docker Compose: backend (Spring Boot), frontend (статика через nginx), Postgres, RabbitMQ, SMTP (email), Telegram-бот.

## 1. Архитектура прод-окружения

Оркестрируется файлом `docker-compose.prod.yml` из `orkestro-backend`:

| Сервис | Что делает | Порты наружу |
|---|---|---|
| `nginx` | Отдаёт статику фронтенда, терминирует TLS, проксирует `/api` и `/ws` на `backend` | 80, 443 |
| `backend` | Spring Boot приложение (WAR), REST API + WebSocket | нет (только внутри сети) |
| `postgres` | Основная БД приложения | нет |
| `rabbitmq` | Очереди для email/Telegram/VK уведомлений | нет |
| `telegram-bot` | Отдельный сервис (репозиторий `orkestro-telegram-bot`), слушает RabbitMQ | нет |

Все сервисы, кроме `nginx`, сидят только во внутренней docker-сети `internal` и не торчат наружу — это осознанное решение, не открывайте им порты без необходимости.

MinIO/S3-хранилище файлов — **внешнее** (по умолчанию Yandex Object Storage), в компоуз-файл не входит.

## 2. Требования к серверу

- Linux-сервер (2+ vCPU, 4+ GB RAM с запасом — суммарные `deploy.resources.limits` по сервисам ~2 GB, плюс ОС и сборка образов).
- Установлены `git`, `docker`, `docker compose` (плагин, не legacy `docker-compose`), `certbot`.
- Node.js 20+ и npm — либо на сервере, либо на машине, с которой собирается фронтенд (см. шаг 6).
- Доменное имя, A-записи которого указывают на IP сервера.
- Открыты порты 80 и 443 во внешнем файрволе.

## 3. Расположение репозиториев

`docker-compose.prod.yml` собирает `telegram-bot` из `../orkestro-telegram-bot` (относительный путь), поэтому три репозитория должны лежать рядом, на одном уровне:

```
/opt/orkestro/
├── orkestro-backend/       # содержит docker-compose.prod.yml
├── orkestro-frontend/
└── orkestro-telegram-bot/
```

```bash
mkdir -p /opt/orkestro && cd /opt/orkestro
git clone <url-backend> orkestro-backend
git clone <url-frontend> orkestro-frontend
git clone <url-telegram-bot> orkestro-telegram-bot
```

## 4. Настройка переменных окружения backend

```bash
cd orkestro-backend
cp .env.prod.example .env.prod
```

Заполните `.env.prod` реальными значениями:

- `DOMAIN` — ваш домен (используется для `EMAIL_RSVP_BASE_URL`/`FRONTEND_BASE_URL`, оба собираются как `https://${DOMAIN}`).
- `POSTGRES_PASSWORD`, `RABBITMQ_USER`/`RABBITMQ_PASSWORD`, `JWT_SECRET` — сгенерируйте случайные строки (`openssl rand -hex 32` и т.п.), никогда не оставляйте примерные значения.
- `MINIO_URL`/`MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY`/`MINIO_BUCKET`/`MINIO_REGION` — реквизиты вашего S3-хранилища.
- `SMTP_HOST`/`SMTP_PORT`/`SMTP_FROM`/`SMTP_USERNAME`/`SMTP_PASSWORD` — реквизиты вашего почтового релея (например Brevo/mail.ru).
- `BOT_TOKEN` — токен Telegram-бота от @BotFather.

## 5. SSL-сертификаты (Let's Encrypt)

`nginx.conf` требует наличия сертификата ещё до старта контейнера (443-блок конфигурируется безусловно), поэтому сертификат нужно получить **до** первого запуска `nginx` через compose.

```bash
# 1. Подставьте домен в nginx-конфиг
sed -i "s/YOUR_DOMAIN/${DOMAIN}/g" orkestro-backend/nginx/nginx.conf

# 2. Временно поднимите certbot в standalone-режиме (порт 80 должен быть свободен)
sudo certbot certonly --standalone -d ${DOMAIN}
```

Сертификаты появятся в `/etc/letsencrypt/live/${DOMAIN}/` — именно этот путь смонтирован в контейнер `nginx` как read-only volume.

Для автопродления (после того как `nginx` уже поднят и работает — тогда можно переключиться на webroot-метод через `/var/www/certbot`, который тоже смонтирован в `nginx.conf`):

```bash
sudo crontab -e
# добавить строку:
0 3 * * * certbot renew --webroot -w /var/www/certbot --deploy-hook "docker compose -f /opt/orkestro/orkestro-backend/docker-compose.prod.yml restart nginx" --quiet
```

## 6. Сборка и выкладка фронтенда

Frontend не оборачивается в отдельный Docker-образ — nginx отдаёт статику из `orkestro-backend/nginx/frontend`, куда нужно положить результат сборки.

```bash
cd orkestro-frontend
```

Отредактируйте `.env.production` (в репозитории он в `.gitignore`, создаётся вручную на сервере или на билд-машине):

```env
VITE_API_BASE_URL=https://yourdomain.com
VITE_APP_BASE_URL=https://yourdomain.com
```

Затем:

```bash
npm ci
npm run build
rm -rf ../orkestro-backend/nginx/frontend
cp -r dist ../orkestro-backend/nginx/frontend
```

Повторяйте эти шаги при каждом обновлении фронтенда — автосборки в CI на данный момент нет.

## 7. Полный запуск

```bash
cd orkestro-backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Проверка:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

Откройте `https://yourdomain.com` в браузере, проверьте логин, создание события (проверяет БД+RabbitMQ), WebSocket-уведомление и отправку email (created/reminder уведомление на событие).

## 8. Обновление (redeploy)

```bash
# backend
cd orkestro-backend && git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build backend

# frontend — пересобрать и скопировать (см. шаг 6), затем:
docker compose -f docker-compose.prod.yml restart nginx

# telegram-bot
cd ../orkestro-telegram-bot && git pull
cd ../orkestro-backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build telegram-bot
```

Миграции БД (Liquibase) применяются автоматически при старте `backend`.

## 9. Бэкапы

Постоянные данные лежат в именованных docker-volume:

- `pgdata` — основная БД приложения.
- `rabbitmq_data` — не критично, очереди не требуют долгосрочного хранения.

```bash
docker run --rm -v orkestro-backend_pgdata:/data -v $(pwd):/backup alpine \
  tar czf /backup/pgdata-$(date +%F).tar.gz -C /data .
```

(имя volume у docker compose обычно с префиксом `<директория-проекта>_`, уточните через `docker volume ls`).

Файлы приложения (аватарки, вложения) хранятся во внешнем S3 — бэкапится средствами провайдера.

## 10. Диагностика

```bash
# логи конкретного сервиса
docker compose -f docker-compose.prod.yml logs -f <service>

# зайти внутрь контейнера backend
docker compose -f docker-compose.prod.yml exec backend sh
```

Частые проблемы:

- **nginx не стартует** — сертификаты ещё не выпущены (см. шаг 5) либо домен в `nginx.conf` не совпадает с `/etc/letsencrypt/live/<domain>`.
- **email не отправляются** — не заполнены/неверны `SMTP_*` переменные либо релей блокирует подключение (см. логи `backend`).
- **бот не отвечает** — неверный `BOT_TOKEN` или RabbitMQ ещё не готов (`depends_on` уже это учитывает, но проверьте логи `telegram-bot`).
