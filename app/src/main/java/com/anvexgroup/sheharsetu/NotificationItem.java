package com.anvexgroup.sheharsetu;

/**
 * Immutable model for a single notification inbox item.
 * Parsed from JSON returned by list_user_notifications.php.
 */
public final class NotificationItem {

    private final long   notificationId;
    private final String title;
    private final String body;
    private final String imageUrl;
    private final String targetScreen;
    private final String targetId;
    private final String source;
    private       boolean isRead;          // mutable – toggled on mark-read
    private final String readAt;
    private final String createdAt;

    public NotificationItem(
            long   notificationId,
            String title,
            String body,
            String imageUrl,
            String targetScreen,
            String targetId,
            String source,
            boolean isRead,
            String readAt,
            String createdAt) {
        this.notificationId = notificationId;
        this.title          = title          != null ? title          : "";
        this.body           = body           != null ? body           : "";
        this.imageUrl       = imageUrl       != null ? imageUrl       : "";
        this.targetScreen   = targetScreen   != null ? targetScreen   : "";
        this.targetId       = targetId       != null ? targetId       : "";
        this.source         = source         != null ? source         : "admin_push";
        this.isRead         = isRead;
        this.readAt         = readAt         != null ? readAt         : "";
        this.createdAt      = createdAt      != null ? createdAt      : "";
    }

    public long    getNotificationId() { return notificationId; }
    public String  getTitle()          { return title;          }
    public String  getBody()           { return body;           }
    public String  getImageUrl()       { return imageUrl;       }
    public String  getTargetScreen()   { return targetScreen;   }
    public String  getTargetId()       { return targetId;       }
    public String  getSource()         { return source;         }
    public boolean isRead()            { return isRead;         }
    public String  getReadAt()         { return readAt;         }
    public String  getCreatedAt()      { return createdAt;      }

    /** Called locally after a successful mark-read API call. */
    public void markRead() {
        this.isRead = true;
    }

    public boolean hasTarget() {
        return !targetScreen.isEmpty() && !targetId.isEmpty();
    }

    public boolean hasImage() {
        return !imageUrl.isEmpty();
    }
}
