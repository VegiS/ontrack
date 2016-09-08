package net.nemerosa.ontrack.job.support;

import com.google.common.collect.ImmutableSet;
import net.nemerosa.ontrack.common.FutureUtils;
import net.nemerosa.ontrack.job.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DefaultJobSchedulerTest {

    private ScheduledExecutorService scheduledExecutorService;

    @Before
    public void before() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void after() {
        scheduledExecutorService.shutdownNow();
    }

    protected JobScheduler createJobScheduler() {
        return new DefaultJobScheduler(NOPJobDecorator.INSTANCE, scheduledExecutorService, NOPJobListener.INSTANCE);
    }

    @Test
    public void schedule() throws InterruptedException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        Thread.sleep(3000);
        assertTrue(job.getCount() >= 2);
    }

    @Test
    public void reschedule() throws InterruptedException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        // Initially every second
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        Thread.sleep(2500);
        int count = job.getCount();
        assertTrue(count == 3);
        // Then every minute
        jobScheduler.schedule(job, new Schedule(1, 1, TimeUnit.MINUTES));
        // Checks after three more seconds than the count has not moved
        Thread.sleep(3000);
        assertEquals(count, job.getCount());
    }

    @Test
    public void fire_immediately() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        // Fires far in the future
        jobScheduler.schedule(job, Schedule.everySeconds(60).after(60));
        assertEquals(0, job.getCount());
        // Fires immediately and waits for the result
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        assertEquals(1, job.getCount());
    }

    @Test
    public void fire_immediately_in_concurrency() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        LongCountJob job = new LongCountJob();
        // Fires now
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        // After 2 seconds, nothing has happened yet
        Thread.sleep(2000);
        assertEquals(0, job.getCount());
        // Checks its status
        assertTrue(jobScheduler.getJobStatus(job.getKey()).get().isRunning());
        // Fires immediately and waits for the result
        Future<?> future = jobScheduler.fireImmediately(job.getKey());
        // The job is already running, count is still 0
        assertEquals(0, job.getCount());
        // Waits until completion
        future.get(1, TimeUnit.MINUTES);
        assertEquals(1, job.getCount());
    }

    @Test
    public void statuses() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();

        LongCountJob longCountJob = new LongCountJob();
        jobScheduler.schedule(longCountJob, Schedule.EVERY_SECOND);

        CountJob countJob = new CountJob();
        jobScheduler.schedule(countJob, Schedule.EVERY_SECOND.after(60));

        // After 2 seconds, the long job is already running, not the short one
        Thread.sleep(2000);
        Map<JobKey, JobStatus> statuses = jobScheduler.getJobStatuses().stream().collect(Collectors.toMap(
                JobStatus::getKey,
                status -> status
        ));

        JobStatus longStatus = statuses.get(longCountJob.getKey());
        assertTrue(longStatus.isRunning());

        JobStatus shortStatus = statuses.get(countJob.getKey());
        assertFalse(shortStatus.isRunning());
    }

    @Test
    public void removing_a_running_job() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        // Fires now
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        // After some seconds, the job keeps running
        Thread.sleep(2500);
        assertEquals(3, job.getCount());
        // Now, removes the job
        jobScheduler.unschedule(job.getKey());
        // Waits a bit, and checks the job has stopped running
        Thread.sleep(2500);
        assertEquals(3, job.getCount());
    }

    @Test
    public void removing_a_long_running_job() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        LongCountJob job = new LongCountJob();
        // Fires now
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        // The job is now running
        Thread.sleep(1000);
        assertTrue(jobScheduler.getJobStatus(job.getKey()).get().isRunning());
        // Now, removes the job
        jobScheduler.unschedule(job.getKey());
        // Waits a bit, and checks the job has stopped running
        Thread.sleep(1000);
        assertEquals(0, job.getCount());
    }

    @Test
    public void job_failures() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        ErrorJob job = new ErrorJob();
        // Fires now
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        // After some seconds, the job keeps running and has only failed
        Thread.sleep(2500);
        JobStatus status = jobScheduler.getJobStatus(job.getKey()).get();
        assertEquals(3, status.getLastErrorCount());
        assertEquals("Failure", status.getLastError());
        // Now, fixes the job
        job.setFail(false);
        // Waits a bit, and checks the job is now OK
        Thread.sleep(2500);
        status = jobScheduler.getJobStatus(job.getKey()).get();
        assertEquals(0, status.getLastErrorCount());
        assertNull(status.getLastError());
    }

    protected void test_with_pause(BiConsumer<JobScheduler, PauseableJob> pause, BiConsumer<JobScheduler, PauseableJob> resume) throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        PauseableJob job = new PauseableJob();
        // Fires now
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        // After some seconds, the job keeps running
        Thread.sleep(2500);
        assertEquals(3, job.getCount());
        // Pauses
        pause.accept(jobScheduler, job);
        // After some seconds, the job has not run
        Thread.sleep(2000);
        assertEquals(3, job.getCount());
        // Resumes the job
        resume.accept(jobScheduler, job);
        // After some seconds, the job has started again
        Thread.sleep(2000);
        assertEquals(5, job.getCount());
    }

    @Test
    public void job_pause() throws InterruptedException, ExecutionException, TimeoutException {
        test_with_pause(
                (jobScheduler, job) -> job.pause(),
                (jobScheduler, job) -> job.resume()
        );
    }

    @Test
    public void job_schedule_pause() throws InterruptedException, ExecutionException, TimeoutException {
        test_with_pause(
                (jobScheduler, job) -> jobScheduler.pause(job.getKey()),
                (jobScheduler, job) -> jobScheduler.resume(job.getKey())
        );
    }

    @Test
    public void scheduler_pause() throws InterruptedException, ExecutionException, TimeoutException {
        test_with_pause(
                (jobScheduler, job) -> jobScheduler.pause(),
                (jobScheduler, job) -> jobScheduler.resume()
        );
    }

    @Test
    public void keys() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();

        LongCountJob longCountJob = new LongCountJob();
        CountJob countJob = new CountJob();

        jobScheduler.schedule(longCountJob, Schedule.EVERY_SECOND);
        jobScheduler.schedule(countJob, Schedule.EVERY_SECOND);
        OtherTypeJob otherTypeJob = new OtherTypeJob();

        jobScheduler.schedule(otherTypeJob, Schedule.EVERY_SECOND);

        assertEquals(
                ImmutableSet.of(longCountJob.getKey(), countJob.getKey(), otherTypeJob.getKey()),
                jobScheduler.getAllJobKeys()
        );

        assertEquals(
                ImmutableSet.of(longCountJob.getKey(), countJob.getKey()),
                jobScheduler.getJobKeysOfCategory(Fixtures.TEST_CATEGORY)
        );

        assertEquals(
                ImmutableSet.of(otherTypeJob.getKey()),
                jobScheduler.getJobKeysOfCategory(Fixtures.TEST_OTHER_CATEGORY)
        );
    }

    @Test(expected = JobNotScheduledException.class)
    public void pause_for_not_schedule_job() {
        JobScheduler jobScheduler = createJobScheduler();
        jobScheduler.pause(JobCategory.of("test").getType("test").getKey("x"));
    }

    @Test(expected = JobNotScheduledException.class)
    public void resume_for_not_schedule_job() {
        JobScheduler jobScheduler = createJobScheduler();
        jobScheduler.resume(JobCategory.of("test").getType("test").getKey("x"));
    }

    @Test(expected = JobNotScheduledException.class)
    public void fire_immediately_for_not_schedule_job() {
        JobScheduler jobScheduler = createJobScheduler();
        jobScheduler.fireImmediately(JobCategory.of("test").getType("test").getKey("x"));
    }

    @Test
    public void job_status_for_not_schedule_job() {
        JobScheduler jobScheduler = createJobScheduler();
        assertFalse(jobScheduler.getJobStatus(JobCategory.of("test").getType("test").getKey("x")).isPresent());
    }

    @Test
    public void job_with_param() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        ParamJob job = new ParamJob();
        jobScheduler.schedule(job, Schedule.EVERY_MINUTE.after(1)); // Not fired immediately
        // Fires manually, without any parameter
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        assertEquals(1, job.getCount());
        assertNull(job.getValue());
        // Fires manually, with one parameter
        jobScheduler.fireImmediately(job.getKey(), Collections.singletonMap("text", "Test")).get(1, TimeUnit.SECONDS);
        assertEquals(2, job.getCount());
        assertEquals("Test", job.getValue());
        // Fires manually, without any parameter
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        assertEquals(3, job.getCount());
        assertNull(job.getValue());
    }

    @Test
    public void invalid_job() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        ValidJob job = new ValidJob();
        // Fires now
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        // After some seconds, the job keeps running
        Thread.sleep(2500);
        assertEquals(3, job.getCount());
        // Invalidates the job
        job.invalidate();
        // The status indicates the job is no longer valid, but is still there
        Optional<JobStatus> status = jobScheduler.getJobStatus(job.getKey());
        assertTrue(status.isPresent());
        assertFalse(status.get().isValid());
        assertNull(status.get().getNextRunDate());
        // After some seconds, the job has not run
        Thread.sleep(1000);
        assertEquals(3, job.getCount());
        // ... and it's gone
        assertFalse(jobScheduler.getJobStatus(job.getKey()).isPresent());
    }

    @Test
    public void paused_job_can_be_fired() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        // Initially every second
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        Thread.sleep(2500);
        // After a few seconds, the count has moved
        assertEquals(3, job.getCount());
        // Pauses the job now
        jobScheduler.pause(job.getKey());
        // Not running
        Thread.sleep(2500);
        assertEquals(3, job.getCount());
        // Forcing the run
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        System.out.println("*****");
        assertEquals(4, job.getCount());
    }

    @Test
    public void not_scheduled_job_can_be_fired() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        // No schedule
        jobScheduler.schedule(job, Schedule.NONE);
        Thread.sleep(1500);
        // After a few seconds, the count has NOT moved
        assertEquals(0, job.getCount());
        // Forcing the run
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        assertEquals(1, job.getCount());
    }

    @Test
    public void not_scheduled_job_cannot_be_paused() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        CountJob job = new CountJob();
        // No schedule
        jobScheduler.schedule(job, Schedule.NONE);
        Thread.sleep(1500);
        // After a few seconds, the count has NOT moved
        assertEquals(0, job.getCount());
        // Pausing the job
        jobScheduler.pause(job.getKey());
        // Not paused
        assertFalse(jobScheduler.getJobStatus(job.getKey()).get().isPaused());
    }

    @Test
    public void disabled_job_cannot_be_fired() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        PauseableJob job = new PauseableJob();
        job.pause();
        // Initially every second
        jobScheduler.schedule(job, Schedule.EVERY_SECOND);
        Thread.sleep(2500);
        // After a few seconds, the count has NOT moved
        assertEquals(0, job.getCount());
        // Forcing the run
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        // ... to not avail
        assertEquals(0, job.getCount());
    }

    @Test
    public void invalid_job_cannot_be_fired() throws InterruptedException, ExecutionException, TimeoutException {
        JobScheduler jobScheduler = createJobScheduler();
        ValidJob job = new ValidJob();
        job.invalidate();
        // Schedules, but not now
        jobScheduler.schedule(job, Schedule.EVERY_MINUTE.after(1));
        // Forcing the run
        Thread.sleep(2500);
        jobScheduler.fireImmediately(job.getKey()).get(1, TimeUnit.SECONDS);
        // ... to not avail
        assertEquals(0, job.getCount());
        // ... and it's now gone
        assertFalse(jobScheduler.getJobStatus(job.getKey()).isPresent());
    }

    @Test
    public void run_once_job() {
        // Job to fire once
        JobKey key = JobCategory.of("once").getType("short").getKey("1");
        AtomicInteger count = new AtomicInteger();
        Job job = new Job() {
            @Override
            public JobKey getKey() {
                return key;
            }

            @Override
            public JobRun getTask() {
                return listener -> {
                    listener.message("Very fast...");
                    count.incrementAndGet();
                };
            }

            @Override
            public String getDescription() {
                return "Short";
            }

            @Override
            public boolean isDisabled() {
                return false;
            }
        };
        // Scheduler
        JobScheduler jobScheduler = createJobScheduler();
        // Fires once
        Future<?> future = jobScheduler.runOnce(job);
        // Waits for its result
        FutureUtils.wait("Short job", future, 2);
        // Checks the job is unregistered
        assertFalse("Job is unregistered", jobScheduler.getJobStatus(key).isPresent());
        // Checks it has run
        assertEquals("Job has run", 1, count.get());
    }

    @Test
    public void run_once_long_job() {
        // Job to fire once
        JobKey key = JobCategory.of("once").getType("long").getKey("1");
        AtomicInteger count = new AtomicInteger();
        Job job = new Job() {
            @Override
            public JobKey getKey() {
                return key;
            }

            @Override
            public JobRun getTask() {
                return listener -> {
                    listener.message("Very long...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    count.incrementAndGet();
                };
            }

            @Override
            public String getDescription() {
                return "Long";
            }

            @Override
            public boolean isDisabled() {
                return false;
            }
        };
        // Scheduler
        JobScheduler jobScheduler = createJobScheduler();
        // Fires once
        Future<?> future = jobScheduler.runOnce(job);
        // Checks it IS registered
        Optional<JobStatus> status = jobScheduler.getJobStatus(key);
        assertTrue("Job is registered", status.isPresent());
        assertTrue("Job is running", status.get().isRunning());
        // Waits for its result
        FutureUtils.wait("Long job", future, 4);
        // Checks the job is unregistered
        assertFalse("Job is unregistered", jobScheduler.getJobStatus(key).isPresent());
        // Checks it has run
        assertEquals("Job has run", 1, count.get());
    }

}
