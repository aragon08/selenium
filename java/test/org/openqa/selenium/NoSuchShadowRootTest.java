// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.openqa.selenium.testing.drivers.Browser.CHROME;
import static org.openqa.selenium.testing.drivers.Browser.EDGE;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.testing.Ignore;
import org.openqa.selenium.testing.JupiterTestBase;

public class NoSuchShadowRootTest extends JupiterTestBase {

  @Test
  @Ignore(value = CHROME, reason = "https://issues.chromium.org/issues/375892677")
  @Ignore(value = EDGE, reason = "https://issues.chromium.org/issues/375892677")
  public void getNoSuchShadowRoot() {
    driver.get(pages.shadowRootPage);
    WebElement nonExistentShadowRootElement = driver.findElement(By.id("noShadowRoot"));
    assertThatExceptionOfType(NoSuchShadowRootException.class)
        .isThrownBy(nonExistentShadowRootElement::getShadowRoot);
  }
}
