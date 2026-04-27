package com.example.rawaaproject.models;

import java.io.Serializable;

/** درس منشور من معلم — يُحفظ في مجموعة Appwrite lessons */
public class Lesson implements Serializable {
    public String id;
    public String teacherId;
    public String subject;
    public String lessonTitle;
    /** وقت بدء الدرس بتنسيق ISO-8601 (UTC أو مع إزاحة) */
    public String startAt;
    /** سعر الدرس كنص (مثلاً بالشيكل) — اختياري */
    public String lessonPrice;
    /** "true" إذا ألغى المعلم الدرس من التطبيق */
    public String cancelled;

    public Lesson() {
    }
}
