# Workflow notification contract v1

Core publishes `WorkflowNotificationEvent(kind, recipientId, transcriptId, message)` after an accepted workflow action. The local consumer persists in-app notices idempotently by recipient, transcript, and kind. Notifications contain a safe transcript reference and no grade contents or PDF. External delivery channels are not enabled until a school provider is configured.
