/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.SeeSaw;
import dev.visdb.seesaw.decorators.Decorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The library initialization and SeeSawInitialization.
 * The initialization runs once per JVM, possibly before this test,
 * so the tests only check the state after it.
 */
public class LookupDefaultsTest {
  static final AtomicInteger nFirst = new AtomicInteger();
  static final AtomicInteger nReenter = new AtomicInteger();
  static final AtomicInteger nLast = new AtomicInteger();
  static volatile int nFirstSeenByLast = -1;

  /** Runs first, and fails. */
  @ServiceProvider(service = SeeSawInitialization.class, path = LookupDefaults.INIT_PATH,
                   position = 100)
  public static class First implements SeeSawInitialization {
    @Override
    public void init() {
      nFirst.incrementAndGet();
      throw new IllegalStateException("LookupDefaultsTest: this exception is expected");
    }
  }

  /** Calls back into the initialization. */
  @ServiceProvider(service = SeeSawInitialization.class, path = LookupDefaults.INIT_PATH,
                   position = 200)
  public static class Reenter implements SeeSawInitialization {
    @Override
    public void init() {
      nReenter.incrementAndGet();
      SeeSaw.init();
      LookupDefaults.checkInit();
    }
  }

  /** Runs after the others. */
  @ServiceProvider(service = SeeSawInitialization.class, path = LookupDefaults.INIT_PATH,
                   position = 300)
  public static class Last implements SeeSawInitialization {
    @Override
    public void init() {
      nFirstSeenByLast = nFirst.get();
      nLast.incrementAndGet();
    }
  }

  @Test
  void testInit() throws InterruptedException {
    SeeSaw.init();
    // Again, and from other threads.
    SeeSaw.init();
    Thread[] threads = new Thread[4];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(LookupDefaults::checkInit);
      threads[i].start();
    }
    for (Thread thread : threads) {
      thread.join(10_000);
      assertThat(thread.isAlive()).isFalse();
    }

    // Each ran exactly once: not again for the call from within the
    // initialization, and the one that failed did not stop the others.
    assertThat(nFirst.get()).isEqualTo(1);
    assertThat(nReenter.get()).isEqualTo(1);
    assertThat(nLast.get()).isEqualTo(1);
    // In the order of their position.
    assertThat(nFirstSeenByLast).isEqualTo(1);

    // The defaults were set up after the initializers.
    assertThat(Globals.getOption(Decorator.DecoratorStyle.class)).isNotNull();
    // ImageLimits is registered.
    assertThat(ImageLimits.maxBytes()).isEqualTo(ImageLimits.DEFAULT_MAX_BYTES);
  }
}
// vi: sw=2 ts=8
