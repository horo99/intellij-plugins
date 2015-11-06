package training.lesson;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import org.jetbrains.annotations.Nullable;
import training.editor.eduUI.EduIcons;
import training.lesson.exceptons.BadCourseException;
import training.lesson.exceptons.BadLessonException;
import training.lesson.exceptons.LessonIsOpenedException;
import training.lesson.exceptons.NoProjectException;
import training.lesson.log.LessonLog;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by karashevich on 29/01/15.
 */
public class Lesson extends AnAction {

    private Scenario scn;
    private String name;
    private String targetPath;
    private ArrayList<LessonListener> lessonListeners;
    private Course parentCourse;


    private boolean passed;
    private boolean isOpen;

    /*Log lesson metrics*/
    private LessonLog lessonLog;

    public boolean getPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public ArrayList<LessonListener> getLessonListeners() {
        return lessonListeners;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @AbstractCollection
    public void setScn(Scenario scn) {
        this.scn = scn;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }


    public Lesson(){
        passed = false;
        lessonLog = new LessonLog(this);
    }

    public Lesson(Scenario scenario, boolean passed, @Nullable Course course) throws BadLessonException {

        super(scenario.getName());
        scn = scenario;
        name = scn.getName();

        this.passed = passed;
        if (!scn.getSubtype().equals("aimless")) {
            targetPath = scn.getTarget();
        } else {
            targetPath = null;
        }
        lessonListeners = new ArrayList<LessonListener>();
        parentCourse = course;

        isOpen = false;

    }

    @Deprecated
    public void open(Dimension infoPanelDimension) throws IOException, FontFormatException, LessonIsOpenedException {
        //init infoPanel, check that Lesson has not opened yet
        if (isOpen) throw new LessonIsOpenedException(this.getName() + "is opened");
        onStart();

        isOpen = true;
    }


    public void open() throws NoProjectException, BadLessonException, ExecutionException, LessonIsOpenedException, IOException, FontFormatException, InterruptedException, BadCourseException {
        Project currentProject = CourseManager.getInstance().getCurrentProject();
        if (currentProject == null) {
            currentProject = CourseManager.getInstance().getEduProject();
        }
        if (currentProject == null) throw new NoProjectException();
        CourseManager.getInstance().openLesson(currentProject, this);
    }

    public void open(Project projectWhereToOpenLesson) throws NoProjectException, BadLessonException, ExecutionException, LessonIsOpenedException, IOException, FontFormatException, InterruptedException, BadCourseException {
        CourseManager.getInstance().openLesson(projectWhereToOpenLesson, this);
    }

    @Deprecated
    public void close(){
        //destroy infoPanel (infoPanel = null)
        isOpen = false;
        onClose();
    }



    public boolean isOpen() {return isOpen;}

    public LessonLog getLessonLog(){
        return lessonLog;
    }

    public Scenario getScn(){
        return scn;
    }

    @Nullable
    public String getTargetPath() {
        return targetPath;
    }

    @Nullable
    public Course getCourse() {return parentCourse;}

    //Listeners
    public void addLessonListener(LessonListener lessonListener){
        if (lessonListeners == null) lessonListeners = new ArrayList<LessonListener>();

        lessonListeners.add(lessonListener);
    }

    public void removeLessonListener(LessonListener lessonListener) {
        if (lessonListeners.contains(lessonListener)) {
            lessonListeners.remove(lessonListener);
        }
    }

    public void onStart(){
        lessonLog = new LessonLog(this);
        lessonLog.log("Lesson started");
        lessonLog.resetCounter();
        if (lessonListeners == null) lessonListeners = new ArrayList<LessonListener>();

        for (LessonListener lessonListener : lessonListeners) {
            lessonListener.lessonStarted(this);
        }
    }

    public void onClose(){
        for (LessonListener lessonListener : lessonListeners) {
            lessonListener.lessonClosed(this);
        }

        lessonListeners = null;

    }

    //call onPass handlers in lessonListeners
    public void onPass(){
        lessonLog.log("Lesson passed");
        CourseManager.getInstance().getGlobalLessonLog().commitSession(this);

        for (LessonListener lessonListener : lessonListeners) {
            lessonListener.lessonPassed(this);
        }

    }

    public void onNextLesson() throws BadLessonException, ExecutionException, IOException, FontFormatException, InterruptedException, BadCourseException, LessonIsOpenedException {
        for (LessonListener lessonListener : lessonListeners) {
            lessonListener.lessonNext(this);
        }
    }

    public void pass(){
        setPassed(true);
        onPass();
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        try {
            CourseManager.getInstance().openLesson(anActionEvent.getProject(), this);
        } catch (BadCourseException e) {
            e.printStackTrace();
        } catch (BadLessonException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (LessonIsOpenedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        if(getPassed())
            e.getPresentation().setIcon(IconLoader.getIcon(EduIcons.CHECKMARK_DARK_GRAY));

    }

    class EditorParameters{
        final public static String PROJECT_TREE = "projectTree";
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof Lesson)) return false;
        if(this.getName() == null) return false;
        if (((Lesson) o).getName() == null) return false;
        if(((Lesson) o).getName().equals(this.getName())) return true;
        return false;

    }
}
