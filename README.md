# Notifier

Notification service for alert routing and delivery.

## Overview

Receives alerts from monitoring systems (Alertmanager webhook) and routes them to appropriate channels based on configuration.

## Channels

- **Telegram Bot** — configuration UI, text alerts
- **Pushover** — critical alerts with iOS Critical Alerts (bypass DND)

## Features

- Telegram bot for managing subscriptions and alert rules
- Webhook endpoint for Alertmanager integration
- Alert routing based on severity/labels
- On-call schedule management

## Tech Stack

- Kotlin + Spring Boot 3.4
- Telegram Bot API
- Pushover API