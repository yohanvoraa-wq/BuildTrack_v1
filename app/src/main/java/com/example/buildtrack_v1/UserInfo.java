package com.example.buildtrack_v1;

public class UserInfo {
    private String name;
    private String email;
    private String role;
    private String engineerId;
    private String phone;   // digits only, shown to the roles allowed to contact this user

    public UserInfo() {}

    public UserInfo(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getEngineerId() { return engineerId; }
    public String getPhone() { return phone; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setEngineerId(String engineerId) { this.engineerId = engineerId; }
    public void setPhone(String phone) { this.phone = phone; }
}