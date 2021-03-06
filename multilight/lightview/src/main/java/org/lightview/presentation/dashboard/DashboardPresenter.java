/*
 Copyright 2012 Adam Bien, adam-bien.com

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.lightview.presentation.dashboard;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.fxml.Initializable;
import javax.inject.Inject;
import org.lightview.model.Application;
import org.lightview.model.ConnectionPool;
import org.lightview.model.Snapshot;
import org.lightview.presenter.ConnectionPoolBindings;
import org.lightview.presenter.EscalationsPresenter;
import org.lightview.presenter.EscalationsPresenterBindings;
import org.lightview.service.SnapshotSocketListener;

/**
 * User: blog.adam-bien.com Date: 21.11.11 Time: 17:50
 */
public class DashboardPresenter implements Initializable {

    private ObservableList<Snapshot> snapshots;
    private ObservableMap<String, ConnectionPoolBindings> pools;
    private LongProperty usedHeapSizeInMB;
    private LongProperty threadCount;
    private IntegerProperty peakThreadCount;
    private IntegerProperty busyThreads;
    private IntegerProperty queuedConnections;
    private IntegerProperty commitCount;
    private IntegerProperty rollbackCount;
    private IntegerProperty totalErrors;
    private IntegerProperty activeSessions;
    private IntegerProperty expiredSessions;
    private DoubleProperty commitsPerSecond;
    private DoubleProperty rollbacksPerSecond;
    private LongProperty id;
    private StringProperty deadlockedThreads;
    private EscalationsPresenter escalationsPresenter;
    private Snapshot old;
    private long lastTimeStamp;

    @Inject
    DashboardModel dashboardModel;

    @Inject
    SnapshotSocketListener listener;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.snapshots = FXCollections.observableArrayList();
        this.pools = FXCollections.observableHashMap();
        this.escalationsPresenter = new EscalationsPresenter(this.dashboardModel.serverUriProperty());
        this.usedHeapSizeInMB = new SimpleLongProperty();
        this.threadCount = new SimpleLongProperty();
        this.peakThreadCount = new SimpleIntegerProperty();
        this.busyThreads = new SimpleIntegerProperty();
        this.queuedConnections = new SimpleIntegerProperty();
        this.commitCount = new SimpleIntegerProperty();
        this.rollbackCount = new SimpleIntegerProperty();
        this.totalErrors = new SimpleIntegerProperty();
        this.activeSessions = new SimpleIntegerProperty();
        this.expiredSessions = new SimpleIntegerProperty();
        this.id = new SimpleLongProperty();
        this.commitsPerSecond = new SimpleDoubleProperty();
        this.rollbacksPerSecond = new SimpleDoubleProperty();
        this.deadlockedThreads = new SimpleStringProperty();
        this.initializeListeners();
    }

    public void initializeListeners() {
        this.dashboardModel.serverUriProperty().addListener((observableValue, s, newUri) -> {
            //re-initialize socket listener
        });
        this.listener.snapshotProperty().addListener((o, oldValue, newValue) -> {
            snapshots.add(newValue);
            onSnapshotArrival(newValue);
        });
    }

    void onSnapshotArrival(Snapshot snapshot) {
        this.usedHeapSizeInMB.set(snapshot.getUsedHeapSizeInMB());
        this.threadCount.set(snapshot.getThreadCount());
        this.peakThreadCount.set(snapshot.getPeakThreadCount());
        this.busyThreads.set(snapshot.getCurrentThreadBusy());
        this.queuedConnections.set(snapshot.getQueuedConnections());
        this.commitCount.set(snapshot.getCommittedTX());
        this.rollbackCount.set(snapshot.getRolledBackTX());
        this.totalErrors.set(snapshot.getTotalErrors());
        this.activeSessions.set(snapshot.getActiveSessions());
        this.expiredSessions.set(snapshot.getExpiredSessions());
        this.deadlockedThreads.set(snapshot.getDeadlockedThreads());
        this.id.set(snapshot.getId());
        this.updatePools(snapshot);
        this.dashboardModel.updateApplications(snapshot.getApps());
        long current = System.currentTimeMillis();
        long delta = current - lastTimeStamp;
        if (old == null) {
            old = snapshot;
        }
        this.commitsPerSecond.set(getTPSValue(delta, old.getCommittedTX(), snapshot.getCommittedTX()));
        this.rollbacksPerSecond.set(getTPSValue(delta, old.getRolledBackTX(), snapshot.getRolledBackTX()));
        lastTimeStamp = current;
        this.old = snapshot;
        this.dashboardModel.currentSnapshotProperty().set(snapshot);
    }

    public double getTPSValue(long delta, long oldValue, long newValue) {
        return (newValue - oldValue) / ((delta / 1000));
    }

    void updatePools(Snapshot snapshot) {
        List<ConnectionPool> connectionPools = snapshot.getPools();
        for (ConnectionPool connectionPool : connectionPools) {
            String jndiName = connectionPool.getJndiName();
            ConnectionPoolBindings bindings = ConnectionPoolBindings.from(connectionPool);
            ConnectionPoolBindings poolBindings = this.pools.get(jndiName);
            if (poolBindings != null) {
                poolBindings.update(connectionPool);
            } else {
                this.pools.put(jndiName, bindings);
            }
        }
    }

    public EscalationsPresenterBindings getEscalationsPresenterBindings() {
        return this.escalationsPresenter;
    }

    public LongProperty getUsedHeapSizeInMB() {
        return usedHeapSizeInMB;
    }

    public LongProperty getThreadCount() {
        return threadCount;
    }

    public IntegerProperty getPeakThreadCount() {
        return peakThreadCount;
    }

    public IntegerProperty getBusyThreads() {
        return busyThreads;
    }

    public IntegerProperty getQueuedConnections() {
        return queuedConnections;
    }

    public IntegerProperty getCommitCount() {
        return commitCount;
    }

    public IntegerProperty getRollbackCount() {
        return rollbackCount;
    }

    public IntegerProperty getTotalErrors() {
        return totalErrors;
    }

    public IntegerProperty getActiveSessions() {
        return activeSessions;
    }

    public IntegerProperty getExpiredSessions() {
        return expiredSessions;
    }

    public LongProperty getId() {
        return id;
    }

    public ObservableList<Snapshot> getSnapshots() {
        return snapshots;
    }

    public ObservableMap<String, ConnectionPoolBindings> getPools() {
        return pools;
    }

    public StringProperty getDeadlockedThreads() {
        return this.deadlockedThreads;
    }

    public DoubleProperty getCommitsPerSecond() {
        return commitsPerSecond;
    }

    public ReadOnlyDoubleProperty getRollbacksPerSecond() {
        return rollbacksPerSecond;
    }

    public ObservableSet<Application> getApplications() {
        return this.dashboardModel.applicationsSetProperty();
    }

    public StringProperty getUriProperty() {
        return this.dashboardModel.serverUriProperty();
    }

    private String getUri() {
        return getUriProperty().get();
    }

}
