package com.example.rawaaproject.models;

import java.io.Serializable;

/** طلب انضمام طالب لدرس — pending / approved / rejected */
public class LessonEnrollment implements Serializable {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";

    public String id;
    public String lessonId;
    public String studentId;
    public String status;

    public LessonEnrollment() {
        this.status = STATUS_PENDING;
    }
}
