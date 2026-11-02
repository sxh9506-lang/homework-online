package com.homework.online.service;

import com.homework.online.domain.Assignment;
import com.homework.online.domain.AssignmentStatus;
import com.homework.online.domain.ClassMember;
import com.homework.online.domain.Role;
import com.homework.online.domain.SchoolClass;
import com.homework.online.domain.Submission;
import com.homework.online.domain.SubmissionStatus;
import com.homework.online.domain.User;
import com.homework.online.repo.AssignmentRepository;
import com.homework.online.repo.ClassMemberRepository;
import com.homework.online.repo.SchoolClassRepository;
import com.homework.online.repo.SubmissionRepository;
import com.homework.online.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 首次启动时写入演示数据（教师、学生、班级、作业、提交记录）。
 * 关闭方式：app.seed-demo-data=false
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository memberRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DataSeeder(UserRepository userRepository,
                      SchoolClassRepository schoolClassRepository,
                      ClassMemberRepository memberRepository,
                      AssignmentRepository assignmentRepository,
                      SubmissionRepository submissionRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed-demo-data:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        String password = passwordEncoder.encode("123456");
        LocalDateTime now = LocalDateTime.now();

        User zeng = createUser("zeng", password, "曾芮", Role.TEACHER, null, "语文");
        User li = createUser("li", password, "李文博", Role.TEACHER, null, "数学");

        User zhang = createUser("zhang", password, "张小明", Role.STUDENT, "20240101", null);
        User zhao = createUser("zhao", password, "赵一鸣", Role.STUDENT, "20240102", null);
        User sun = createUser("sun", password, "孙悦", Role.STUDENT, "20240103", null);
        User qian = createUser("qian", password, "钱多多", Role.STUDENT, "20240104", null);
        User zhou = createUser("zhou", password, "周思远", Role.STUDENT, "20240105", null);
        User wu = createUser("wu", password, "吴桐", Role.STUDENT, "20240106", null);
        User chen = createUser("chen", password, "陈嘉禾", Role.STUDENT, "20240107", null);

        SchoolClass class3 = createClass("高一（3）班 · 语文", "G1-3", zeng, now.minusDays(30));
        SchoolClass class5 = createClass("高一（5）班 · 语文", "G1-5", zeng, now.minusDays(25));
        SchoolClass math3 = createClass("高一（3）班 · 数学", "M1-3", li, now.minusDays(20));

        for (User student : new User[]{zhang, zhao, sun, qian, zhou, wu, chen}) {
            memberRepository.save(new ClassMember(class3, student));
        }
        memberRepository.save(new ClassMember(class5, sun));
        memberRepository.save(new ClassMember(class5, zhou));
        memberRepository.save(new ClassMember(class5, chen));
        memberRepository.save(new ClassMember(math3, zhang));
        memberRepository.save(new ClassMember(math3, qian));

        Assignment lotus = createAssignment(class3, zeng, "《荷塘月色》第 4-6 段赏析",
                "## 作业要求\n\n1. 阅读朱自清《荷塘月色》第 4—6 段，从**通感、比喻、叠词**三个角度各找出一处例句；\n"
                        + "2. 任选其中一处，写一段不少于 300 字的赏析文字；\n"
                        + "3. 可以手写拍照上传，也可以直接在系统中在线录入。\n\n> 注意：请勿直接复制网络赏析，独立完成。",
                now.plusDays(2).withHour(21).withMinute(0), 100, true, AssignmentStatus.PUBLISHED, now.minusDays(2));

        Assignment dictation = createAssignment(class3, zeng, "古诗文默写 · 必修上册第三单元",
                "## 默写范围\n\n- 《短歌行》全文\n- 《归园田居（其一）》前八句\n- 《念奴娇·赤壁怀古》下阕\n\n**提交方式**：在系统内在线录入默写内容，错别字每处扣 2 分。",
                now.minusDays(1), 50, true, AssignmentStatus.PUBLISHED, now.minusDays(6));

        Assignment reading = createAssignment(class3, zeng, "现代文阅读训练（一）",
                "## 材料\n\n阅读下发材料《故乡的榕树》，完成文后 4 道小题。\n\n1. 分析第 3 段景物描写的作用（6 分）\n2. 理解“榕树是故乡的魂”的含义（8 分）\n3. 概括作者情感变化（6 分）\n4. 结合自身经历写一段 200 字感悟（10 分）",
                now.minusDays(4), 30, false, AssignmentStatus.PUBLISHED, now.minusDays(10));

        createAssignment(class5, zeng, "周末随笔：我身边的小人物",
                "## 写作任务\n\n观察身边一位普通劳动者，用细节描写刻画人物，不少于 600 字。\n\n要求：不出现真实姓名，可用化名；结尾有一段抒情性议论。",
                now.plusDays(4).withHour(21).withMinute(0), 100, false, AssignmentStatus.PUBLISHED, now.minusDays(1));

        createAssignment(class3, zeng, "《劝学》论证结构梳理",
                "（草稿）用思维导图梳理《劝学》的论证层次，下周三前上传图片。",
                now.plusDays(8), 100, false, AssignmentStatus.DRAFT, null);

        createSubmission(dictation, zhang,
                "对酒当歌，人生几何！譬如朝露，去日苦多。\n慨当以慷，忧思难忘。何以解忧？唯有杜康。\n青青子衿，悠悠我心。但为君故，沉吟至今。",
                now.minusDays(1).minusHours(10), SubmissionStatus.GRADED, 44,
                "默写整体准确，第 4 句“慨当以慷”后漏写“忧思难忘”，扣 2 分；结尾两句顺序有误，扣 4 分。订正后交组长检查。",
                now.minusDays(1), zeng);

        createSubmission(dictation, zhao,
                "对酒当歌，人生几何！譬如朝露，去日苦多。……\n山不厌高，海不厌深。周公吐哺，天下归心。",
                now.minusDays(1).minusHours(5), SubmissionStatus.GRADED, 48,
                "内容完整，仅一处标点缺失。继续保持！", now.minusDays(1), zeng);

        createSubmission(dictation, sun, "（在线录入内容略）", now.minusDays(1).minusHours(12),
                SubmissionStatus.SUBMITTED, null, null, null, null);

        createSubmission(dictation, qian, "对酒当歌，人生几何！譬如朝露，去日苦多。慨当以慷，忧思难忘。",
                now.minusDays(1).minusHours(2), SubmissionStatus.SUBMITTED, null, null, null, null);

        createSubmission(lotus, zhou,
                "赏析角度：通感。\n\n“微风过处，送来缕缕清香，仿佛远处高楼上渺茫的歌声似的。”作者用听觉上的歌声来写嗅觉上的清香，两种感官彼此打通……（后文略，共 420 字）",
                now.minusHours(12), SubmissionStatus.SUBMITTED, null, null, null, null);

        createSubmission(reading, wu,
                "一、景物描写作用：渲染了……（略）\n二、“榕树是故乡的魂”……（略）",
                now.minusDays(3).minusHours(12), SubmissionStatus.GRADED, 26,
                "第 3 题概括不够准确，注意分点作答。", now.minusDays(3), zeng);

        createSubmission(reading, zhang,
                "一、景物描写作用：交代时间地点，营造宁静氛围，为下文抒情做铺垫。\n二、榕树见证了几代人的生活，是乡愁的寄托。\n三、由思念到感伤，再到释然。",
                now.minusDays(3).minusHours(5), SubmissionStatus.GRADED, 28,
                "要点齐全，第 4 题感悟部分可以更具体。", now.minusDays(3), zeng);

        log.info("已写入演示数据：教师 zeng/li，学生 zhang/zhao/sun/qian/zhou/wu/chen，初始密码均为 123456");
    }

    private User createUser(String username, String encodedPassword, String name, Role role, String studentNo, String subject) {
        User user = new User(username, encodedPassword, name, role);
        user.setStudentNo(studentNo);
        user.setSubject(subject);
        user.setEmail(username + (role == Role.TEACHER ? "@school.edu.cn" : "@stu.edu.cn"));
        return userRepository.save(user);
    }

    private SchoolClass createClass(String name, String code, User teacher, LocalDateTime createdAt) {
        SchoolClass schoolClass = new SchoolClass(name, code, teacher);
        schoolClass.setCreatedAt(createdAt);
        return schoolClassRepository.save(schoolClass);
    }

    private Assignment createAssignment(SchoolClass schoolClass, User teacher, String title, String content,
                                       LocalDateTime dueAt, int totalScore, boolean allowLate,
                                       AssignmentStatus status, LocalDateTime createdAt) {
        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setContent(content);
        assignment.setSchoolClass(schoolClass);
        assignment.setTeacher(teacher);
        assignment.setDueAt(dueAt);
        assignment.setTotalScore(totalScore);
        assignment.setAllowLate(allowLate);
        assignment.setStatus(status);
        assignment.setCreatedAt(createdAt == null ? LocalDateTime.now() : createdAt);
        assignment.setPublishedAt(status == AssignmentStatus.PUBLISHED ? assignment.getCreatedAt() : null);
        return assignmentRepository.save(assignment);
    }

    private void createSubmission(Assignment assignment, User student, String content, LocalDateTime submittedAt,
                                  SubmissionStatus status, Integer score, String comment,
                                  LocalDateTime gradedAt, User gradedBy) {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(content);
        submission.setSubmittedAt(submittedAt);
        submission.setStatus(status);
        submission.setScore(score);
        submission.setComment(comment);
        submission.setGradedAt(gradedAt);
        submission.setGradedBy(gradedBy);
        submissionRepository.save(submission);
    }
}
