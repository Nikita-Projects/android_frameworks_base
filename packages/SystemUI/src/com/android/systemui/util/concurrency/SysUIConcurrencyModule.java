/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.util.concurrency;

import static com.android.systemui.Dependency.TIME_TICK_HANDLER_NAME;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.android.systemui.Flags;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.BroadcastRunning;
import com.android.systemui.dagger.qualifiers.LongRunning;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.qualifiers.NotifInflation;

import dagger.Module;
import dagger.Provides;

import java.util.concurrent.Executor;

import javax.inject.Named;

/**
 * Dagger Module for classes found within the concurrent package.
 */
@Module
public abstract class SysUIConcurrencyModule {

    // Slow BG executor can potentially affect UI if UI is waiting for an updated state from this
    // thread
    private static final Long BG_SLOW_DISPATCH_THRESHOLD = 1000L;
    private static final Long BG_SLOW_DELIVERY_THRESHOLD = 1000L;
    private static final Long LONG_SLOW_DISPATCH_THRESHOLD = 2500L;
    private static final Long LONG_SLOW_DELIVERY_THRESHOLD = 2500L;
    private static final Long BROADCAST_SLOW_DISPATCH_THRESHOLD = 1000L;
    private static final Long BROADCAST_SLOW_DELIVERY_THRESHOLD = 1000L;
    private static final Long NOTIFICATION_INFLATION_SLOW_DISPATCH_THRESHOLD = 1000L;
    private static final Long NOTIFICATION_INFLATION_SLOW_DELIVERY_THRESHOLD = 1000L;

    /** Background Looper */
    @Provides
    @SysUISingleton
    @Background
    public static Looper provideBgLooper() {
        HandlerThread thread = new HandlerThread("SysUiBg",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        thread.getLooper().setSlowLogThresholdMs(BG_SLOW_DISPATCH_THRESHOLD,
                BG_SLOW_DELIVERY_THRESHOLD);
        return thread.getLooper();
    }

    /** BroadcastRunning Looper (for sending and receiving broadcasts) */
    @Provides
    @SysUISingleton
    @BroadcastRunning
    public static Looper provideBroadcastRunningLooper() {
        HandlerThread thread = new HandlerThread("BroadcastRunning",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        thread.getLooper().setSlowLogThresholdMs(BROADCAST_SLOW_DISPATCH_THRESHOLD,
                BROADCAST_SLOW_DELIVERY_THRESHOLD);
        return thread.getLooper();
    }

    /** Long running tasks Looper */
    @Provides
    @SysUISingleton
    @LongRunning
    public static Looper provideLongRunningLooper() {
        HandlerThread thread = new HandlerThread("SysUiLng",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        thread.getLooper().setSlowLogThresholdMs(LONG_SLOW_DISPATCH_THRESHOLD,
                LONG_SLOW_DELIVERY_THRESHOLD);
        return thread.getLooper();
    }

    /** Notification inflation Looper */
    @Provides
    @SysUISingleton
    @NotifInflation
    public static Looper provideNotifInflationLooper(@Background Looper bgLooper) {
        if (!Flags.dedicatedNotifInflationThread()) {
            return bgLooper;
        }

        final HandlerThread thread = new HandlerThread("NotifInflation",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        final Looper looper = thread.getLooper();
        looper.setSlowLogThresholdMs(NOTIFICATION_INFLATION_SLOW_DISPATCH_THRESHOLD,
                NOTIFICATION_INFLATION_SLOW_DELIVERY_THRESHOLD);
        return looper;
    }

    /**
     * Background Handler.
     *
     * Prefer the Background Executor when possible.
     */
    @Provides
    @Background
    public static Handler provideBgHandler(@Background Looper bgLooper) {
        return new Handler(bgLooper);
    }

    /**
     * Provide a BroadcastRunning Executor (for sending and receiving broadcasts).
     */
    @Provides
    @SysUISingleton
    @BroadcastRunning
    public static Executor provideBroadcastRunningExecutor(@BroadcastRunning Looper looper) {
        return new ExecutorImpl(looper);
    }

    /**
     * Provide a Long running Executor.
     */
    @Provides
    @SysUISingleton
    @LongRunning
    public static Executor provideLongRunningExecutor(@LongRunning Looper looper) {
        return new ExecutorImpl(looper);
    }

    /**
     * Provide a Long running Executor.
     */
    @Provides
    @SysUISingleton
    @LongRunning
    public static DelayableExecutor provideLongRunningDelayableExecutor(
            @LongRunning Looper looper) {
        return new ExecutorImpl(looper);
    }

    /**
     * Provide a Background-Thread Executor.
     */
    @Provides
    @SysUISingleton
    @Background
    public static Executor provideBackgroundExecutor(@Background Looper looper) {
        return new ExecutorImpl(looper);
    }

    /**
     * Provide a Background-Thread Executor.
     */
    @Provides
    @SysUISingleton
    @Background
    public static DelayableExecutor provideBackgroundDelayableExecutor(@Background Looper looper) {
        return new ExecutorImpl(looper);
    }

    /**
     * Provide a Background-Thread Executor.
     */
    @Provides
    @SysUISingleton
    @Background
    public static RepeatableExecutor provideBackgroundRepeatableExecutor(
            @Background DelayableExecutor exec) {
        return new RepeatableExecutorImpl(exec);
    }

    /**
     * Provide a Main-Thread Executor.
     */
    @Provides
    @SysUISingleton
    @Main
    public static RepeatableExecutor provideMainRepeatableExecutor(@Main DelayableExecutor exec) {
        return new RepeatableExecutorImpl(exec);
    }

    /** */
    @Provides
    @Main
    public static MessageRouter providesMainMessageRouter(
            @Main DelayableExecutor executor) {
        return new MessageRouterImpl(executor);
    }

    /** */
    @Provides
    @Background
    public static MessageRouter providesBackgroundMessageRouter(
            @Background DelayableExecutor executor) {
        return new MessageRouterImpl(executor);
    }

    /** */
    @Provides
    @SysUISingleton
    @Named(TIME_TICK_HANDLER_NAME)
    public static Handler provideTimeTickHandler() {
        HandlerThread thread = new HandlerThread("TimeTick");
        thread.start();
        return new Handler(thread.getLooper());
    }

    /** */
    @Provides
    @SysUISingleton
    @NotifInflation
    public static Executor provideNotifInflationExecutor(@NotifInflation Looper looper) {
        return new ExecutorImpl(looper);
    }
}
