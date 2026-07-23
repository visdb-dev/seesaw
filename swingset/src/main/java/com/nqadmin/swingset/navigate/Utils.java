/* *****************************************************************************
 * Copyright (C) 2024, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 * ****************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset.navigate;

import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeListener;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.Queue;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.nqadmin.swingset.datasources.DbOpsCustomizer;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSComponent;
import com.nqadmin.swingset.utils.SSUtils;

import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * TODO: Replace EventBUs with https://dagger.dev/ and RxJava
 *			https://www.baeldung.com/rx-java
 */
public class Utils
{
	private static final System.Logger logger = JStuff.getLogger();

	private Utils() { }
	
	// Notes on implementing a weak subscriber
	//		https://github.com/google/guava/issues/807#issuecomment-61328188
	// Consider the following. much like event bus, does weak listener
	//		https://github.com/bennidi/mbassador
	// And see NavigateActions for example; includes use of Cleaner.register.

	/** Singleton EventBus */
	private static final EventBus globalEventBus = new EventBus(new BusExceptionMonitor());
	static { setupTrackerForKFM(); }

	////////////////////////////////////////////////////////////////////////////
	//
	// EventBus
	//
	//     posting Events
	//     finding a bus
	//

	/**
	 * @return true means save the backtrace in the Event.
	 */
	static boolean recordEventBacktrace()
	{
		return SSUtils.isJunit() || logger.isLoggable(DEBUG);
	}

	private static void postFieldEvent(EventObjectBacktrace eo)
	{
		addToEventHistory(eo);
		getGlobalEventBus().post(eo);
	}

	/**
	 * Post a DbOps change.
	 * @param dbOps
	 * @param allow 
	 */
	public static void postDbOpsChange(DbOpsCustomizer dbOps, DbOpsCustomizer.Allow allow) {
		postFieldEvent(new DbOpsChangeEvent(dbOps, allow));
	}

	/**
	 * Post a column change event.
	 * @param source SSComponent modifying the column
	 * @param value new value
	 */
	public static void postColumnChangeStart(SSComponent source, Object value)
	{
		// May want to extend to handling local EventBus per Frame/Panel;
		// Use either/both source/rs to find a local eventBus.
		postFieldEvent(new ColumnChangeStartEvent(source, value));
	}

	/**
	 * Post an error column change event.
	 * @param source SSComponent modifying the column
	 * @param value new value
	 */
	public static void postColumnChangeStartError(SSComponent source, Object value)
	{
		ColumnChangeStartEvent ev = new ColumnChangeStartEvent(source, value, true);
		logger.log(DEBUG, () -> ev.toString());
		postFieldEvent(ev);
	}

	/**
	 * Post a completion column change event for the parm.
	 * Typically after undo/redo stack records the change.
	 * Also broadcast after an undo/redo action.
	 * @param startEv ColumnChangeStartEvent or RowSetUndoRedoEvent 
	 */
	public static void postColumnChangeDone(ChangeEventData startEv)
	{
		ColumnChangeDoneEvent ev = new ColumnChangeDoneEvent(startEv);
		logger.log(DEBUG, () -> ev.toString());
		postFieldEvent(ev);
	}

	/**
	 * Post an undo/redo event and if value an error.
	 * @param source SSComponent modifying the column
	 * @param value new value
	 * @param isError value is an error
	 */
	public static void postColumnUndoRedo(SSComponent source, Object value,
									boolean isError)
	{
		ColumnUndoRedoEvent ev = new ColumnUndoRedoEvent(source, value, isError);
		if (isError)
			logger.log(DEBUG, () -> ev.toString());
		postFieldEvent(ev);
	}


