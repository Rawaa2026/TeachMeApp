package com.example.rawaaproject.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.rawaaproject.LinkToDb.DALAppWriteConnection;
import com.example.rawaaproject.models.Lesson;
import com.example.rawaaproject.models.LessonEnrollment;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * دروس منشورة وطلبات انضمام عبر مجموعات Appwrite: lessons، lesson_enrollments.
 */
public class LessonRepository {
    private static final String TAG = "LessonRepository";
    public static final String TABLE_LESSONS = "lessons";
    public static final String TABLE_ENROLLMENTS = "lesson_enrollments";

    /** updatedAt مطلوب؛ lessonPrice اختياري — cancelled يُحدَّث عند الإلغاء فقط */
    private static final String SCHEMA_LESSONS =
            "teacherId:string:true:t,subject:string:true:s,lessonTitle:string:true:lt,startAt:string:true:sa,updatedAt:string:true:u,lessonPrice:string:false:lp,cancelled:string:false:c";
    private static final String SCHEMA_ENROLLMENTS =
            "lessonId:string:true:l,studentId:string:true:st,status:string:true:ss";

    private final Context context;
    private final DALAppWriteConnection dal;
    private final Executor io = Executors.newCachedThreadPool();
    private final Handler main = new Handler(Looper.getMainLooper());

    private static boolean collectionsEnsured = false;

