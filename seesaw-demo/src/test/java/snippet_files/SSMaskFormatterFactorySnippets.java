/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2025 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */
package snippet_files;

import java.text.ParseException;

import dev.visdb.seesaw.formatting.MaskFormatterFactory;
import dev.visdb.seesaw.formatting.MaskFormatterFactory.SSMaskFormatter;

/**
 *
 */
public class SSMaskFormatterFactorySnippets {
  // @start region=init1
  static class CustomBuilder extends MaskFormatterFactory.Builder<CustomBuilder> {
    CustomBuilder(String mask) {
      super(mask);
    }

    @Override
    protected SSMaskFormatter getSSMaskFormatter() throws ParseException {
      return new CustomSSMaskFormatter();
    }
  }

  @SuppressWarnings("serial")
  static class CustomSSMaskFormatter extends SSMaskFormatter {
    CustomSSMaskFormatter() throws ParseException {
      super(null);
    }
    // ...
  }

  void createCustomFormatterFactory() {
    MaskFormatterFactory ssmff = new CustomBuilder("...")
                                       // ...ssFormat(SSFormat.CUSTOM)
                                       .build();
  }
  // @end region=init1
}
// vi: sw=2 ts=8
