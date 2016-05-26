package com.texocoyotl.jobs;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.codelabs.migratingtojobs.shared.BaseEventListener;
import com.google.codelabs.migratingtojobs.shared.CatalogItem;
import com.google.codelabs.migratingtojobs.shared.CatalogItemStore;
import com.google.codelabs.migratingtojobs.shared.EventBus;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;


public class DownloaderJobService extends JobService {

    /**
     * List of all listeners we register, so we can make sure they get unregistered when this
     * service goes away.
     */
    final List<EventBus.EventListener> eventListeners = new LinkedList<>();

    @Inject
    EventBus bus;

    @Inject
    CatalogItemStore itemStore;

    @Override
    public boolean onStartJob(JobParameters job) {
        EventListener listener = new EventListener(this, job, bus);
        synchronized (eventListeners) {
            eventListeners.add(listener);
            bus.register(listener);
        }

        // TRIGGER WORK
        bus.postRetryDownloads(itemStore);

        return true; // true because there's more work being done on a separate thread
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        // If this is being called it means we haven't explicitly finished our work yet.
        // Return true so we get rescheduled.
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize everything if it's not already, plus inject dependencies.
        JobsGlobalState.get(getApplication()).inject(this);
    }

    @Override
    public void onDestroy() {
        synchronized (eventListeners) {
            for (EventBus.EventListener listener : eventListeners) {
                // unregistering prevents leaks.
                bus.unregister(listener);
            }
        }

        super.onDestroy();
    }

    private final static class EventListener extends BaseEventListener {
        private final JobService service;
        private final JobParameters job;
        private final EventBus bus;

        public EventListener(JobService service, JobParameters job, EventBus bus) {
            this.service = service;
            this.job = job;
            this.bus = bus;
        }

        @Override
        public void onItemDownloadFailed(CatalogItem item) {
            service.jobFinished(job, true);
            JobEvents.postDownloadJobFailed(bus);
        }

        @Override
        public void onAllDownloadsFinished() {
            service.jobFinished(job, false);
            JobEvents.postDownloadJobFinished(bus);
        }
    }
}