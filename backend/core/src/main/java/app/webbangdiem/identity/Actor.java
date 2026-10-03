package app.webbangdiem.identity;

public record Actor(String id, String displayName, String role, String departmentId) {
    public boolean hasRole(String expected) {
        return role != null && role.equals(expected);
    }
}
