package com.example.rawaaproject.models;

import java.io.Serializable;

/**
 * نموذج المستخدم المبسط - يحتوي على البيانات الأساسية فقط
 */
public class User implements Serializable {
    
    // البيانات الأساسية
    public String id;
    /** يُملأ من مستند Appwrite عند الحاجة للمطابقة مع حقل userId في البيانات */
    public String userId;
    public String email;
    /** لا يُسلسل إلى JSON/قاعدة البيانات — للتحقق والتسجيل في الذاكرة فقط */
    public transient String password;
    public String fullName;
    public String role; // "teacher" أو "student"
    
    // بيانات الاتصال
    public String phoneNumber;
    public String profileImageUrl;
    /** سنة أو تاريخ ميلاد الطالب — لا يُستخدم للمعلم */
    public String birthDate;
    /** مواد التخصص للمعلم (أسطر أو فواصل) — تُحفظ في الجلسة */
    public String specialization;
    /** سنوات الخبرة للمعلم كنص (1–40) */
    public String experience;
    public String hourlyRate;
    /** نبذة اختيارية */
    public String description;
    /** الصف للطالب */
    public String grade;
    public String school;
    /** المواد التي يحتاجها الطالب */
    public String subjectsNeeded;
    
    // بيانات النظام
    public long createdAt;
    public long updatedAt;
    /** الحساب يُعتبر مفعّلاً بعد التسجيل — لا يُعرض في الواجهة */
    public boolean isActive;
    
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public User(String email, String password, String fullName, String role) {
        this();
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }
    
    // طرق مساعدة
    public boolean isTeacher() {
        return "teacher".equals(role);
    }
    
    public boolean isStudent() {
        return "student".equals(role);
    }
    
    public String getDisplayName() {
        return fullName != null && !fullName.isEmpty() ? fullName : email;
    }
    
    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    // التحقق من صحة البيانات
    public boolean isValid() {
        return email != null && !email.isEmpty() &&
               password != null && password.length() >= 6 &&
               fullName != null && !fullName.isEmpty() &&
               role != null && (role.equals("teacher") || role.equals("student"));
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