    public LessonRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dal = new DALAppWriteConnection(this.context);
    }

    private void ensureCollections() {
        if (collectionsEnsured) return;
        try {
            if (!dal.tableExists(TABLE_LESSONS, null)) {
                dal.createTable(TABLE_LESSONS, null, SCHEMA_LESSONS);
            } else {
                // التأكد من وجود جميع الحقول في حالة تم تحديث الـ schema في الكود
                dal.createTableAttributes(TABLE_LESSONS, SCHEMA_LESSONS, TABLE_LESSONS);
            }

            if (!dal.tableExists(TABLE_ENROLLMENTS, null)) {
                dal.createTable(TABLE_ENROLLMENTS, null, SCHEMA_ENROLLMENTS);
            } else {
                dal.createTableAttributes(TABLE_ENROLLMENTS, SCHEMA_ENROLLMENTS, TABLE_ENROLLMENTS);
            }
            collectionsEnsured = true;
        } catch (Exception e) {
            Log.w(TAG, "ensureCollections", e);
        }
    }

    public interface VoidCallback {
        void onDone(DALAppWriteConnection.OperationResult<Void> result);
    }

    public interface LessonsCallback {
        void onDone(DALAppWriteConnection.OperationResult<ArrayList<Lesson>> result);
    }

    public interface EnrollmentsCallback {
        void onDone(DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> result);
    }

    public static class PendingItem {
        public final LessonEnrollment enrollment;
        public final Lesson lesson;

        public PendingItem(LessonEnrollment enrollment, Lesson lesson) {
            this.enrollment = enrollment;
            this.lesson = lesson;
        }
    }

    public interface PendingListCallback {
        void onDone(DALAppWriteConnection.OperationResult<ArrayList<PendingItem>> result);
    }

    public void listAllLessons(LessonsCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<Lesson>> r =
                    dal.getData(TABLE_LESSONS, null, Lesson.class);
            normalizeLessons(r.data);
            main.post(() -> cb.onDone(r));
        });
    }

    /**
     * دروس للتصفح: القادمة أولاً ثم المنتهية (للعرض مع تشطيب في الواجهة).
     * تصفية اختيارية بالمادة.
     */
    public void listUpcomingLessons(@Nullable String subjectContains, LessonsCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<Lesson>> r =
                    dal.getData(TABLE_LESSONS, null, Lesson.class);
            if (!r.success) {
                main.post(() -> cb.onDone(r));
                return;
            }
            if (r.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, r.message, new ArrayList<>())));
                return;
            }
            normalizeLessons(r.data);
            Instant now = Instant.now();
            String needle = subjectContains != null ? subjectContains.trim() : "";
            ArrayList<Lesson> upcoming = new ArrayList<>();
            ArrayList<Lesson> past = new ArrayList<>();
            for (Lesson l : r.data) {
                if (isLessonCancelled(l)) {
                    continue;
                }
                if (needle.length() > 0) {
                    String subj = l.subject != null ? l.subject : "";
                    if (!subj.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                }
                try {
                    Instant st = Instant.parse(l.startAt != null ? l.startAt : "");
                    if (st.isAfter(now)) {
                        upcoming.add(l);
                    } else {
                        past.add(l);
                    }
                } catch (Exception ignored) {
                }
            }
            upcoming.sort(Comparator.comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception e) {
                    return Instant.EPOCH;
                }
            }));
            past.sort(Comparator.<Lesson, Instant>comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception e) {
                    return Instant.EPOCH;
                }
            }).reversed());
            ArrayList<Lesson> out = new ArrayList<>(upcoming.size() + past.size());
            out.addAll(upcoming);
            out.addAll(past);
            main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, r.message, out)));
        });
    }

    private static void normalizeLessons(ArrayList<Lesson> list) {
        if (list == null) {
            return;
        }
        for (Lesson l : list) {
            if (l.teacherId == null) {
                l.teacherId = "";
            }
            if (l.subject == null) {
                l.subject = "";
            }
            if (l.lessonTitle == null) {
                l.lessonTitle = "";
            }
            if (l.startAt == null) {
                l.startAt = "";
            }
            if (l.lessonPrice == null) {
                l.lessonPrice = "";
            }
            if (l.cancelled == null) {
                l.cancelled = "";
            }
        }
    }

    public static boolean isLessonCancelled(Lesson l) {
        if (l == null || l.cancelled == null) {
            return false;
        }
        String c = l.cancelled.trim();
        return "true".equalsIgnoreCase(c) || "1".equals(c);
    }

    /** يُسمح بالإلغاء طالما لم يبدأ وقت الدرس بعد */
    public static boolean canCancelBeforeLessonStart(String startAtIso) {
        if (startAtIso == null || startAtIso.isEmpty()) {
            return false;
        }
        try {
            return Instant.now().isBefore(Instant.parse(startAtIso));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isStartAtInFuture(String startAtIso) {
        if (startAtIso == null || startAtIso.isEmpty()) {
            return false;
        }
        try {
            Instant start = Instant.parse(startAtIso);
            return start.isAfter(Instant.now().plusSeconds(60));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public void publishLesson(String teacherId, String subject, String lessonTitle, String startAtIso,
                              @Nullable String lessonPrice, VoidCallback cb) {
        io.execute(() -> {
            ensureCollections();
            if (teacherId == null || teacherId.isEmpty()) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "معرف المعلم غير صالح")));
                return;
            }
            if (subject == null || subject.trim().isEmpty() || lessonTitle == null || lessonTitle.trim().isEmpty()) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "املأ الموضوع وعنوان الدرس")));
                return;
            }
            if (!isStartAtInFuture(startAtIso)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                        context.getString(com.example.rawaaproject.R.string.lesson_time_must_be_future))));
                return;
            }
            String docId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            Map<String, Object> data = new HashMap<>();
            data.put("teacherId", teacherId.trim());
            data.put("subject", subject.trim());
            data.put("lessonTitle", lessonTitle.trim());
            data.put("startAt", startAtIso);
            data.put("updatedAt", String.valueOf(now));
            String price = lessonPrice != null ? lessonPrice.trim() : "";
            data.put("lessonPrice", price);
            DALAppWriteConnection.OperationResult<Void> res =
                    dal.createDocumentWithData(TABLE_LESSONS, null, docId, data);
            main.post(() -> cb.onDone(res));
        });
    }

    public void listEnrollments(EnrollmentsCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> r =
                    dal.getData(TABLE_ENROLLMENTS, null, LessonEnrollment.class);
            normalizeEnrollments(r.data);
            main.post(() -> cb.onDone(r));
        });
    }

    private static void normalizeEnrollments(ArrayList<LessonEnrollment> list) {
        if (list == null) {
            return;
        }
        for (LessonEnrollment e : list) {
            if (e.lessonId == null) {
                e.lessonId = "";
            }
            if (e.studentId == null) {
                e.studentId = "";
            }
            if (e.status == null) {
                e.status = LessonEnrollment.STATUS_PENDING;
            }
        }
    }

    public void requestJoin(String lessonId, String studentId, VoidCallback cb) {
        io.execute(() -> {
            ensureCollections();
            if (lessonId == null || studentId == null || lessonId.isEmpty() || studentId.isEmpty()) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "بيانات غير صالحة")));
                return;
            }
            DALAppWriteConnection.OperationResult<Lesson> lessonCheck =
                    dal.getDataById(TABLE_LESSONS, lessonId, null, Lesson.class);
            Lesson les = lessonCheck.success ? lessonCheck.data : null;
            if (les == null || isLessonCancelled(les)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                        context.getString(com.example.rawaaproject.R.string.lesson_cancelled_or_missing))));
                return;
            }
            DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> all =
                    dal.getData(TABLE_ENROLLMENTS, null, LessonEnrollment.class);
            if (all.success && all.data != null) {
                for (LessonEnrollment e : all.data) {
                    if (!lessonId.equals(e.lessonId) || !studentId.equals(e.studentId)) {
                        continue;
                    }
                    if (LessonEnrollment.STATUS_CANCELLED.equals(e.status)) {
                        continue;
                    }
                    if (LessonEnrollment.STATUS_PENDING.equals(e.status)
                            || LessonEnrollment.STATUS_APPROVED.equals(e.status)) {
                        main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                                context.getString(com.example.rawaaproject.R.string.lesson_join_already))));
                        return;
                    }
                }
            }
            String id = UUID.randomUUID().toString();
            Map<String, Object> m = new HashMap<>();
            m.put("lessonId", lessonId);
            m.put("studentId", studentId);
            m.put("status", LessonEnrollment.STATUS_PENDING);
            DALAppWriteConnection.OperationResult<Void> res =
                    dal.createDocumentWithData(TABLE_ENROLLMENTS, null, id, m);
            main.post(() -> cb.onDone(res));
        });
    }

    public void setEnrollmentStatus(String enrollmentId, String newStatus, String teacherId, VoidCallback cb) {
        io.execute(() -> {
            if (enrollmentId == null || newStatus == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "بيانات غير صالحة")));
                return;
            }
            DALAppWriteConnection.OperationResult<LessonEnrollment> one =
                    dal.getDataById(TABLE_ENROLLMENTS, enrollmentId, null, LessonEnrollment.class);
            LessonEnrollment en = one.success ? one.data : null;
            if (en == null || en.lessonId == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "طلب غير موجود")));
                return;
            }
            DALAppWriteConnection.OperationResult<Lesson> lr =
                    dal.getDataById(TABLE_LESSONS, en.lessonId, null, Lesson.class);
            Lesson lesson = lr.success ? lr.data : null;
            if (lesson == null || lesson.teacherId == null || !lesson.teacherId.equals(teacherId)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "ليست صلاحيتك لهذا الطلب")));
                return;
            }
            Map<String, Object> patch = new HashMap<>();
            patch.put("status", newStatus);
            DALAppWriteConnection.OperationResult<Void> res =
                    dal.patchDocumentData(TABLE_ENROLLMENTS, null, enrollmentId, patch);
            main.post(() -> cb.onDone(res));
        });
    }

    public void loadPendingForTeacher(String teacherId, PendingListCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<Lesson>> lr =
                    dal.getData(TABLE_LESSONS, null, Lesson.class);
            DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> er =
                    dal.getData(TABLE_ENROLLMENTS, null, LessonEnrollment.class);
            if (!lr.success) {
                Log.w(TAG, "loadPendingForTeacher lessons: " + lr.message);
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, lr.message, new ArrayList<>())));
                return;
            }
            if (lr.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, lr.message, new ArrayList<>())));
                return;
            }
            if (!er.success) {
                Log.w(TAG, "loadPendingForTeacher enrollments: " + er.message);
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, er.message, new ArrayList<>())));
                return;
            }
            if (er.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, er.message, new ArrayList<>())));
                return;
            }
            normalizeLessons(lr.data);
            normalizeEnrollments(er.data);
            Map<String, Lesson> byLessonId = new HashMap<>();
            if (lr.data != null) {
                for (Lesson l : lr.data) {
                    if (l.id != null && teacherId != null && teacherId.equals(l.teacherId)) {
                        byLessonId.put(l.id, l);
                    }
                }
            }
            ArrayList<PendingItem> out = new ArrayList<>();
            if (er.data != null) {
                for (LessonEnrollment e : er.data) {
                    if (!LessonEnrollment.STATUS_PENDING.equals(e.status)) {
                        continue;
                    }
                    Lesson les = byLessonId.get(e.lessonId);
                    if (les != null && !isLessonCancelled(les)) {
                        out.add(new PendingItem(e, les));
                    }
                }
            }
            main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, "ok", out)));
        });
    }

    public void loadTeacherSchedule(String teacherId, LessonsCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<Lesson>> lr =
                    dal.getData(TABLE_LESSONS, null, Lesson.class);
            if (!lr.success || lr.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true,
                        lr != null ? lr.message : "", new ArrayList<>())));
                return;
            }
            normalizeLessons(lr.data);
            DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> er =
                    dal.getData(TABLE_ENROLLMENTS, null, LessonEnrollment.class);
            Set<String> lessonIdsWithApproval = new HashSet<>();
            if (er.success && er.data != null) {
                for (LessonEnrollment e : er.data) {
                    if (LessonEnrollment.STATUS_APPROVED.equals(e.status)) {
                        lessonIdsWithApproval.add(e.lessonId);
                    }
                }
            }
            Instant now = Instant.now();
            ArrayList<Lesson> upcoming = new ArrayList<>();
            ArrayList<Lesson> past = new ArrayList<>();
            for (Lesson l : lr.data) {
                if (!teacherId.equals(l.teacherId)) {
                    continue;
                }
                if (isLessonCancelled(l)) {
                    continue;
                }
                if (!lessonIdsWithApproval.contains(l.id)) {
                    continue;
                }
                try {
                    Instant st = Instant.parse(l.startAt != null ? l.startAt : "");
                    if (st.isAfter(now)) {
                        upcoming.add(l);
                    } else {
                        past.add(l);
                    }
                } catch (Exception ignored) {
                }
            }
            upcoming.sort(Comparator.comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception e) {
                    return Instant.EPOCH;
                }
            }));
            past.sort(Comparator.<Lesson, Instant>comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception e) {
                    return Instant.EPOCH;
                }
            }).reversed());
            ArrayList<Lesson> rows = new ArrayList<>(upcoming.size() + past.size());
            rows.addAll(upcoming);
            rows.addAll(past);
            main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, lr.message, rows)));
        });
    }

    public void loadStudentSchedule(String studentId, LessonsCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> er =
                    dal.getData(TABLE_ENROLLMENTS, null, LessonEnrollment.class);
            ArrayList<LessonEnrollment> enrollments = new ArrayList<>();
            if (er.success && er.data != null) {
                enrollments.addAll(er.data);
            }
            normalizeEnrollments(enrollments);
            Set<String> approvedLessonIds = new HashSet<>();
            for (LessonEnrollment e : enrollments) {
                if (LessonEnrollment.STATUS_APPROVED.equals(e.status) && studentId.equals(e.studentId)) {
                    approvedLessonIds.add(e.lessonId);
                }
            }
            DALAppWriteConnection.OperationResult<ArrayList<Lesson>> lr =
                    dal.getData(TABLE_LESSONS, null, Lesson.class);
            if (!lr.success || lr.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true,
                        lr != null ? lr.message : "", new ArrayList<>())));
                return;
            }
            normalizeLessons(lr.data);
            Instant now = Instant.now();
            ArrayList<Lesson> upcoming = new ArrayList<>();
            ArrayList<Lesson> past = new ArrayList<>();
            for (Lesson l : lr.data) {
                if (!approvedLessonIds.contains(l.id)) {
                    continue;
                }
                if (isLessonCancelled(l)) {
                    continue;
                }
                try {
                    Instant st = Instant.parse(l.startAt != null ? l.startAt : "");
                    if (st.isAfter(now)) {
                        upcoming.add(l);
                    } else {
                        past.add(l);
                    }
                } catch (Exception ignored) {
                }
            }
            upcoming.sort(Comparator.comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception e) {
                    return Instant.EPOCH;
                }
            }));
            past.sort(Comparator.<Lesson, Instant>comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception e) {
                    return Instant.EPOCH;
                }
            }).reversed());
            ArrayList<Lesson> rows = new ArrayList<>(upcoming.size() + past.size());
            rows.addAll(upcoming);
            rows.addAll(past);
            main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, "ok", rows)));
        });
    }

    public void cancelLessonByTeacher(String lessonId, String teacherId, VoidCallback cb) {
        io.execute(() -> {
            if (lessonId == null || teacherId == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "بيانات غير صالحة")));
                return;
            }
            DALAppWriteConnection.OperationResult<Lesson> lr =
                    dal.getDataById(TABLE_LESSONS, lessonId, null, Lesson.class);
            Lesson lesson = lr.success ? lr.data : null;
            if (lesson == null || !teacherId.equals(lesson.teacherId)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "الدرس غير موجود")));
                return;
            }
            if (isLessonCancelled(lesson)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                        context.getString(com.example.rawaaproject.R.string.lesson_already_cancelled))));
                return;
            }
            if (!canCancelBeforeLessonStart(lesson.startAt)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                        context.getString(com.example.rawaaproject.R.string.lesson_cannot_cancel_after_start))));
                return;
            }
            Map<String, Object> patch = new HashMap<>();
            patch.put("cancelled", "true");
            patch.put("updatedAt", String.valueOf(System.currentTimeMillis()));
            DALAppWriteConnection.OperationResult<Void> res =
                    dal.patchDocumentData(TABLE_LESSONS, null, lessonId, patch);
            main.post(() -> cb.onDone(res));
        });
    }

    public void cancelStudentEnrollment(String lessonId, String studentId, VoidCallback cb) {
        io.execute(() -> {
            if (lessonId == null || studentId == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "بيانات غير صالحة")));
                return;
            }
            DALAppWriteConnection.OperationResult<Lesson> lr =
                    dal.getDataById(TABLE_LESSONS, lessonId, null, Lesson.class);
            Lesson lesson = lr.success ? lr.data : null;
            if (lesson == null || isLessonCancelled(lesson)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                        context.getString(com.example.rawaaproject.R.string.lesson_cancelled_or_missing))));
                return;
            }
            if (!canCancelBeforeLessonStart(lesson.startAt)) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false,
                        context.getString(com.example.rawaaproject.R.string.lesson_cannot_cancel_after_start))));
                return;
            }
            DALAppWriteConnection.OperationResult<ArrayList<LessonEnrollment>> all =
                    dal.getData(TABLE_ENROLLMENTS, null, LessonEnrollment.class);
            if (!all.success || all.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, all.message)));
                return;
            }
            LessonEnrollment target = null;
            for (LessonEnrollment e : all.data) {
                if (!lessonId.equals(e.lessonId) || !studentId.equals(e.studentId)) {
                    continue;
                }
                if (LessonEnrollment.STATUS_CANCELLED.equals(e.status)) {
                    continue;
                }
                if (LessonEnrollment.STATUS_PENDING.equals(e.status)
                        || LessonEnrollment.STATUS_APPROVED.equals(e.status)) {
                    target = e;
                }
            }
            if (target == null || target.id == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(false, "لا يوجد تسجيل للإلغاء")));
                return;
            }
            Map<String, Object> patch = new HashMap<>();
            patch.put("status", LessonEnrollment.STATUS_CANCELLED);
            DALAppWriteConnection.OperationResult<Void> res =
                    dal.patchDocumentData(TABLE_ENROLLMENTS, null, target.id, patch);
            main.post(() -> cb.onDone(res));
        });
    }

    public void listMyLessonsAsTeacher(String teacherId, LessonsCallback cb) {
        io.execute(() -> {
            ensureCollections();
            DALAppWriteConnection.OperationResult<ArrayList<Lesson>> r =
                    dal.getData(TABLE_LESSONS, null, Lesson.class);
            if (!r.success) {
                main.post(() -> cb.onDone(r));
                return;
            }
            if (r.data == null) {
                main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, r.message, new ArrayList<>())));
                return;
            }
            normalizeLessons(r.data);
            Instant now = Instant.now();
            ArrayList<Lesson> upcoming = new ArrayList<>();
            ArrayList<Lesson> past = new ArrayList<>();
            for (Lesson l : r.data) {
                if (teacherId == null || !teacherId.equals(l.teacherId)) {
                    continue;
                }
                try {
                    Instant st = Instant.parse(l.startAt != null ? l.startAt : "");
                    if (st.isAfter(now)) {
                        upcoming.add(l);
                    } else {
                        past.add(l);
                    }
                } catch (Exception e) {
                    past.add(l);
                }
            }
            upcoming.sort(Comparator.comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception ex) {
                    return Instant.EPOCH;
                }
            }));
            past.sort(Comparator.<Lesson, Instant>comparing(x -> {
                try {
                    return Instant.parse(x.startAt != null ? x.startAt : "");
                } catch (Exception ex) {
                    return Instant.EPOCH;
                }
            }).reversed());
            ArrayList<Lesson> mine = new ArrayList<>(upcoming.size() + past.size());
            mine.addAll(upcoming);
            mine.addAll(past);
            main.post(() -> cb.onDone(new DALAppWriteConnection.OperationResult<>(true, r.message, mine)));
        });
    }
}
