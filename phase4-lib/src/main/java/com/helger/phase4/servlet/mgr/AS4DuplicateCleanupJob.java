/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.servlet.mgr;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.lang.ClassHelper;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.web.scope.util.AbstractScopeAwareJob;

/**
 * A special job, that removes all entries
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public final class AS4DuplicateCleanupJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4DuplicateCleanupJob.class);
  private static final String KEY_MINUTES = "mins";

  public AS4DuplicateCleanupJob ()
  {}

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
  {
    final long nMins = aJobDataMap.getAsLong (KEY_MINUTES);
    final OffsetDateTime aOldDT = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ().minusMinutes (nMins);

    final ICommonsList <String> aEvicted = MetaAS4Manager.getIncomingDuplicateMgr ().evictAllItemsBefore (aOldDT);
    if (aEvicted.isNotEmpty ())
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Evicted " + aEvicted.size () + " incoming duplicate message IDs before " + aOldDT.toString ());
  }

  private static final AtomicBoolean s_aScheduled = new AtomicBoolean (false);

  /**
   * Start a job that runs every minute, that removes all messages older than a
   * certain time from duplication check. If the job is already scheduled, it
   * cannot be scheduled again.
   *
   * @param nDisposalMinutes
   *        Messages older than this number of minutes will not be checked for
   *        duplicates. Must be &gt; 0.
   * @return <code>null</code> if no job was scheduled, the trigger key of the
   *         respective job otherwise.
   */
  @Nullable
  public static TriggerKey scheduleMe (final long nDisposalMinutes)
  {
    TriggerKey aTriggerKey = null;
    if (nDisposalMinutes > 0)
    {
      if (!s_aScheduled.getAndSet (true))
      {
        final JobDataMap aJobDataMap = new JobDataMap ();
        aJobDataMap.putIn (KEY_MINUTES, nDisposalMinutes);
        aTriggerKey = GlobalQuartzScheduler.getInstance ()
                                           .scheduleJob (ClassHelper.getClassLocalName (AS4DuplicateCleanupJob.class) +
                                                         "-" +
                                                         nDisposalMinutes,
                                                         JDK8TriggerBuilder.newTrigger ()
                                                                           .startNow ()
                                                                           .withSchedule (SimpleScheduleBuilder.repeatMinutelyForever ()),
                                                         AS4DuplicateCleanupJob.class,
                                                         aJobDataMap);
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob is already scheduled");
      }
    }
    else
    {
      LOGGER.warn ("Incoming duplicate message cleaning is disabled!");
    }
    return aTriggerKey;
  }

  public static void unschedule (@Nullable final TriggerKey aTriggerKey)
  {
    if (aTriggerKey != null)
    {
      // Was the job scheduled?
      if (s_aScheduled.getAndSet (false))
      {
        GlobalQuartzScheduler.getInstance ().unscheduleJob (aTriggerKey);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob was successfully unscheduled");
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob is not scheduled");
      }
    }
  }
}