	/**
	 * The global EventBus.
	 * @return EventBus for this
	 */
	public static EventBus getGlobalEventBus() {
		return globalEventBus;
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// KeyboardFocusManager Tracking
	//
	private static PropertyChangeListener listenerFocusOwner;
	private static PropertyChangeListener listenerManagingFocus;
	private static KeyboardFocusManager KFM;

	/** @return Current KeyboardFocusManager */
	public static KeyboardFocusManager getKFM() { return KFM; }

	private static void trackCurrentKFM() {
		KFM = KeyboardFocusManager.getCurrentKeyboardFocusManager();

		KFM.addPropertyChangeListener("focusOwner", listenerFocusOwner);
		KFM.addPropertyChangeListener("managingFocus", listenerManagingFocus);
	}

	/** keep track of currentKeyboardFocusManager and switch listeners as needed */
	private static void setupTrackerForKFM() {
		if (listenerFocusOwner != null)
			return; // Could throw an exception

		// broadcast focus changes
		listenerFocusOwner = pce -> {
			globalEventBus.post(new FocusChangeEvent(pce));
		};
		// detect change in focus manager,
		// remove listeners from old, move them to current
		listenerManagingFocus = pce -> {
			if (Boolean.FALSE.equals(pce.getNewValue())) {
				logger.log(INFO, "switching to new KeyboardFocusManager");
				KeyboardFocusManager oldKFM = (KeyboardFocusManager) pce.getSource();

				oldKFM.removePropertyChangeListener("focusOwner", listenerFocusOwner);
				oldKFM.removePropertyChangeListener("managingFocus", listenerManagingFocus);

				trackCurrentKFM();
			}
		};
		// kick things off; start tracking current focus manager
		trackCurrentKFM();
	}
	
	private static class BusExceptionMonitor implements SubscriberExceptionHandler
	{
		@Override
		@SuppressWarnings("CallToPrintStackTrace")
		public void handleException(Throwable exception, SubscriberExceptionContext context)
		{
			exception.printStackTrace();
			logger.log(Level.ERROR, () -> "BusException: " + exception.getMessage());
			logger.log(Level.ERROR, () -> "    " + context.getEventBus());
			logger.log(Level.ERROR, () -> "    " + context.getSubscriber());
			logger.log(Level.ERROR, () -> "    " + context.getSubscriberMethod());
			if (context.getEvent() instanceof EventObjectBacktrace eobt
					&& eobt.getEventBacktrace() != null)
				logger.log(Level.ERROR, () -> "    " + context.getEvent(), eobt.getEventBacktrace());
			else
				logger.log(Level.ERROR, () -> "    " + context.getEvent());
			StringBuilder sb = new StringBuilder("    ");
			logger.log(Level.ERROR, () -> latestEvents("Where", 20, sb).toString());
		}
	}

	//////////////////////////////////////////////////////////////////////
	//
	// Event tracking for debug
	//

	private record EventHistoryItem(String ev, Throwable bt){}

	private static final int N_EVENTS = 50;
	private static final Queue<EventHistoryItem> latestEvents = new ArrayDeque<>();
	static void addToEventHistory(EventObjectBacktrace event) {
		if (!SSUtils.isJunit() && !logger.isLoggable(DEBUG))
			return;
		while (latestEvents.size() >= N_EVENTS)
			latestEvents.remove();
		// Convert event to String so there are no references
		// (like RowSet, RowsModel) in the history.
		latestEvents.add(new EventHistoryItem(event.toString(), event.getEventBacktrace()));
	}

	@SuppressWarnings("FieldMayBeFinal")
	private static int N_DUMP = 20;

	/**
	 * 
	 * @param tag 
	 */
	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	public static void dumpLatestEvents(String tag) {
		StringBuilder sb = latestEvents(tag, N_DUMP, null);
		System.err.println(sb.toString());
		//logger.log(INFO, sb.toString());
	}

	/**
	 * 
	 * @param tag
	 * @param limit
	 * @param _sb
	 * @return 
	 */
	public static StringBuilder latestEvents(String tag, int limit, StringBuilder _sb) {
		StringBuilder sb = _sb == null ? new StringBuilder() : _sb;
		sb.append(sf("******* %s All Events (%d) *******\n", tag, latestEvents.size()));
		latestEvents.stream().limit(limit)
				.forEach((evhist) -> sb.append("    ")
						.append(evhist.ev())
						.append('\n'));
		sb.setLength(sb.length() - 1);
		return sb;
	}
}
