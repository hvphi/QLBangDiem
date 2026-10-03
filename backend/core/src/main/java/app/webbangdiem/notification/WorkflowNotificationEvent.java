package app.webbangdiem.notification;

public record WorkflowNotificationEvent(String kind, String recipientId, String transcriptId, String message) {}
